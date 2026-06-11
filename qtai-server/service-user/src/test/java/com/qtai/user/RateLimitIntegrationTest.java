package com.qtai.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.member.client.kakao.KakaoApiException;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * rate limit 시큐리티 체인 통합 테스트 (PR #486 리뷰 후속 ②).
 *
 * <p>단위 테스트가 필터 단독 동작을 검증했다면, 여기서는 실제 시큐리티 필터 체인에
 * {@code RateLimitFilter}가 실제로 끼어 있고(permitAll 경로에서도 동작), 한도 초과 시
 * 컨트롤러 진입 전에 429 공통 봉투로 끊기는지 — 즉 SecurityConfig 배선을 검증한다.
 *
 * <p>Redis는 {@code StringRedisTemplate} @MockBean으로 격리해 카운트 값을 제어한다.
 *
 * <p>주의: 테스트 클래스패스에서는 {@code src/test/resources/application.yml}이 main yml을
 * 대체해 rate-limit 규칙이 비므로, 여기서 규칙을 명시 주입한다.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.rate-limit.enabled=true",
        "security.rate-limit.rules[0].path=/api/v1/auth/kakao",
        "security.rate-limit.rules[0].limit-per-minute=10"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;
    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;

    private static final String LOGIN_BODY = "{\"kakaoAccessToken\":\"dummy-kakao-token\"}";

    private void stubCount(long count) {
        when(stringRedisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .thenReturn(count);
    }

    @Test
    @DisplayName("한도 초과 시 컨트롤러 진입 전에 429 + 공통 봉투(C0007)로 차단된다")
    void 한도초과_429_공통봉투() throws Exception {
        stubCount(11L); // 한도(10/분) 초과

        mvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0007"));
        // 컨트롤러 미진입 — 카카오 클라이언트가 호출되지 않아야 하지만,
        // 호출됐다면 stub이 없어 null 응답으로 이후 단계에서 실패했을 것이므로 429 자체가 증거다.
    }

    @Test
    @DisplayName("한도 내면 필터를 통과해 컨트롤러까지 도달한다(429 아님 — 카카오 실패 401로 응답)")
    void 한도내_체인_통과() throws Exception {
        stubCount(1L);
        when(kakaoOAuthClient.getUserInfo(anyString()))
                .thenThrow(new KakaoApiException("dummy kakao 실패"));

        // 401(KAKAO_AUTH_FAILED) = 필터를 지나 AuthService(컨트롤러 이후)까지 도달했다는 증거
        mvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0009"));
    }

    @Test
    @DisplayName("Redis 장애(fail-open)에도 로그인 경로는 막히지 않는다 — 체인 통과")
    void redis장애_체인_통과() throws Exception {
        when(stringRedisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .thenThrow(new org.springframework.dao.QueryTimeoutException("redis down"));
        when(kakaoOAuthClient.getUserInfo(anyString()))
                .thenThrow(new KakaoApiException("dummy kakao 실패"));

        mvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized()); // 429가 아니라 비즈니스 응답
    }
}
