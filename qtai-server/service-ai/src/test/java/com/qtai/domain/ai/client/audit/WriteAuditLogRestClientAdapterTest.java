package com.qtai.domain.ai.client.audit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link WriteAuditLogRestClientAdapter} 단위 테스트 — admin-server 감사 기록 호출을 MockRestServiceServer로 흉내낸다.
 *
 * <p>검증: POST + 시스템 토큰 Bearer 전송, <b>fire-and-forget</b>(전송 실패·토큰 미설정에도 예외를 던지지 않음).
 */
class WriteAuditLogRestClientAdapterTest {

    private static final String ADMIN_BASE = "http://admin.test";
    private static final String AUDIT_URI = ADMIN_BASE + "/api/v1/system/audit-logs";
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

    private static AuditLogWriteRequest request() {
        return new AuditLogWriteRequest(
                null, "SYSTEM", 0L, "AI", "AI_ASSET_APPROVE", "AI_ASSET", 5001L, null, "{}");
    }

    @Test
    @DisplayName("감사 로그를 POST로 전송하고(시스템 토큰 Bearer) 정상 종료한다")
    void 기록_정상() {
        var adapter = new WriteAuditLogRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));

        server.expect(requestTo(AUDIT_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess("{\"success\":true,\"data\":null}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        assertThatCode(() -> adapter.write(request())).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("fire-and-forget — 전송이 5xx로 실패해도 예외를 던지지 않는다")
    void 전송실패_무시() {
        var adapter = new WriteAuditLogRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));

        server.expect(requestTo(AUDIT_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatCode(() -> adapter.write(request())).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("시스템 토큰 발급기가 없으면(시크릿 미설정) HTTP 호출 없이 생략한다(fire-and-forget)")
    void 토큰_미설정_생략() {
        var adapter = new WriteAuditLogRestClientAdapter(builder, endpoints, (SystemTokenProvider) null);

        assertThatCode(() -> adapter.write(request())).doesNotThrowAnyException();
        server.verify(); // 기대한 요청이 없음 = 호출이 없었음
    }
}
