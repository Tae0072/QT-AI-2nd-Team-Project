package com.qtai.domain.member.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * KakaoOAuthClient 단위 테스트.
 *
 * RestTemplate을 mock으로 주입하여 정상/4xx/5xx/timeout/null body 케이스를 검증한다.
 */
class KakaoOAuthClientTest {

    private RestTemplate restTemplate;
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        kakaoOAuthClient = new KakaoOAuthClient(restTemplate, "https://kapi.kakao.com/v2/user/me");
    }

    @Test
    void getUserInfo_정상_응답() {
        KakaoUserInfo expected = new KakaoUserInfo(12345L, null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KakaoUserInfo.class)))
                .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

        KakaoUserInfo result = kakaoOAuthClient.getUserInfo("valid-token");

        assertThat(result.id()).isEqualTo(12345L);
    }

    @Test
    void getUserInfo_null_body_예외() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KakaoUserInfo.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThatThrownBy(() -> kakaoOAuthClient.getUserInfo("valid-token"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("비어 있습니다");
    }

    @Test
    void getUserInfo_null_id_예외() {
        KakaoUserInfo nullId = new KakaoUserInfo(null, null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KakaoUserInfo.class)))
                .thenReturn(new ResponseEntity<>(nullId, HttpStatus.OK));

        assertThatThrownBy(() -> kakaoOAuthClient.getUserInfo("valid-token"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("비어 있습니다");
    }

    @Test
    void getUserInfo_4xx_에러_KakaoApiException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KakaoUserInfo.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> kakaoOAuthClient.getUserInfo("invalid-token"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("실패");
    }

    @Test
    void getUserInfo_5xx_에러_KakaoApiException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KakaoUserInfo.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> kakaoOAuthClient.getUserInfo("valid-token"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("실패");
    }

    @Test
    void getUserInfo_타임아웃_KakaoApiException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KakaoUserInfo.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        assertThatThrownBy(() -> kakaoOAuthClient.getUserInfo("valid-token"))
                .isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("실패");
    }
}
