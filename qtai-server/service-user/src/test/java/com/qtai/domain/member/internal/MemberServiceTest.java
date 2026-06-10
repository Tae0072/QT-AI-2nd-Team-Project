package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link MemberService} 단위 테스트 — 조회/닉네임 변경 정책 검증(Mockito, DB 미사용).
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @InjectMocks
    private MemberService memberService;

    private Member activeMember() {
        return Member.builder().kakaoId(1L).email("e@test.dev").nickname("nick").build();
    }

    @Test
    void getMember_없는회원이면_MEMBER_NOT_FOUND() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void getMember_존재하면_응답으로_변환한다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember()));

        MemberResponse response = memberService.getMember(1L);

        assertThat(response.nickname()).isEqualTo("nick");
        assertThat(response.email()).isEqualTo("e@test.dev");
    }

    @Test
    void changeNickname_중복닉네임이면_DUPLICATE_NICKNAME() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember()));
        when(memberRepository.existsByNickname("taken")).thenReturn(true);

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("taken")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
    }

    @Test
    void changeNickname_예약접두사는_INVALID_INPUT() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember()));

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("user_hacker")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void changeNickname_탈퇴회원이면_MEMBER_ALREADY_WITHDRAWN() {
        Member withdrawn = activeMember();
        withdrawn.withdraw(clock);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("newNick")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN));
    }
}
