package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;

/**
 * MemberService 단위 테스트.
 *
 * <p>Clock 주입으로 시간 의존 테스트 가능.
 */
class MemberServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-26T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemberRepository memberRepository;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        memberService = new MemberService(memberRepository, FIXED_CLOCK);
    }

    // ── getMember ──

    @Test
    void getMember_성공() {
        Member member = createMember(1L, "testUser");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse response = memberService.getMember(1L);

        assertThat(response.nickname()).isEqualTo("testUser");
    }

    @Test
    void getMember_존재하지_않는_회원() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    // ── getMemberPublic ──

    @Test
    void getMemberPublic_비공개_필드_제외() {
        Member member = createMember(1L, "publicUser");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberPublicResponse response = memberService.getMemberPublic(1L);

        assertThat(response.nickname()).isEqualTo("publicUser");
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getMemberPublic_탈퇴_회원_NOT_FOUND() {
        Member member = createMember(1L, "withdrawn");
        member.withdraw(FIXED_CLOCK);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.getMemberPublic(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    // ── changeNickname ──

    @Test
    void changeNickname_성공_첫_변경() {
        Member member = createMember(1L, "oldNick");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("newNick")).thenReturn(false);

        MemberResponse response = memberService.changeNickname(1L, new NicknameChangeRequest("newNick"));

        assertThat(response.nickname()).isEqualTo("newNick");
    }

    @Test
    void changeNickname_7일_잠금_위반() {
        Member member = createMember(1L, "locked");
        // 닉네임을 방금 변경한 상태 시뮬레이션
        member.changeNickname("locked", FIXED_CLOCK);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("another")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NICKNAME_LOCKED);
    }

    @Test
    void changeNickname_중복_닉네임() {
        Member member = createMember(1L, "original");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("taken")).thenReturn(true);

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("taken")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    void changeNickname_TOCTOU_UK위반_DUPLICATE_NICKNAME() {
        Member member = createMember(1L, "original");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("race")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("UK violation"))
                .when(memberRepository).flush();

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("race")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    // ── updateProfile ──

    @Test
    void updateProfile_닉네임만_변경() {
        Member member = createMember(1L, "before");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("after")).thenReturn(false);

        MemberResponse response = memberService.updateProfile(1L,
                new ProfileUpdateRequest("after", null));

        assertThat(response.nickname()).isEqualTo("after");
        assertThat(response.profileImageUrl()).isEqualTo("https://img.test/before");
    }

    @Test
    void updateProfile_이미지만_변경() {
        Member member = createMember(1L, "keep");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse response = memberService.updateProfile(1L,
                new ProfileUpdateRequest(null, "https://new-image.png"));

        assertThat(response.nickname()).isEqualTo("keep");
        assertThat(response.profileImageUrl()).isEqualTo("https://new-image.png");
    }

    @Test
    void updateProfile_닉네임과_이미지_동시_변경() {
        Member member = createMember(1L, "old");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("new")).thenReturn(false);

        MemberResponse response = memberService.updateProfile(1L,
                new ProfileUpdateRequest("new", "https://new.png"));

        assertThat(response.nickname()).isEqualTo("new");
        assertThat(response.profileImageUrl()).isEqualTo("https://new.png");
    }

    @Test
    void updateProfile_닉네임_7일_잠금_위반() {
        Member member = createMember(1L, "locked");
        member.changeNickname("locked", FIXED_CLOCK); // 방금 변경 → 7일 잠금
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateProfile(1L,
                new ProfileUpdateRequest("newName", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NICKNAME_LOCKED);
    }

    @Test
    void updateProfile_공백_닉네임_거부() {
        Member member = createMember(1L, "valid");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateProfile(1L,
                new ProfileUpdateRequest("   ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateProfile_null_필드_미변경() {
        Member member = createMember(1L, "unchanged");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse response = memberService.updateProfile(1L,
                new ProfileUpdateRequest(null, null));

        assertThat(response.nickname()).isEqualTo("unchanged");
        assertThat(response.profileImageUrl()).isEqualTo("https://img.test/unchanged");
    }

    // ── isNicknameAvailable ──

    @Test
    void isNicknameAvailable_사용가능() {
        when(memberRepository.existsByNickname("unique")).thenReturn(false);
        assertThat(memberService.isNicknameAvailable("unique")).isTrue();
    }

    @Test
    void isNicknameAvailable_이미사용중() {
        when(memberRepository.existsByNickname("taken")).thenReturn(true);
        assertThat(memberService.isNicknameAvailable("taken")).isFalse();
    }

    // ── withdraw ──

    @Test
    void withdraw_성공_개인정보_익명화() {
        Member member = createMember(1L, "toWithdraw");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.withdraw(1L, "서비스 불만");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getNickname()).startsWith("탈퇴회원_");
        assertThat(member.getEmail()).isNull();
        assertThat(member.getKakaoId()).isEqualTo(-1L);
    }

    @Test
    void withdraw_이미_탈퇴한_회원() {
        Member member = createMember(1L, "withdrawn");
        member.withdraw(FIXED_CLOCK);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.withdraw(1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
    }

    // ── helper ──

    private Member createMember(Long id, String nickname) {
        Member member = Member.builder()
                .kakaoId(100L + id)
                .email(nickname + "@test.com")
                .nickname(nickname)
                .profileImageUrl("https://img.test/" + nickname)
                .build();
        // ID 설정 (리플렉션)
        try {
            var field = member.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return member;
    }
}
