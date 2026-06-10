package com.qtai.note;

import static org.assertj.core.api.Assertions.assertThat;
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
 * 회원 데이터 정리(purge) 내부 엔드포인트(서비스 간 배치 호출용) MockMvc 통합 테스트.
 *
 * <p>service-user 보존기간 만료 배치가 호출하는 note/sharing/report purge 엔드포인트의 라우팅·권한·정상 응답을
 * 고정한다. 회원 데이터 hard delete라 <b>SYSTEM_BATCH 전용</b>이다(일반 사용자·ADMIN은 403). 빈 DB 기준 삭제 0건.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@Transactional
class MemberDataPurgeApiTest {

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
    @DisplayName("SYSTEM_BATCH는 note/sharing/report purge를 200(삭제 0건)으로 받는다")
    void 시스템배치_purge_200() throws Exception {
        for (String uri : List.of("/api/v1/notes/purge", "/api/v1/sharing/purge", "/api/v1/reports/purge")) {
            mockMvc.perform(post(uri).param("memberId", "123").with(systemBatch()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(0));
        }
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 purge가 403")
    void 사용자_403() throws Exception {
        for (String uri : List.of("/api/v1/notes/purge", "/api/v1/sharing/purge", "/api/v1/reports/purge")) {
            mockMvc.perform(post(uri).param("memberId", "123").with(user(123L)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("미인증 요청은 purge가 401 또는 403으로 차단된다")
    void 미인증_차단() throws Exception {
        for (String uri : List.of("/api/v1/notes/purge", "/api/v1/sharing/purge", "/api/v1/reports/purge")) {
            mockMvc.perform(post(uri).param("memberId", "123"))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
        }
    }
}
