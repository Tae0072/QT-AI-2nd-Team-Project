package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class DevMemberSeedRunnerTest {

    @Test
    @DisplayName("회원이 없으면 dev-bypass용 회원을 생성한다")
    void run_createsDevMemberWhenEmpty() {
        MemberRepository repository = mock(MemberRepository.class);
        when(repository.existsByKakaoId(9_000_000_001L)).thenReturn(false);
        DevMemberSeedRunner runner = new DevMemberSeedRunner(repository);

        runner.run(null);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(repository).saveAndFlush(captor.capture());
        Member saved = captor.getValue();
        assertThat(saved.getKakaoId()).isEqualTo(9_000_000_001L);
        assertThat(saved.getNickname()).isEqualTo("dev-user");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("이미 회원이 있으면 dev-bypass용 회원을 추가하지 않는다")
    void run_skipsWhenMemberExists() {
        MemberRepository repository = mock(MemberRepository.class);
        when(repository.existsByKakaoId(9_000_000_001L)).thenReturn(true);
        DevMemberSeedRunner runner = new DevMemberSeedRunner(repository);

        runner.run(null);

        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("동시 seed 경합으로 unique 제약이 먼저 걸리면 예외를 흡수한다")
    void run_ignoresConcurrentSeedConflict() {
        MemberRepository repository = mock(MemberRepository.class);
        when(repository.existsByKakaoId(9_000_000_001L)).thenReturn(false);
        when(repository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate dev member"));
        DevMemberSeedRunner runner = new DevMemberSeedRunner(repository);

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
    }
}
