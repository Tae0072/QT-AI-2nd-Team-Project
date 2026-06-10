package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link AuthService} 단위 테스트 — 로그인(신규 가입)·토큰 갱신(차단)·로그아웃 핵심 흐름(Mockito).
 *
 * <p>보안 핵심 경로이므로 외부(Kakao)·Redis·JWT·트랜잭션을 모두 모킹하고 분기 동작만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private KakaoOAuthClient kakaoOAuthClient;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberAuthProviderRepository authProviderRepository;
    @Mock private RefreshTokenStore refreshTokenStore;
    @Mock private JwtProvider jwtProvider;
    @Mock private TransactionTemplate transactionTemplate;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(kakaoOAuthClient, memberRepository, authProviderRepository,
                refreshTokenStore, jwtProvider, transactionTemplate, 1_209_600_000L);
        // TransactionTemplate.execute(callback)가 콜백을 실제로 실행하도록 한다(트랜잭션 경계 대체).
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    private KakaoUserInfo kakaoUser(long id) {
        return new KakaoUserInfo(id, new KakaoUserInfo.KakaoAccount(
                "u@test.dev", new KakaoUserInfo.KakaoAccount.Profile("n", null)));
    }

    @Test
    void login_신규회원이면_가입하고_토큰을_발급한다() {
        when(kakaoOAuthClient.getUserInfo(anyString())).thenReturn(kakaoUser(100L));
        when(memberRepository.findByKakaoId(100L)).thenReturn(Optional.empty());
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtProvider.issueAccessToken(any(), eq("USER"))).thenReturn("atk");
        when(jwtProvider.issueRefreshToken(any())).thenReturn("rtk");

        LoginResponse response = authService.login(new LoginRequest("ktoken"));

        assertThat(response.accessToken()).isEqualTo("atk");
        assertThat(response.refreshToken()).isEqualTo("rtk");
        verify(memberRepository).save(any(Member.class));
        verify(authProviderRepository).save(any(MemberAuthProvider.class));
        verify(refreshTokenStore).save(any(), eq("rtk"), any());
    }

    @Test
    void login_카카오인증_실패면_KAKAO_AUTH_FAILED() {
        when(kakaoOAuthClient.getUserInfo(anyString()))
                .thenThrow(new KakaoApiException("카카오 호출 실패"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ktoken")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.KAKAO_AUTH_FAILED));
    }

    @Test
    void refresh_변조된_토큰이면_INVALID_REFRESH_TOKEN() {
        when(jwtProvider.validateRefreshToken("bad")).thenThrow(new JwtException("변조"));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("bad")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void refresh_탈퇴회원은_재활성화하지_않고_차단한다() {
        Member withdrawn = Member.builder().kakaoId(5L).email("e@test.dev").nickname("nick").build();
        withdrawn.withdraw(Clock.systemDefaultZone());
        when(jwtProvider.validateRefreshToken("rtk")).thenReturn(5L);
        when(refreshTokenStore.find(5L)).thenReturn("rtk");
        when(memberRepository.findById(5L)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("rtk")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN));
        verify(refreshTokenStore).delete(5L);
    }

    @Test
    void logout_리프레시토큰을_삭제한다() {
        authService.logout(7L);

        verify(refreshTokenStore).delete(7L);
    }
}
