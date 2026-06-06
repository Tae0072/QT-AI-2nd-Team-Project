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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
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
    private ApplicationEventPublisher eventPublisher;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        memberService = new MemberService(memberRepository, eventPublisher, FIXED_CLOCK);
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
    void changeNickname_앞뒤_공백_trim_적용() {
        // P2: changeNickname 경로도 updateProfile처럼 trim 일원화 — 중복 검사·저장이 trim 값 기준이어야 한다.
        Member member = createMember(1L, "oldNick");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("trimmed")).thenReturn(false);

        MemberResponse response = memberService.changeNickname(1L, new NicknameChangeRequest("  trimmed  "));

        assertThat(response.nickname()).isEqualTo("trimmed");
        verify(memberRepository).existsByNickname("trimmed"); // 공백 포함 원문이 아니라 trim 값으로 검사
    }

    @Test
    void changeNickname_공백만_입력_거부() {
        Member member = createMember(1L, "oldNick");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("   ")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
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

    @Test
    void changeNickname_시스템_예약접두사_user_차단() {
        // P1-4: 임시 닉네임 접두사 'user_' 사칭 차단
        Member member = createMember(1L, "original");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.changeNickname(1L, new NicknameChangeRequest("user_1234")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        // 예약어는 중복 조회 전에 차단된다
        verify(memberRepository, org.mockito.Mockito.never()).existsByNickname("user_1234");
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
    void withdraw_성공_상태전환_개인정보보존_세션무효화_이벤트발행() {
        Member member = createMember(1L, "toWithdraw");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.withdraw(1L, "서비스 불만");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getWithdrawnAt()).isNotNull();
        // 2년 보존 정책 — 즉시 익명화하지 않는다 (익명화 시 auth_provider UNIQUE 충돌로 재가입 영구 차단)
        assertThat(member.getNickname()).isEqualTo("toWithdraw");
        assertThat(member.getEmail()).isEqualTo("toWithdraw@test.com");
        assertThat(member.getKakaoId()).isEqualTo(101L);
        // 세션 무효화는 AFTER_COMMIT 이벤트로 분리 — 발행 여부와 대상 회원 검증
        ArgumentCaptor<MemberWithdrawnEvent> captor =
                ArgumentCaptor.forClass(MemberWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
        assertThat(captor.getValue().eventId()).isNotBlank();
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
