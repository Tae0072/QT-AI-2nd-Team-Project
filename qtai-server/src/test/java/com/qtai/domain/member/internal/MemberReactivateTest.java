package com.qtai.domain.member.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Member.reactivate 재활성화 도메인 규칙(P2): 카카오 선택 동의 미제공(null) 시 기존 값 유지.
 */
class MemberReactivateTest {

    private static Member member() {
        return Member.builder()
                .kakaoId(1000L)
                .email("old@example.com")
                .nickname("기존닉")
                .profileImageUrl("old-url")
                .build();
    }

    @Test
    @DisplayName("재활성화 시 카카오 email/profile이 null이면 기존 값을 유지한다(과거 값 소실 방지)")
    void reactivate_nullKakaoValues_keepsExisting() {
        Member m = member();

        m.reactivate(null, null);

        assertThat(m.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(m.getEmail()).isEqualTo("old@example.com");
        assertThat(m.getProfileImageUrl()).isEqualTo("old-url");
    }

    @Test
    @DisplayName("재활성화 시 카카오 email/profile이 있으면 최신 값으로 갱신한다")
    void reactivate_presentKakaoValues_updates() {
        Member m = member();

        m.reactivate("new@example.com", "new-url");

        assertThat(m.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(m.getEmail()).isEqualTo("new@example.com");
        assertThat(m.getProfileImageUrl()).isEqualTo("new-url");
    }
}
