package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.qtai.domain.member.api.LoginUseCase;
import com.qtai.domain.member.api.dto.KakaoCodeLoginRequest;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.client.kakao.KakaoTokenClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KakaoWebAuthService 단위 테스트 — 인가코드를 토큰으로 교환 후 기존 LoginUseCase 재사용.
 */
@ExtendWith(MockitoExtension.class)
class KakaoWebAuthServiceTest {

    @Mock
    private KakaoTokenClient kakaoTokenClient;

    @Mock
    private LoginUseCase loginUseCase;

    @InjectMocks
    private KakaoWebAuthService service;

    @Test
    void loginWithKakaoCode_인가코드를_토큰으로_교환후_기존_로그인_재사용() {
        given(kakaoTokenClient.getAccessTokenByCode("code-123")).willReturn("kakao-token-xyz");
        LoginResponse expected = new LoginResponse(
                "acc", "ref",
                new LoginResponse.MemberSummary(1L, "닉네임", "USER", "ACTIVE", false));
        given(loginUseCase.login(any(LoginRequest.class))).willReturn(expected);

        LoginResponse result = service.loginWithKakaoCode(new KakaoCodeLoginRequest("code-123"));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(loginUseCase).login(captor.capture());
        assertThat(captor.getValue().kakaoAccessToken()).isEqualTo("kakao-token-xyz");
    }
}
