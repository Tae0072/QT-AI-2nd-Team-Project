package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * DevAdminSeedRunner 단위 테스트.
 *
 * <p>검증: admin_users 부재 시 SUPER_ADMIN 활성 시드 / 존재 시 미시드 / FK 위반 흡수.
 */
class DevAdminSeedRunnerTest {

    private DevAdminSeedRunner newRunner(AdminUserRepository repo, long memberId) {
        DevAdminSeedRunner runner = new DevAdminSeedRunner(repo);
        ReflectionTestUtils.setField(runner, "adminMemberId", memberId);
        return runner;
    }

    @Test
    @DisplayName("admin_users에 없으면 지정 회원을 SUPER_ADMIN 활성 관리자로 시드한다")
    void run_seedsSuperAdminWhenAbsent() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        when(repo.findByMemberId(1L)).thenReturn(Optional.empty());
        DevAdminSeedRunner runner = newRunner(repo, 1L);

        runner.run(null);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(repo).save(captor.capture());
        AdminUser saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getAdminRole()).isEqualTo(AdminRole.SUPER_ADMIN);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("이미 관리자면 다시 시드하지 않는다")
    void run_skipsWhenAdminExists() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        when(repo.findByMemberId(1L)).thenReturn(
                Optional.of(AdminUser.builder().memberId(1L).adminRole(AdminRole.SUPER_ADMIN).build()));
        DevAdminSeedRunner runner = newRunner(repo, 1L);

        runner.run(null);

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("회원 미존재(FK 위반)면 예외를 흡수한다")
    void run_ignoresForeignKeyViolation() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        when(repo.findByMemberId(1L)).thenReturn(Optional.empty());
        when(repo.save(any(AdminUser.class)))
                .thenThrow(new DataIntegrityViolationException("FK: member not found"));
        DevAdminSeedRunner runner = newRunner(repo, 1L);

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
    }
}
