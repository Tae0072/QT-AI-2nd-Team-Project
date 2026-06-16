package com.qtai.domain.appversion.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.appversion.api.AdminAppVersionUseCase;
import com.qtai.domain.appversion.api.dto.AppVersionStateResponse;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateCreateRequest;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 앱 버전/업데이트 관리 API (appversion 도메인, 2026-06-14 Lead 승인).
 *
 * <p>콘텐츠 버전 즉시 게시(POST /apply-content)와 앱 출시 버전 업데이트(업데이트 예정 큐)를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/admin/app-updates")
@RequiredArgsConstructor
public class AdminAppVersionController {

    private static final List<String> ALLOWED_ROLES = List.of("OPERATOR");

    private final AdminAppVersionUseCase adminAppVersionUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /** GET /api/v1/admin/app-updates/state — 현재 버전 상태 */
    @GetMapping("/state")
    public ResponseEntity<ApiResponse<AppVersionStateResponse>> state(Authentication authentication) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminAppVersionUseCase.getState()));
    }

    /** POST /api/v1/admin/app-updates/apply-content — 콘텐츠 버전 즉시 게시 */
    @PostMapping("/apply-content")
    public ResponseEntity<ApiResponse<AppVersionStateResponse>> applyContent(Authentication authentication) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminAppVersionUseCase.applyContent()));
    }

    /** GET /api/v1/admin/app-updates/pending?status= — 업데이트 예정 목록 */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingAppUpdateResponse>>> listPending(
            @RequestParam(value = "status", required = false) String status,
            Authentication authentication) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminAppVersionUseCase.listPending(status)));
    }

    /** POST /api/v1/admin/app-updates/pending — 업데이트 예정 등록 */
    @PostMapping("/pending")
    public ResponseEntity<ApiResponse<PendingAppUpdateResponse>> createPending(
            Authentication authentication,
            @Valid @RequestBody PendingAppUpdateCreateRequest request) {
        requireRole(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminAppVersionUseCase.createPending(request)));
    }

    /** POST /api/v1/admin/app-updates/pending/{id}/apply — 적용(앱 출시 버전 업데이트) */
    @PostMapping("/pending/{id}/apply")
    public ResponseEntity<ApiResponse<AppVersionStateResponse>> applyPending(
            @PathVariable Long id, Authentication authentication) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminAppVersionUseCase.applyPending(id)));
    }

    /** DELETE /api/v1/admin/app-updates/pending/{id} — 업데이트 예정 삭제 */
    @DeleteMapping("/pending/{id}")
    public ResponseEntity<Void> deletePending(@PathVariable Long id, Authentication authentication) {
        requireRole(authentication);
        adminAppVersionUseCase.deletePending(id);
        return ResponseEntity.noContent().build();
    }

    private Long requireRole(Authentication requestAuthentication) {
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
        return verifyAdminRoleUseCase.verifyAnyRole(memberId, ALLOWED_ROLES).adminUserId();
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
}
