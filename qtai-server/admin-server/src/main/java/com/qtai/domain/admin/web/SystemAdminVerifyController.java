package com.qtai.domain.admin.web;

import java.util.List;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 권한 검증 — <b>시스템 배치(SYSTEM_BATCH) 전용</b> 수신 엔드포인트.
 *
 * <p>다른 서비스(service-user 등)가 사용자의 admin 자격을 확인해야 할 때 admin-server(admin 소유)에 조회한다.
 * admin은 admin-server 소유라 타 서비스는 {@link VerifyAdminRoleUseCase} api 계약만 client 어댑터로 호출한다(CLAUDE.md §4).
 * 예: service-user의 관리자 카카오 로그인이 "이 회원이 활성 관리자인가/필요 역할을 갖는가"를 본 엔드포인트로 검증한다.
 *
 * <p>경로가 {@code /api/v1/system/**}이라 {@link com.qtai.security.SecurityConfig}의
 * {@code .requestMatchers("/api/v1/system/**").hasRole("SYSTEM_BATCH")} 규칙으로 시스템 배치 주체만 접근한다
 * (일반 사용자·ADMIN은 403, 미인증은 401). 시스템 토큰(HS256)은 {@code JwtAuthenticationFilter}가 검증한다.
 *
 * <p>관리자 본인이 보는 admin 조회는 {@link AdminController}(/api/v1/admin/**)가 담당하고, 이 컨트롤러는
 * 서비스 간 시스템 호출의 <b>검증</b>만 처리한다. 비즈니스 예외(ADMIN_USER_NOT_FOUND/DISABLED/ROLE_INSUFFICIENT)는
 * 공통 예외 핸들러가 표준 에러 envelope(코드 AD000x, 403)로 변환한다.
 */
@RestController
@RequestMapping("/api/v1/system/admin")
@RequiredArgsConstructor
public class SystemAdminVerifyController {

    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    /**
     * memberId의 활성 관리자 정보를 검증·조회한다.
     *
     * <ul>
     *   <li>{@code requiredRoles} 없음 → {@link VerifyAdminRoleUseCase#getActiveAdmin}(활성 관리자 여부만).</li>
     *   <li>{@code requiredRoles} 있음 → {@link VerifyAdminRoleUseCase#verifyAnyRole}(나열 역할 중 하나 이상 + SUPER_ADMIN 우월).</li>
     * </ul>
     *
     * @param memberId      검증 대상 회원 ID
     * @param requiredRoles 허용 관리자 역할 목록(선택). 비면 활성 여부만 검증.
     * @return 관리자 정보(adminUserId, memberId, adminRole)
     */
    @GetMapping("/verify")
    public ApiResponse<AdminUserInfo> verify(
            @RequestParam Long memberId,
            @RequestParam(name = "requiredRoles", required = false) List<String> requiredRoles) {
        AdminUserInfo info = (requiredRoles == null || requiredRoles.isEmpty())
                ? verifyAdminRoleUseCase.getActiveAdmin(memberId)
                : verifyAdminRoleUseCase.verifyAnyRole(memberId, requiredRoles);
        return ApiResponse.success(info);
    }
}
