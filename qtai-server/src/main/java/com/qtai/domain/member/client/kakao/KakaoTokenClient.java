package com.qtai.domain.member.client.kakao;

import com.qtai.domain.member.client.kakao.dto.KakaoTokenResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 인가 코드 → access token 교환 클라이언트 (웹 카카오 로그인, 서버 OAuth = B안).
 *
 * <p><b>DRAFT / 정책 충돌 주의</b>: 본 클래스는 서버사이드 OAuth 코드 교환을 수행한다.
 * 이는 {@code CLAUDE.md §1}의 "서버사이드 /oauth2/** 경로 미사용, Flutter SDK가 카카오 토큰을
 * 직접 받아 POST /api/v1/auth/kakao로 전달" 결정과 충돌한다. 카카오 JavaScript SDK가
 * 브라우저에서 access token을 직접 주지 않게 바뀌어(현재 Kakao.Auth.authorize 인가코드 방식),
 * 웹 로그인을 동작시키려면 서버 코드 교환이 불가피하다. 머지 전 강사/Lead 검토 필요.
 *
 * <p>redirect_uri는 보안을 위해 클라이언트 값이 아니라 서버 설정값을 사용한다(오픈 리다이렉트 방지).
 * 토큰 값은 로그에 남기지 않는다(CLAUDE.md §9).
 */
@Slf4j
@Component
public class KakaoTokenClient {

    private final RestTemplate restTemplate;
    private final String tokenUrl;
    private final String restApiKey;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoTokenClient(
            @Value("${kakao.oauth.token-url:https://kauth.kakao.com/oauth/token}") String tokenUrl,
            @Value("${kakao.oauth.rest-api-key:}") String restApiKey,
            @Value("${kakao.oauth.client-secret:}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri:}") String redirectUri) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(factory);
        this.tokenUrl = tokenUrl;
        this.restApiKey = restApiKey;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 인가 코드로 카카오 access token을 교환한다.
     *
     * @param code 카카오 인가 서버가 redirect_uri로 발급한 authorization code
     * @return 카카오 access token
     * @throws KakaoApiException 설정 누락 또는 카카오 호출 실패 시
     */
    public String getAccessTokenByCode(String code) {
        if (!StringUtils.hasText(restApiKey)) {
            throw new KakaoApiException("카카오 REST API 키가 설정되지 않았습니다(kakao.oauth.rest-api-key).");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", restApiKey);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret);
        }

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, new HttpEntity<>(form, headers), KakaoTokenResponse.class);

            KakaoTokenResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.accessToken())) {
                throw new KakaoApiException("카카오 토큰 응답에 access_token이 없습니다.");
            }
            log.info("카카오 인가코드 교환 성공(웹 로그인)");
            return body.accessToken();
        } catch (RestClientException e) {
            log.error("카카오 토큰 교환 실패(웹 로그인)", e);
            throw new KakaoApiException("카카오 토큰 교환에 실패했습니다.", e);
        }
    }
}
