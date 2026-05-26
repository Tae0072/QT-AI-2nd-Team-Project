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

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

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
    private AuthService authService;

    @BeforeEach
    void setUp() {
        kakaoOAuthClient = Mockito.mock(KakaoOAuthClient.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        authProviderRepository = Mockito.mock(MemberAuthProviderRepository.class);
        refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
        jwtProvider = Mockito.mock(JwtProvider.class);

        authService = new AuthService(
                kakaoOAuthClient, memberRepository, authProviderRepository,
                refreshTokenStore, jwtProvider);
        ReflectionTestUtils.setField(authService, "refreshExpiryMs", 1209600000L);
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

    // ── login 실패 ──

    @Test
    void login_탈퇴회원_MEMBER_ALREADY_WITHDRAWN() {
        LoginRequest request = new LoginRequest("kakao-token");
        KakaoUserInfo kakaoUser = createKakaoUserInfo(12345L);
        Member withdrawn = createMember(1L, 12345L, MemberStatus.WITHDRAWN);

        when(kakaoOAuthClient.getUserInfo("kakao-token")).thenReturn(kakaoUser);
        when(memberRepository.findByKakaoId(12345L)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        verify(jwtProvider, never()).issueAccessToken(anyLong(), anyString());
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
    void refresh_비활성_회원_MEMBER_NOT_FOUND() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");
        Member suspended = createMember(1L, 12345L, MemberStatus.SUSPENDED);

        when(jwtProvider.validateRefreshToken("valid-refresh")).thenReturn(1L);
        when(refreshTokenStore.find(1L)).thenReturn("valid-refresh");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(refreshTokenStore).delete(1L);
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
        try {
            // id는 BaseEntity(부모)에 선언
            var idField = member.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
            var statusField = member.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(member, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return member;
    }
}
