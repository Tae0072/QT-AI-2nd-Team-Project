package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * service-bible 전 컨트롤러 보안 통합 테스트 (MockMvc) — PR #2 이월 인수조건.
 *
 * <p>검증 범위:
 * <ul>
 *   <li>미인증 요청 → 401 (qt/study/bible/music/praise 보호 엔드포인트 전체)</li>
 *   <li>인증 요청 → 200 (빈 DB로도 정상 응답하는 읽기 엔드포인트)</li>
 *   <li>{@code /api/v1/admin/**} → ADMIN 권한이 있어도 denyAll로 403 (denyAll 회귀 방지)</li>
 * </ul>
 *
 * <p>실제 JWT 발급 없이 {@code @AuthenticationPrincipal Long memberId}만 채우면 되므로,
 * SecurityContext에 principal=Long 인증을 주입한다(실서버 JwtAuthenticationFilter와 동일 형태).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static RequestPostProcessor admin(long adminId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // ──────────────────────────── 미인증 → 401 ────────────────────────────

    @Test
    @DisplayName("미인증 요청은 보호 엔드포인트 전체에서 401")
    void 미인증_401() throws Exception {
        String[] protectedGets = {
                "/api/v1/bible/books",
                "/api/v1/music/tracks",
                "/api/v1/praise-songs",
                "/api/v1/me/praise-songs",
                "/api/v1/qt/today",
                "/api/v1/qt/passages/1",
                "/api/v1/qt/1/study-content",
                "/api/v1/qt/1/simulator"
        };
        for (String uri : protectedGets) {
            mockMvc.perform(get(uri)).andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────── 인증 → 200 ────────────────────────────

    @Test
    @DisplayName("인증된 사용자는 읽기 엔드포인트에서 200")
    void 인증_200() throws Exception {
        String[] readableGets = {
                "/api/v1/bible/books",
                "/api/v1/music/tracks",
                "/api/v1/praise-songs",
                "/api/v1/me/praise-songs",
                "/api/v1/qt/today"
        };
        for (String uri : readableGets) {
            mockMvc.perform(get(uri).with(user(123L))).andExpect(status().isOk());
        }
    }

    // ──────────────────── /api/v1/admin/** denyAll 회귀 방지 ────────────────────

    @Test
    @DisplayName("admin 경로는 미인증이면 401")
    void admin_미인증_401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/praise-songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin 경로는 ADMIN 권한이 있어도 denyAll로 403 (콘텐츠 서비스는 관리자 기능 미제공)")
    void admin_권한있어도_denyAll_403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/praise-songs")
                        .with(admin(999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
