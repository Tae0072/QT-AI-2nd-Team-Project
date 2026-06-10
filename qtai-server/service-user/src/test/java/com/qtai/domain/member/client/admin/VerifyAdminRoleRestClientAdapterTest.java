package com.qtai.domain.member.client.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link VerifyAdminRoleRestClientAdapter} 단위 테스트 — admin-server 시스템 검증 호출을 MockRestServiceServer로 흉내낸다.
 *
 * <p>호출 경로/쿼리/시스템 토큰 Bearer, 성공 언랩, admin 오류 3종(AD0001/2/3) 정밀 역매핑, 그 외/연결 실패·토큰 미설정 검증.
 */
class VerifyAdminRoleRestClientAdapterTest {

    private static final String ADMIN_BASE = "http://admin.test";
    private static final String VERIFY = ADMIN_BASE + "/api/v1/system/admin/verify";
    // HS256 시크릿은 256비트(32바이트) 이상이어야 한다. 테스트 전용 더미 값(실제 시크릿 아님).
    private static final String TEST_SYSTEM_SECRET = "test-system-secret-0123456789-abcdefghij";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private ServiceEndpointsProperties endpoints;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        endpoints = new ServiceEndpointsProperties();
        endpoints.setAdminBaseUrl(ADMIN_BASE);
    }

    private VerifyAdminRoleRestClientAdapter adapter(SystemTokenProvider provider) {
        return new VerifyAdminRoleRestClientAdapter(builder, endpoints, provider, new ObjectMapper());
    }

    private SystemTokenProvider provider() {
        return new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L);
    }

    @Test
    @DisplayName("getActiveAdmin — memberId로 GET 검증 호출(시스템 토큰 Bearer) 후 관리자 정보를 언랩한다")
    void getActiveAdmin_정상() {
        server.expect(requestTo(startsWith(VERIFY)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("memberId", "12"))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"adminUserId\":100,\"memberId\":12,\"adminRole\":\"SUPER_ADMIN\"}}",
                        MediaType.APPLICATION_JSON));

        AdminUserInfo info = adapter(provider()).getActiveAdmin(12L);

        assertThat(info.adminUserId()).isEqualTo(100L);
        assertThat(info.memberId()).isEqualTo(12L);
        assertThat(info.adminRole()).isEqualTo("SUPER_ADMIN");
        server.verify();
    }

    @Test
    @DisplayName("verifyAnyRole — requiredRoles 쿼리를 함께 보낸다")
    void verifyAnyRole_requiredRoles_쿼리() {
        server.expect(requestTo(containsString("requiredRoles=OPERATOR")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("memberId", "12"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"adminUserId\":100,\"memberId\":12,\"adminRole\":\"OPERATOR\"}}",
                        MediaType.APPLICATION_JSON));

        AdminUserInfo info = adapter(provider()).verifyAnyRole(12L, List.of("OPERATOR", "REVIEWER"));

        assertThat(info.adminRole()).isEqualTo("OPERATOR");
        server.verify();
    }

    @Test
    @DisplayName("403 AD0001 → ADMIN_USER_NOT_FOUND로 역매핑한다")
    void 오류_AD0001() {
        respondError(HttpStatus.FORBIDDEN, "AD0001");
        assertThatThrownBy(() -> adapter(provider()).getActiveAdmin(12L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("403 AD0002 → ADMIN_USER_DISABLED로 역매핑한다")
    void 오류_AD0002() {
        respondError(HttpStatus.FORBIDDEN, "AD0002");
        assertThatThrownBy(() -> adapter(provider()).getActiveAdmin(12L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_USER_DISABLED);
    }

    @Test
    @DisplayName("403 AD0003 → ADMIN_ROLE_INSUFFICIENT로 역매핑한다")
    void 오류_AD0003() {
        respondError(HttpStatus.FORBIDDEN, "AD0003");
        assertThatThrownBy(() -> adapter(provider()).verifyRole(12L, "SUPER_ADMIN"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_ROLE_INSUFFICIENT);
    }

    @Test
    @DisplayName("알 수 없는 오류 코드는 EXTERNAL_API_FAILURE로 처리한다")
    void 오류_미지정코드() {
        respondError(HttpStatus.FORBIDDEN, "X9999");
        assertThatThrownBy(() -> adapter(provider()).getActiveAdmin(12L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("5xx 응답은 EXTERNAL_API_FAILURE로 변환한다")
    void 오류_5xx() {
        server.expect(requestTo(startsWith(VERIFY)))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> adapter(provider()).getActiveAdmin(12L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("시스템 토큰 발급기가 없으면(시크릿 미설정) EXTERNAL_API_FAILURE로 실패한다")
    void 토큰_미설정() {
        assertThatThrownBy(() -> adapter(null).getActiveAdmin(12L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    private void respondError(HttpStatus status, String code) {
        server.expect(requestTo(startsWith(VERIFY)))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"data\":null,\"error\":{\"code\":\"" + code
                                + "\",\"message\":\"x\"}}"));
    }
}
