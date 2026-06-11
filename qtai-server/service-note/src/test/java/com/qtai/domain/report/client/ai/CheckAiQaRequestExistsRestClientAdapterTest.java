package com.qtai.domain.report.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class CheckAiQaRequestExistsRestClientAdapterTest {

    private static final String AI_BASE_URL = "http://ai.test";

    private MockRestServiceServer server;
    private CheckAiQaRequestExistsRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ServiceEndpointsProperties endpoints = new ServiceEndpointsProperties();
        endpoints.setAiBaseUrl(AI_BASE_URL);
        adapter = new CheckAiQaRequestExistsRestClientAdapter(builder, endpoints);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returns_true_when_ai_qa_request_is_readable() {
        server.expect(requestTo(AI_BASE_URL + "/api/v1/ai/qa-requests/9"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThat(adapter.exists(1L, 9L)).isTrue();
        server.verify();
    }

    @Test
    void maps_404_to_report_target_not_found() {
        server.expect(requestTo(AI_BASE_URL + "/api/v1/ai/qa-requests/9"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.exists(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }

    @Test
    void maps_403_to_report_target_not_found() {
        server.expect(requestTo(AI_BASE_URL + "/api/v1/ai/qa-requests/9"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> adapter.exists(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }

    @Test
    void maps_5xx_to_external_api_failure() {
        server.expect(requestTo(AI_BASE_URL + "/api/v1/ai/qa-requests/9"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.exists(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    void invalid_request_id_returns_false_without_http_call() {
        assertThat(adapter.exists(1L, 0L)).isFalse();
        server.verify();
    }

    @Test
    void forwards_authorization_header() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(requestTo(AI_BASE_URL + "/api/v1/ai/qa-requests/9"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess());

        assertThat(adapter.exists(1L, 9L)).isTrue();
        server.verify();
    }
}
