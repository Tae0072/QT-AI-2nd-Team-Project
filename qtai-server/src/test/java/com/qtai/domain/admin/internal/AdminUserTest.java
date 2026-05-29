package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AdminUser 엔티티 단위 테스트.
 *
 * <p>ERD §2.31 역할 정책 및 CLAUDE.md §5 권한 체계를 검증한다.
 */
class AdminUserTest {

    @Test
    @DisplayName("SUPER_ADMIN은 모든 역할을 포함한다")
    void SUPER_ADMIN은_모든_역할을_포함한다() {
        // given
        AdminUser superAdmin = AdminUser.builder()
                .memberId(1L)
                .adminRole(AdminRole.SUPER_ADMIN)
                .build();

        // then: 모든 역할에 대해 hasRole이 true
        assertThat(superAdmin.hasRole(AdminRole.SUPER_ADMIN)).isTrue();
        assertThat(superAdmin.hasRole(AdminRole.OPERATOR)).isTrue();
        assertThat(superAdmin.hasRole(AdminRole.REVIEWER)).isTrue();
        assertThat(superAdmin.hasRole(AdminRole.CONTENT_CREATOR)).isTrue();
    }

    @Test
    @DisplayName("OPERATOR는 자기 역할만 통과한다")
    void OPERATOR는_자기_역할만_통과한다() {
        // given
        AdminUser operator = AdminUser.builder()
                .memberId(2L)
                .adminRole(AdminRole.OPERATOR)
                .build();

        // then
        assertThat(operator.hasRole(AdminRole.OPERATOR)).isTrue();
        assertThat(operator.hasRole(AdminRole.REVIEWER)).isFalse();
        assertThat(operator.hasRole(AdminRole.CONTENT_CREATOR)).isFalse();
        assertThat(operator.hasRole(AdminRole.SUPER_ADMIN)).isFalse();
    }

    @Test
    @DisplayName("REVIEWER는 자기 역할만 통과한다")
    void REVIEWER는_자기_역할만_통과한다() {
        // given
        AdminUser reviewer = AdminUser.builder()
                .memberId(3L)
                .adminRole(AdminRole.REVIEWER)
                .build();

        // then
        assertThat(reviewer.hasRole(AdminRole.REVIEWER)).isTrue();
        assertThat(reviewer.hasRole(AdminRole.OPERATOR)).isFalse();
        assertThat(reviewer.hasRole(AdminRole.CONTENT_CREATOR)).isFalse();
    }

    @Test
    @DisplayName("CONTENT_CREATOR는 자기 역할만 통과한다")
    void CONTENT_CREATOR는_자기_역할만_통과한다() {
        // given
        AdminUser contentCreator = AdminUser.builder()
                .memberId(4L)
                .adminRole(AdminRole.CONTENT_CREATOR)
                .build();

        // then
        assertThat(contentCreator.hasRole(AdminRole.CONTENT_CREATOR)).isTrue();
        assertThat(contentCreator.hasRole(AdminRole.OPERATOR)).isFalse();
        assertThat(contentCreator.hasRole(AdminRole.REVIEWER)).isFalse();
    }

    @Test
    @DisplayName("신규 관리자는 ACTIVE 상태로 생성된다")
    void 신규_관리자는_ACTIVE_상태로_생성된다() {
        // given
        AdminUser admin = AdminUser.builder()
                .memberId(5L)
                .adminRole(AdminRole.OPERATOR)
                .build();

        // then
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getStatus()).isEqualTo(AdminStatus.ACTIVE);
    }

    @Test
    @DisplayName("비활성화 후 isActive가 false를 반환한다")
    void 비활성화_후_isActive가_false를_반환한다() {
        // given
        AdminUser admin = AdminUser.builder()
                .memberId(6L)
                .adminRole(AdminRole.OPERATOR)
                .build();

        // when
        admin.disable();

        // then
        assertThat(admin.isActive()).isFalse();
        assertThat(admin.getStatus()).isEqualTo(AdminStatus.DISABLED);
    }

    @Test
    @DisplayName("비활성화 후 다시 활성화할 수 있다")
    void 비활성화_후_다시_활성화할_수_있다() {
        // given
        AdminUser admin = AdminUser.builder()
                .memberId(7L)
                .adminRole(AdminRole.OPERATOR)
                .build();
        admin.disable();

        // when
        admin.enable();

        // then
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    @DisplayName("관리자 역할을 변경할 수 있다")
    void 관리자_역할을_변경할_수_있다() {
        // given
        AdminUser admin = AdminUser.builder()
                .memberId(8L)
                .adminRole(AdminRole.OPERATOR)
                .build();

        // when
        admin.changeRole(AdminRole.REVIEWER);

        // then
        assertThat(admin.getAdminRole()).isEqualTo(AdminRole.REVIEWER);
        assertThat(admin.hasRole(AdminRole.REVIEWER)).isTrue();
        assertThat(admin.hasRole(AdminRole.OPERATOR)).isFalse();
    }
}
