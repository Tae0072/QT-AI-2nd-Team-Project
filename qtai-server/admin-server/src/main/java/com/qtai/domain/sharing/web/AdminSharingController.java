package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.sharing.api.AdminSharingPostUseCase;
import com.qtai.domain.sharing.api.dto.AdminSharingPostResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 나눔 공유글 운영 API (F-10, AD-15).
 *
 * <p>base path: {@code /api/v1/admin/sharing-posts}. 권한: ADMIN + admin_users OPERATOR.
 * 모더레이션은 숨김/복원만 제공한다(하드 삭제 없음).
 * <ul>
 *   <li>GET   /api/v1/admin/sharing-posts             — 목록(status·q 필터, 페이징)</li>
 *   <li>GET   /api/v1/admin/sharing-posts/{postId}    — 상세(전체 본문)</li>
 *   <li>PATCH /api/v1/admin/sharing-posts/{postId}/hide    — 숨김</li>
 *   <li>PATCH /api/v1/admin/sharing-posts/{postId}/restore — 복원(공개)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/sharing-posts")
@RequiredArgsConstructor
public class AdminSharingController {

    private final AdminSharingPostUseCase adminSharingPostUseCase;
    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminSharingPostResponse>>> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireOperator(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                adminSharingPostUseCase.listForAdmin(status, q, pageable)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<AdminSharingPostResponse>> detail(
            @PathVariable Long postId, Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminSharingPostUseCase.getForAdmin(postId)));
    }

    @PatchMapping("/{postId}/hide")
    public ResponseEntity<ApiResponse<AdminSharingPostResponse>> hide(
            @PathVariable Long postId, Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminSharingPostUseCase.hide(postId)));
    }

    @PatchMapping("/{postId}/restore")
    public ResponseEntity<ApiResponse<AdminSharingPostResponse>> restore(
            @PathVariable Long postId, Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminSharingPostUseCase.restore(postId)));
    }

    // ── 관리자 인증/권한 (ADMIN + admin_users OPERATOR, SUPER_ADMIN 우월권) ──

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
}
