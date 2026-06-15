package com.qtai.domain.mission.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.mission.api.AdminMissionUseCase;
import com.qtai.domain.mission.api.dto.AdminMissionResponse;
import com.qtai.domain.mission.api.dto.MissionCreateRequest;
import com.qtai.domain.mission.api.dto.MissionUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 미션 정의 관리 API (F-13).
 *
 * <p>미션 정의의 목록·생성·수정·상태(ACTIVE/HIDDEN) 변경을 제공한다.
 */
@RestController
@RequestMapping("/api/v1/admin/missions")
@RequiredArgsConstructor
public class AdminMissionController {

    private static final List<String> ALLOWED_ROLES = List.of("CONTENT_CREATOR", "OPERATOR");

    private final AdminMissionUseCase adminMissionUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /** GET /api/v1/admin/missions */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminMissionResponse>>> list(Authentication authentication) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminMissionUseCase.listForAdmin()));
    }

    /** GET /api/v1/admin/missions/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminMissionResponse>> detail(
            @PathVariable Long id, Authentication authentication) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminMissionUseCase.getForAdmin(id)));
    }

    /** POST /api/v1/admin/missions */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminMissionResponse>> create(
            Authentication authentication,
            @Valid @RequestBody MissionCreateRequest request) {
        requireRole(authentication);
        AdminMissionResponse response = adminMissionUseCase.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/missions/" + response.id()))
                .body(ApiResponse.success(response));
    }

    /** PATCH /api/v1/admin/missions/{id} */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminMissionResponse>> update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody MissionUpdateRequest request) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminMissionUseCase.update(id, request)));
    }

    /** PATCH /api/v1/admin/missions/{id}/status?value=ACTIVE|HIDDEN */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminMissionResponse>> changeStatus(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam("value") String value) {
        requireRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminMissionUseCase.changeStatus(id, value)));
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
