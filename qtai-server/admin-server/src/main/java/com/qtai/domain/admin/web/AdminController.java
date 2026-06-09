package com.qtai.domain.admin.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 REST 엔드포인트. base path: /api/v1/admin
 *
 * <p>모든 엔드포인트는 ROLE_ADMIN 필수 — Spring Security의 @PreAuthorize로 강제.
 * 일반 회원 토큰으로 접근 시 403 FORBIDDEN.
 *
 * <p>CLAUDE.md §5: Spring Security가 1차 ADMIN role 검증 후,
 * 서비스 레이어에서 admin_users.admin_role 2차 검증.
 *
 * <p>W3 1단계: 관리자 본인 정보 조회 엔드포인트.
 * 통계, 회원 검색, 콘텐츠 모더레이션은 후속 PR에서 구현.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /**
     * 현재 관리자 본인 정보 조회.
     *
     * <p>관리자 웹 프런트엔드 로그인 후 세부 권한 확인 용도.
     * Spring Security가 ROLE_ADMIN을 1차 검증하고,
     * 서비스 레이어에서 admin_users 2차 조회.
     *
     * @param memberId JWT에서 추출한 회원 ID (@AuthenticationPrincipal)
     * @return 관리자 계정 정보 (adminUserId, memberId, adminRole)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminUserInfo>> getMyAdminInfo(
            @AuthenticationPrincipal Long memberId) {

        AdminUserInfo adminInfo = verifyAdminRoleUseCase.getActiveAdmin(memberId);
        return ResponseEntity.ok(ApiResponse.success(adminInfo));
    }
}
