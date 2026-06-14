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

    @Test
    void changeNickname_잠금없이_연속_변경_가능하다() {
        // 2026-06-11 잠금 폐지: 방금 변경한 회원도 즉시 다시 변경할 수 있다(NICKNAME_LOCKED 미발생).
        Member member = activeMember();
        member.changeNickname("first", clock); // nicknameChangedAt = 지금
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("second")).thenReturn(false);

        MemberResponse response =
                memberService.changeNickname(1L, new NicknameChangeRequest("second"));

        assertThat(response.nickname()).isEqualTo("second");
        assertThat(response.nicknameUnlockAt()).isNull(); // 잠금 해제 시각 노출 안 함
    }

    // ── 프로필 사진 업로드/조회/삭제 ──

    @Test
    void updateProfilePhoto_정상_업로드시_저장하고_스트림URL을_채운다() {
        Member member = activeMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        byte[] png = {1, 2, 3, 4};
        MemberResponse response =
                memberService.updateProfilePhoto(1L, png, "image/png");

        assertThat(member.hasProfilePhoto()).isTrue();
        assertThat(member.getProfileImageContentType()).isEqualTo("image/png");
        assertThat(response.profileImageUrl()).startsWith("/api/v1/me/profile-photo");
    }

    @Test
    void updateProfilePhoto_이미지가_아니면_INVALID_INPUT() {
        assertThatThrownBy(
                () -> memberService.updateProfilePhoto(1L, new byte[]{1}, "application/pdf"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void updateProfilePhoto_5MB_초과면_INVALID_INPUT() {
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        assertThatThrownBy(
                () -> memberService.updateProfilePhoto(1L, tooBig, "image/jpeg"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void getOwnProfilePhoto_사진이_없으면_RESOURCE_NOT_FOUND() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember()));

        assertThatThrownBy(() -> memberService.getOwnProfilePhoto(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void deleteProfilePhoto_사진을_지운다() {
        Member member = activeMember();
        member.updateProfilePhoto(new byte[]{9}, "image/png", clock);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.deleteProfilePhoto(1L);

        assertThat(member.hasProfilePhoto()).isFalse();
        assertThat(member.getProfileImageUrl()).isNull();
    }
}
