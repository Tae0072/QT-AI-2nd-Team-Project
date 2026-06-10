package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 찬양 데이터 정리(purge) 내부 엔드포인트 MockMvc 통합 테스트.
 *
 * <p>service-user 보존기간 만료 배치가 호출하는 {@code POST /api/v1/praise-songs/purge}의 라우팅·권한·정상 응답을
 * 고정한다. 회원 찬양 저장 hard delete라 <b>SYSTEM_BATCH 전용</b>이다(일반 사용자·ADMIN은 403). 빈 DB 기준 삭제 0건.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PraisePurgeApiTest {

    private static final String URI = "/api/v1/praise-songs/purge";

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor systemBatch() {
        return authentication(new UsernamePasswordAuthenticationToken(
                0L, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_BATCH"))));
    }

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 찬양 purge를 200(삭제 0건)으로 받는다")
    void 시스템배치_200() throws Exception {
        mockMvc.perform(post(URI).param("memberId", "123").with(systemBatch()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 찬양 purge가 403")
    void 사용자_403() throws Exception {
        mockMvc.perform(post(URI).param("memberId", "123").with(user(123L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 요청은 401")
    void 미인증_401() throws Exception {
        mockMvc.perform(post(URI).param("memberId", "123"))
                .andExpect(status().isUnauthorized());
    }
}
