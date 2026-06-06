package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.api.dto.RefreshTokenRequest;
import com.qtai.domain.member.client.kakao.KakaoApiException;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import com.qtai.security.JwtProvider;
import io.jsonwebtoken.JwtException;

/**
 * AuthService 단위 테스트.
 */
class AuthServiceTest {

    private KakaoOAuthClient kakaoOAuthClient;
    private MemberRepository memberRepository;
    private MemberAuthProviderRepository authProviderRepository;
    private RefreshTokenStore refreshTokenStore;
    private JwtProvider jwtProvider;
    private TransactionTemplate transactionTemplate;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        kakaoOAuthClient = Mockito.mock(KakaoOAuthClient.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        authProviderRepository = Mockito.mock(MemberAuthProviderRepository.class);
        refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
        jwtProvider = Mockito.mock(JwtProvider.class);

        // TransactionTemplate — 콜백을 즉시 실행하는 스텁
        transactionTemplate = Mockito.mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        authService = new AuthService(
                kakaoOAuthClient, memberRepository, authProviderRepository,
                refreshTokenStore, jwtProvider, transactionTemplate, 1209600000L);
    }

    // ── login 정상 ──

    @Test
    void login_기존회원_정상_로그인() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(12345L);
        Member member = createMember(1L, 12345L, MemberStatus.ACTIVE);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(12345L)).thenReturn(Optional.of(member));
        when(jwtProvider.issueAccessToken(1L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(1L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-jwt");
        assertThat(response.member().id()).isEqualTo(1L);
        verify(refreshTokenStore).save(eq(1L), eq("refresh-jwt"), any(Duration.class));
    }

    @Test
    void login_신규회원_자동가입() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(99999L);
        Member newMember = createMember(10L, 99999L, MemberStatus.ACTIVE);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(99999L)).thenReturn(Optional.empty());
        when(memberRepository.existsByNickname("user_99999")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(newMember);
        when(authProviderRepository.save(any(MemberAuthProvider.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.issueAccessToken(10L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(10L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.member().id()).isEqualTo(10L);
        verify(memberRepository).save(any(Member.class));
        verify(authProviderRepository).save(any(MemberAuthProvider.class));
    }

    @Test
    void login_신규회원_닉네임충돌시_임시닉네임은_20자_이내를_유지한다() {
        // 회귀 방지(P1-4): 10자리 kakaoId + 닉네임 충돌 시 기존 fallback은 24자라
        // VARCHAR(20)을 넘겨 INSERT 실패 → 잘못된 KAKAO_AUTH_FAILED로 가입 불가였다.
        long kakaoId = 9999999999L; // 10자리
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(kakaoId);
        Member newMember = createMember(10L, kakaoId, MemberStatus.ACTIVE);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(kakaoId)).thenReturn(Optional.empty());
        // 기본 닉네임은 이미 선점됨 → 충돌 fallback 경로 진입
        when(memberRepository.existsByNickname(org.mockito.ArgumentMatchers.startsWith("user_")))
                .thenReturn(false);
        when(memberRepository.existsByNickname("user_9999999999")).thenReturn(true);

        org.mockito.ArgumentCaptor<Member> memberCaptor = org.mockito.ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(memberCaptor.capture())).thenReturn(newMember);
        when(authProviderRepository.save(any(MemberAuthProvider.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.issueAccessToken(10L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(10L)).thenReturn("refresh-jwt");

        authService.login(request);

        String savedNickname = memberCaptor.getValue().getNickname();
        assertThat(savedNickname).hasSizeLessThanOrEqualTo(20);
        assertThat(savedNickname).startsWith("user_");
    }

    @Test
    void login_동시가입_경합시_재조회_성공() {
        // 첫 transactionTemplate.execute()에서 DataIntegrityViolationException 발생
        // 두 번째 호출에서 이미 생성된 회원 재조회 성공
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(55555L);
        Member existingMember = createMember(20L, 55555L, MemberStatus.ACTIVE);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);

        // transactionTemplate.execute()를 호출 순서에 따라 다르게 동작시킴
        // doAnswer 스타일로 작성 — when() 스타일은 기존 stub이 호출되어 NPE 발생
        AtomicInteger callCount = new AtomicInteger(0);
        Mockito.doAnswer(inv -> {
            int count = callCount.incrementAndGet();
            if (count == 1) {
                // 첫 호출: 콜백 실행 중 DataIntegrityViolationException 발생 시뮬레이션
                throw new DataIntegrityViolationException("Duplicate entry");
            }
            // 두 번째 호출: 재조회 콜백 정상 실행
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());

        // 재조회 시 이미 생성된 회원 반환
        when(memberRepository.findByKakaoId(55555L)).thenReturn(Optional.of(existingMember));
        when(jwtProvider.issueAccessToken(20L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(20L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.member().id()).isEqualTo(20L);
    }

    @Test
    void login_동시가입_재조회_경로에서도_탈퇴회원_재활성화() {
        // 재조회 블록은 login()의 재시도 경로이므로 재활성화가 적용된다
        // (refresh 경로의 "재활성화 차단" 정책과 무관함을 회귀 테스트로 고정)
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(77777L);
        Member withdrawn = createMember(30L, 77777L, MemberStatus.ACTIVE);
        withdrawn.withdraw(Clock.systemUTC());

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);

        AtomicInteger callCount = new AtomicInteger(0);
        Mockito.doAnswer(inv -> {
            if (callCount.incrementAndGet() == 1) {
                throw new DataIntegrityViolationException("Duplicate entry");
            }
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());

        when(memberRepository.findByKakaoId(77777L)).thenReturn(Optional.of(withdrawn));
        when(jwtProvider.issueAccessToken(30L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(30L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(response.member().status()).isEqualTo("ACTIVE");
    }

    // ── login 실패 ──

    @Test
    void login_탈퇴회원_재활성화_성공() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(12345L);
        Member withdrawn = createMember(1L, 12345L, MemberStatus.ACTIVE);
        withdrawn.withdraw(Clock.systemUTC());

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(12345L)).thenReturn(Optional.of(withdrawn));
        when(jwtProvider.issueAccessToken(1L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(1L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        // 2년 보존 정책 — 탈퇴 회원 재로그인은 기존 계정 재활성화
        assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(withdrawn.getWithdrawnAt()).isNull();
        assertThat(response.member().id()).isEqualTo(1L);
        assertThat(response.member().status()).isEqualTo("ACTIVE");
        // 신규 가입 경로를 타지 않는다 — auth_provider 중복 insert 방지 (M0009 회귀 방지)
        verify(memberRepository, never()).save(any(Member.class));
        verify(authProviderRepository, never()).save(any(MemberAuthProvider.class));
    }

    @Test
    void login_정지회원_MEMBER_SUSPENDED() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(12345L);
        Member suspended = createMember(1L, 12345L, MemberStatus.SUSPENDED);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(12345L)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_SUSPENDED);
    }

    @Test
    void login_카카오API_실패_KAKAO_AUTH_FAILED() {
        LoginRequest request = new LoginRequest("invalid-token");

        when(kakaoOAuthClient.getUserInfo("invalid-token"))
                .thenThrow(new KakaoApiException("카카오 API 실패"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.KAKAO_AUTH_FAILED);
        verify(memberRepository, never()).findByKakaoId(anyLong());
    }

    // ── onboarding 판별 ──

    @Test
    void login_닉네임_미변경_회원_onboardingRequired_true() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(12345L);
        // nicknameChangedAt == null → 온보딩 미완료
        Member member = createMember(1L, 12345L, MemberStatus.ACTIVE);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(12345L)).thenReturn(Optional.of(member));
        when(jwtProvider.issueAccessToken(1L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(1L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.member().onboardingRequired()).isTrue();
    }

    @Test
    void login_닉네임_변경완료_회원_onboardingRequired_false() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(12345L);
        // nicknameChangedAt != null → 온보딩 완료 (닉네임이 user_ prefix여도 false)
        Member member = createMemberWithNicknameChanged(1L, 12345L, "user_custom_name");

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(12345L)).thenReturn(Optional.of(member));
        when(jwtProvider.issueAccessToken(1L, "USER")).thenReturn("access-jwt");
        when(jwtProvider.issueRefreshToken(1L)).thenReturn("refresh-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.member().onboardingRequired()).isFalse();
    }

    // ── logout ──

    @Test
    void logout_정상_처리() {
        authService.logout(1L);
        verify(refreshTokenStore).delete(1L);
    }

    // ── refresh 정상 ──

    @Test
    void refresh_정상_토큰_갱신() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");
        Member member = createMember(1L, 12345L, MemberStatus.ACTIVE);

        when(jwtProvider.validateRefreshToken("old-refresh")).thenReturn(1L);
        when(refreshTokenStore.find(1L)).thenReturn("old-refresh");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtProvider.issueAccessToken(1L, "USER")).thenReturn("new-access");
        when(jwtProvider.issueRefreshToken(1L)).thenReturn("new-refresh");

        LoginResponse response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenStore).save(eq(1L), eq("new-refresh"), any(Duration.class));
    }

    // ── refresh 실패 ──

    @Test
    void refresh_유효하지_않은_토큰_INVALID_REFRESH_TOKEN() {
        RefreshTokenRequest request = new RefreshTokenRequest("bad-token");

        when(jwtProvider.validateRefreshToken("bad-token"))
                .thenThrow(new JwtException("invalid"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refresh_탈취감지_세션_무효화() {
        RefreshTokenRequest request = new RefreshTokenRequest("stolen-token");

        when(jwtProvider.validateRefreshToken("stolen-token")).thenReturn(1L);
        when(refreshTokenStore.find(1L)).thenReturn("different-token"); // Redis에 다른 토큰

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenStore).delete(1L); // 전체 세션 무효화 확인
    }

    @Test
    void refresh_Redis에_토큰_없음_INVALID_REFRESH_TOKEN() {
        RefreshTokenRequest request = new RefreshTokenRequest("orphan-token");

        when(jwtProvider.validateRefreshToken("orphan-token")).thenReturn(1L);
        when(refreshTokenStore.find(1L)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refresh_정지회원_MEMBER_SUSPENDED() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");
        Member suspended = createMember(1L, 12345L, MemberStatus.SUSPENDED);

        when(jwtProvider.validateRefreshToken("valid-refresh")).thenReturn(1L);
        when(refreshTokenStore.find(1L)).thenReturn("valid-refresh");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_SUSPENDED);
        verify(refreshTokenStore).delete(1L);
    }

    @Test
    void refresh_탈퇴회원_MEMBER_ALREADY_WITHDRAWN() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");
        Member withdrawn = createMember(1L, 12345L, MemberStatus.WITHDRAWN);

        when(jwtProvider.validateRefreshToken("valid-refresh")).thenReturn(1L);
        when(refreshTokenStore.find(1L)).thenReturn("valid-refresh");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        verify(refreshTokenStore).delete(1L);
        // 정책 회귀 방지: 재활성화는 login 경로에서만 — refresh는 탈퇴 상태를 바꾸지 않는다
        assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        verify(jwtProvider, never()).issueAccessToken(anyLong(), anyString());
    }

    // ── helper ──

    private KakaoUserInfo createKakaoUserInfo(Long kakaoId) {
        return new KakaoUserInfo(kakaoId, null);
    }

    private Member createMember(Long id, Long kakaoId, MemberStatus status) {
        Member member = Member.builder()
                .kakaoId(kakaoId)
                .nickname("user_" + kakaoId)
                .build();
        setMemberFields(member, id, status, null);
        return member;
    }

    /**
     * nicknameChangedAt이 설정된 회원 생성 (온보딩 완료 상태).
     */
    private Member createMemberWithNicknameChanged(Long id, Long kakaoId, String nickname) {
        Member member = Member.builder()
                .kakaoId(kakaoId)
                .nickname(nickname)
                .build();
        setMemberFields(member, id, MemberStatus.ACTIVE, LocalDateTime.now());
        return member;
    }

    private void setMemberFields(Member member, Long id, MemberStatus status,
                                  LocalDateTime nicknameChangedAt) {
        try {
            // id는 BaseEntity(부모)에 선언
            var idField = member.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
            var statusField = member.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(member, status);
            if (nicknameChangedAt != null) {
                var nField = member.getClass().getDeclaredField("nicknameChangedAt");
                nField.setAccessible(true);
                nField.set(member, nicknameChangedAt);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
