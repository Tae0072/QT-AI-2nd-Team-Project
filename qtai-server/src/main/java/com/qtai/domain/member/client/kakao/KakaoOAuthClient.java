package com.qtai.domain.member.client.kakao;

import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 카카오 OAuth2 사용자 정보 조회 클라이언트.
 *
 * Flutter SDK가 발급한 카카오 access token을 받아 사용자 정보를 조회한다.
 * 로그에 토큰 값을 남기지 않는다 (CLAUDE.md §9).
 */
@Slf4j
@Component
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;
    private final String userInfoUrl;

    public KakaoOAuthClient(
            @Value("${kakao.api.user-info-url}") String userInfoUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(factory);
        this.userInfoUrl = userInfoUrl;
    }

    /**
     * 카카오 access token으로 사용자 정보를 조회한다.
     *
     * @param kakaoAccessToken 카카오 SDK에서 발급받은 access token
     * @return 카카오 사용자 정보
     * @throws KakaoApiException 카카오 API 호출 실패 시
     */
    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        try {
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoUserInfo.class
            );

            KakaoUserInfo userInfo = response.getBody();
            if (userInfo == null || userInfo.id() == null) {
                throw new KakaoApiException("카카오 사용자 정보가 비어 있습니다.");
            }
            log.info("카카오 사용자 정보 조회 성공: kakaoId={}", userInfo.id());
            return userInfo;
        } catch (RestClientException e) {
            log.error("카카오 API 호출 실패", e);
            throw new KakaoApiException("카카오 사용자 정보 조회에 실패했습니다.", e);
        }
    }
}
