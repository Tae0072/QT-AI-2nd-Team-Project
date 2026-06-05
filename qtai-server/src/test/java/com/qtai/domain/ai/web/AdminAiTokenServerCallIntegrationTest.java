package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.domain.admin.internal.AdminRole;
import com.qtai.domain.admin.internal.AdminUser;
import com.qtai.domain.admin.internal.AdminUserRepository;
import com.qtai.security.JwtProvider;

/**
 * 관리자 API 종단 인가 통합 테스트 — 실제 JWT 발급 → 서버 호출.
 *
 * <p>회귀 배경: 기존 web 단위 테스트는 {@code ADMIN_ROLE_*} authority를 수동 주입해
 * "운영 토큰 발급 경로에는 해당 authority가 존재하지 않는" 결함(전 관리자 API 403)을
 * 잡지 못했다. 이 테스트는 {@link JwtProvider}로 발급한 진짜 토큰만으로
 * SecurityFilterChain → JwtAuthenticationFilter → 컨트롤러 →
 * VerifyAdminRoleUseCase(admin_users DB 2차 검증)까지 전 구간을 검증한다 (CLAUDE.md §5, §10).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminAiTokenServerCallIntegrationTest {

    private static final String ENDPOINT = "/api/v1/admin/ai/batch-run-logs";
    private static final long REVIEWER_MEMBER_ID = 901L;
    private static final long CONTENT_CREATOR_MEMBER_ID = 902L;
    private static final long DISABLED_MEMBER_ID = 903L;
    private static final long UNREGISTERED_MEMBER_ID = 904L;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @BeforeEach
    void setUp() {
        clearAdminUsers();
        adminUserRepository.save(AdminUser.builder()
                .memberId(REVIEWER_MEMBER_ID)
                .adminRole(AdminRole.REVIEWER)
                .build());
        adminUserRepository.save(AdminUser.builder()
                .memberId(CONTENT_CREATOR_MEMBER_ID)
                .adminRole(AdminRole.CONTENT_CREATOR)
                .build());
        AdminUser disabled = AdminUser.builder()
                .memberId(DISABLED_MEMBER_ID)
                .adminRole(AdminRole.REVIEWER)
                .build();
        disabled.disable();
        adminUserRepository.save(disabled);
        adminUserRepository.flush();
    }

    @AfterEach
    void tearDown() {
        clearAdminUsers();
    }

    @Test
    @DisplayName("ADMIN 토큰 + admin_users(REVIEWER) 등록 → 200 (종단 인가 성공)")
    void adminJwtWithRegisteredReviewerCanCallAdminApi() throws Exception {
        ResponseEntity<String> response = getWithToken(adminToken(REVIEWER_MEMBER_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").path("content").isArray()).isTrue();
    }

    @Test
    @DisplayName("ADMIN 토큰이지만 admin_users 미등록 → 403 AD0001")
    void adminJwtWithoutAdminUsersRowIsForbidden() throws Exception {
        ResponseEntity<String> response = getWithToken(adminToken(UNREGISTERED_MEMBER_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(response)).isEqualTo("AD0001");
    }

    @Test
    @DisplayName("CONTENT_CREATOR는 모니터링 권한(OPERATOR/REVIEWER) 부족 → 403 AD0003")
    void contentCreatorAdminRoleIsInsufficient() throws Exception {
        ResponseEntity<String> response = getWithToken(adminToken(CONTENT_CREATOR_MEMBER_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(response)).isEqualTo("AD0003");
    }

    @Test
    @DisplayName("비활성(DISABLED) 관리자 계정은 토큰이 유효해도 즉시 차단 → 403 AD0002")
    void disabledAdminAccountIsBlockedImmediately() throws Exception {
        ResponseEntity<String> response = getWithToken(adminToken(DISABLED_MEMBER_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(response)).isEqualTo("AD0002");
    }

    @Test
    @DisplayName("USER 토큰은 필터 레벨에서 차단 → 403 M0003")
    void userJwtIsRejectedAtFilterLevel() throws Exception {
        ResponseEntity<String> response = getWithToken(jwtProvider.issueAccessToken(1L, "USER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(response)).isEqualTo("M0003");
    }

    @Test
    @DisplayName("토큰 없음 → 401 M0002")
    void missingTokenIsUnauthorized() throws Exception {
        ResponseEntity<String> response = getWithToken(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode(response)).isEqualTo("M0002");
    }

    private ResponseEntity<String> getWithToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(ENDPOINT, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String adminToken(long memberId) {
        return jwtProvider.issueAccessToken(memberId, "ADMIN");
    }

    private String errorCode(ResponseEntity<String> response) throws Exception {
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("error").path("code").asText();
    }

    private void clearAdminUsers() {
        adminUserRepository.deleteAll();
        adminUserRepository.flush();
    }
}
