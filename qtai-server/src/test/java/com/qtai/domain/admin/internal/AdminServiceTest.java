package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.dto.AdminUserInfo;

/**
 * AdminService 단위 테스트.
 *
 * <p>CLAUDE.md §5 관리자 권한 이중 검증 로직을 검증한다.
 * <p>CLAUDE.md §10 필수 테스트: admin authorization 검증.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminService adminService;

    // ─── 헬퍼 ───────────────────────────────────────

    private static AdminUser createActiveAdmin(Long memberId, AdminRole role) {
        return AdminUser.builder()
                .memberId(memberId)
                .adminRole(role)
                .build();
    }

    private static AdminUser createDisabledAdmin(Long memberId, AdminRole role) {
        AdminUser admin = AdminUser.builder()
                .memberId(memberId)
                .adminRole(role)
                .build();
        admin.disable();
        return admin;
    }

    // ─── getActiveAdmin ─────────────────────────────

    @Test
    @DisplayName("활성 관리자 조회 성공")
    void 활성_관리자_조회_성공() {
        // given
        Long memberId = 1L;
        AdminUser admin = createActiveAdmin(memberId, AdminRole.OPERATOR);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(admin));

        // when
        AdminUserInfo result = adminService.getActiveAdmin(memberId);

        // then
        assertThat(result.memberId()).isEqualTo(memberId);
        assertThat(result.adminRole()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("admin_users에 레코드 없으면 ADMIN_USER_NOT_FOUND")
    void admin_users에_레코드_없으면_ADMIN_USER_NOT_FOUND() {
        // given
        Long memberId = 99L;
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.getActiveAdmin(memberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_USER_NOT_FOUND));
    }

    @Test
    @DisplayName("비활성 관리자 접근 시 ADMIN_USER_DISABLED")
    void 비활성_관리자_접근_시_ADMIN_USER_DISABLED() {
        // given
        Long memberId = 2L;
        AdminUser disabledAdmin = createDisabledAdmin(memberId, AdminRole.OPERATOR);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(disabledAdmin));

        // when & then
        assertThatThrownBy(() -> adminService.getActiveAdmin(memberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_USER_DISABLED));
    }

    // ─── verifyRole ─────────────────────────────────

    @Test
    @DisplayName("OPERATOR가 OPERATOR 권한 검증 통과")
    void OPERATOR가_OPERATOR_권한_검증_통과() {
        // given
        Long memberId = 3L;
        AdminUser admin = createActiveAdmin(memberId, AdminRole.OPERATOR);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(admin));

        // when
        AdminUserInfo result = adminService.verifyRole(memberId, "OPERATOR");

        // then
        assertThat(result.adminRole()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("SUPER_ADMIN은 OPERATOR 역할도 통과한다")
    void SUPER_ADMIN은_OPERATOR_역할도_통과한다() {
        // given
        Long memberId = 4L;
        AdminUser superAdmin = createActiveAdmin(memberId, AdminRole.SUPER_ADMIN);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(superAdmin));

        // when
        AdminUserInfo result = adminService.verifyRole(memberId, "OPERATOR");

        // then
        assertThat(result.adminRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    @DisplayName("SUPER_ADMIN은 REVIEWER 역할도 통과한다")
    void SUPER_ADMIN은_REVIEWER_역할도_통과한다() {
        // given
        Long memberId = 5L;
        AdminUser superAdmin = createActiveAdmin(memberId, AdminRole.SUPER_ADMIN);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(superAdmin));

        // when
        AdminUserInfo result = adminService.verifyRole(memberId, "REVIEWER");

        // then
        assertThat(result.adminRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    @DisplayName("OPERATOR가 REVIEWER 권한 요청 시 ADMIN_ROLE_INSUFFICIENT")
    void OPERATOR가_REVIEWER_권한_요청_시_ADMIN_ROLE_INSUFFICIENT() {
        // given
        Long memberId = 6L;
        AdminUser operator = createActiveAdmin(memberId, AdminRole.OPERATOR);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(operator));

        // when & then
        assertThatThrownBy(() -> adminService.verifyRole(memberId, "REVIEWER"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_ROLE_INSUFFICIENT));
    }

    @Test
    @DisplayName("CONTENT_CREATOR가 OPERATOR 권한 요청 시 ADMIN_ROLE_INSUFFICIENT")
    void CONTENT_CREATOR가_OPERATOR_권한_요청_시_ADMIN_ROLE_INSUFFICIENT() {
        // given
        Long memberId = 7L;
        AdminUser contentCreator = createActiveAdmin(memberId, AdminRole.CONTENT_CREATOR);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(contentCreator));

        // when & then
        assertThatThrownBy(() -> adminService.verifyRole(memberId, "OPERATOR"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_ROLE_INSUFFICIENT));
    }

    @Test
    @DisplayName("비활성 관리자는 역할 검증 전에 차단된다")
    void 비활성_관리자는_역할_검증_전에_차단된다() {
        // given
        Long memberId = 8L;
        AdminUser disabledAdmin = createDisabledAdmin(memberId, AdminRole.SUPER_ADMIN);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(disabledAdmin));

        // when & then: SUPER_ADMIN이라도 비활성이면 DISABLED
        assertThatThrownBy(() -> adminService.verifyRole(memberId, "OPERATOR"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_USER_DISABLED));
    }

    @Test
    @DisplayName("미등록 관리자는 역할 검증 시 NOT_FOUND")
    void 미등록_관리자는_역할_검증_시_NOT_FOUND() {
        // given
        Long memberId = 99L;
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.verifyRole(memberId, "OPERATOR"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_USER_NOT_FOUND));
    }

    @Test
    @DisplayName("잘못된 역할 문자열은 ADMIN_ROLE_INSUFFICIENT")
    void 잘못된_역할_문자열은_ADMIN_ROLE_INSUFFICIENT() {
        // given
        Long memberId = 10L;
        AdminUser admin = createActiveAdmin(memberId, AdminRole.OPERATOR);
        when(adminUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(admin));

        // when & then: 존재하지 않는 역할 문자열
        assertThatThrownBy(() -> adminService.verifyRole(memberId, "INVALID_ROLE"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ADMIN_ROLE_INSUFFICIENT));
    }
}
