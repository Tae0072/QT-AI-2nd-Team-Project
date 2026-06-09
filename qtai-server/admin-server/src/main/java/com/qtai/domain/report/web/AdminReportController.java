package com.qtai.domain.report.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.ListAdminReportsUseCase;
import com.qtai.domain.report.api.ProcessReportUseCase;
import com.qtai.domain.report.api.dto.AdminReportListQuery;
import com.qtai.domain.report.api.dto.AdminReportListResponse;
import com.qtai.domain.report.api.dto.ProcessReportCommand;
import com.qtai.domain.report.api.dto.ProcessReportResult;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 신고 처리 REST 엔드포인트.
 *
 * <p>API 명세서 §4.7.4. base path: /api/v1/admin/reports. 권한: ADMIN + OPERATOR/SUPER_ADMIN.
 * <ul>
 *   <li>GET    /api/v1/admin/reports               — 신고 목록(status·targetType 필터, 페이징)</li>
 *   <li>POST   /api/v1/admin/reports/{id}/resolve   — 처리 완료(RESOLVED)</li>
 *   <li>POST   /api/v1/admin/reports/{id}/reject    — 반려(REJECTED)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ListAdminReportsUseCase listAdminReportsUseCase;
    private final ProcessReportUseCase processReportUseCase;
    private final com.qtai.domain.admin.api.VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /** 후속 조치 요청 본문(resolve/reject 공통). */
    public record ProcessReportRequest(String action, String reason, boolean notifyReporter) {
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminReportListResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        AdminReportListResponse response = listAdminReportsUseCase.listReports(
                new AdminReportListQuery(status, targetType, page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<ApiResponse<ProcessReportResult>> resolve(
            @PathVariable("reportId") Long reportId,
            Authentication authentication,
            @RequestBody(required = false) ProcessReportRequest request) {
        Long adminId = requireOperator(authentication);
        ProcessReportResult result = processReportUseCase.resolve(toCommand(adminId, reportId, request));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<ProcessReportResult>> reject(
            @PathVariable("reportId") Long reportId,
            Authentication authentication,
            @RequestBody(required = false) ProcessReportRequest request) {
        Long adminId = requireOperator(authentication);
        ProcessReportResult result = processReportUseCase.reject(toCommand(adminId, reportId, request));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private ProcessReportCommand toCommand(Long adminId, Long reportId, ProcessReportRequest request) {
        ProcessReportRequest r = request != null ? request : new ProcessReportRequest(null, null, false);
        return new ProcessReportCommand(adminId, reportId, r.action(), r.reason(), r.notifyReporter());
    }

    // ── 관리자 인증/권한 (ADMIN + admin_users OPERATOR, SUPER_ADMIN 우월권) ──

    /**
     * 신고 처리 권한 검증 — admin_users DB 2차 검증(CLAUDE.md §5).
     *
     * <p>기존 JWT {@code ADMIN_ROLE_*} authority 파싱은 발급 경로가 없어
     * 운영에서 전부 403이 나는 결함이 있어 DB 검증으로 통일했다.
     *
     * @return admin_users.id — processedByAdminId FK(admin_users) 기록용.
     *         기존에는 members.id를 넘겨 운영 MySQL에서 FK 위반이 나던 오기록을 수정.
     */
    private Long requireOperator(Authentication requestAuthentication) {
        Authentication authentication = requestAuthentication != null
                ? requestAuthentication
                : SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        if (!authorities.contains("ROLE_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long memberId = resolvePrincipalId(authentication);
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, List.of("OPERATOR")).adminUserId();
    }

    private static Long resolvePrincipalId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(principal));
        } catch (NumberFormatException e) {
            try {
                return Long.valueOf(authentication.getName());
            } catch (NumberFormatException ex) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), e.getMessage()));
    }
}
