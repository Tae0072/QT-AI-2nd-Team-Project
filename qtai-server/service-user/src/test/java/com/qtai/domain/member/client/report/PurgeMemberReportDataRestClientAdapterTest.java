package com.qtai.domain.member.client.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.security.SystemTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link PurgeMemberReportDataRestClientAdapter} 단위 테스트 — service-note report purge 호출 검증.
 */
class PurgeMemberReportDataRestClientAdapterTest {

    private static final String NOTE_BASE = "http://note.test";
    private static final String TEST_SYSTEM_SECRET = "test-system-secret-0123456789-abcdefghij";

    @Test
    @DisplayName("POST로 삭제 요청하고(시스템 토큰 Bearer) 삭제 행 수를 매핑한다")
    void purge_정상() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServiceEndpointsProperties endpoints = new ServiceEndpointsProperties();
        endpoints.setNoteBaseUrl(NOTE_BASE);
        var adapter = new PurgeMemberReportDataRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));

        server.expect(requestTo(startsWith(NOTE_BASE + "/api/v1/reports/purge")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess("{\"success\":true,\"data\":1}", MediaType.APPLICATION_JSON));

        assertThat(adapter.purgeByMemberId(7L)).isEqualTo(1);
        server.verify();
    }
}
