package com.qtai.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import com.qtai.domain.member.client.kakao.dto.KakaoUserInfo;
import com.qtai.domain.member.internal.RefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * service-user 보안/엔드포인트 통합 테스트 (MockMvc + 실제 시큐리티 필터 체인).
 *
 * <p>검증 범위:
 * <ul>
 *   <li>{@code POST /api/v1/auth/kakao}는 비인증 허용(permitAll)이며 JWT를 발급한다</li>
 *   <li>인증 없이 보호 엔드포인트 접근 시 401</li>
 *   <li>발급 토큰으로 보호 엔드포인트 접근 시 200(표준 envelope)</li>
 *   <li>USER 토큰으로 {@code /api/v1/admin/**} 접근 시 403(denyAll)</li>
 * </ul>
 *
 * <p>외부 의존(Kakao HTTP, Redis)은 @MockBean으로 격리한다. @Transactional로 각 테스트 후 롤백한다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;
    @MockBean
    private RefreshTokenStore refreshTokenStore;

    private static final String LOGIN_BODY = "{\"kakaoAccessToken\":\"dummy-kakao-token\"}";

    /** 카카오 로그인 → 발급된 access token 반환(가입까지 수행). */
    private String loginAndGetAccessToken() throws Exception {
        when(kakaoOAuthClient.getUserInfo(anyString())).thenReturn(new KakaoUserInfo(
                987654321L,
                new KakaoUserInfo.KakaoAccount(
                        "user@test.dev",
                        new KakaoUserInfo.KakaoAccount.Profile("kakaoNick", null))));

        MvcResult result = mvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    @Test
    void 카카오로그인은_비인증_허용이고_토큰을_발급한다() throws Exception {
        String accessToken = loginAndGetAccessToken();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    void 토큰없이_내정보조회는_401() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효토큰으로_내정보조회는_200() throws Exception {
        String accessToken = loginAndGetAccessToken();

        // 첫 로그인은 임시 닉네임(user_…)으로 자동 가입되므로 닉네임 값 대신 결정적인 role/status로 단언한다.
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void 유효토큰으로_알림목록은_200이며_표준_페이징_envelope() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void USER토큰으로_관리자API접근은_403() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}
