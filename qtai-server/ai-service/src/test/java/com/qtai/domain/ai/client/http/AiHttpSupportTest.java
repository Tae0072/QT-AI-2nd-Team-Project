package com.qtai.domain.ai.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;

class AiHttpSupportTest {

    private static final String BASE_URL = "https://provider.example";
    private static final String SERVICE_TOKEN = "service-token";
    private static final String TRACEPARENT = "00-11111111111111111111111111111111-2222222222222222-01";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final JavaType sampleType = objectMapper.getTypeFactory().constructType(SampleResponse.class);

    private MockRestServiceServer server;
    private AiHttpSupport http;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();
        http = new AiHttpSupport(restTemplate, objectMapper, BASE_URL + "/", SERVICE_TOKEN, "study");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        server.verify();
    }

    @Test
    void getSendsAuthAndTraceHeadersAndParsesSuccessEnvelope() {
        MDC.put("traceparent", TRACEPARENT);
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put("book", "JOHN");
        queryParameters.put("chapter", 3);

        server.expect(requestTo(BASE_URL + "/api/v1/system/bible/verses?book=JOHN&chapter=3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + SERVICE_TOKEN))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("traceparent", TRACEPARENT))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"value":"ok"},"error":null,"timestamp":"2026-06-08T00:00:00Z","traceId":"trace-1"}
                        """, MediaType.APPLICATION_JSON));

        SampleResponse response = http.get("/api/v1/system/bible/verses", queryParameters, sampleType);

        assertThat(response.value()).isEqualTo("ok");
    }

    @Test
    void postVoidSendsProvidedIdempotencyKey() {
        String idempotencyKey = http.idempotencyKey("study.publish", 100L);

        server.expect(requestTo(BASE_URL + "/api/v1/system/study/verse-explanations:publish"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SERVICE_TOKEN))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("Idempotency-Key", idempotencyKey))
                .andRespond(withSuccess("""
                        {"success":true,"data":null,"error":null,"timestamp":"2026-06-08T00:00:00Z","traceId":"trace-1"}
                        """, MediaType.APPLICATION_JSON));

        http.postVoid(
                "/api/v1/system/study/verse-explanations:publish",
                Map.of("aiAssetId", 100L),
                idempotencyKey
        );
    }

    @Test
    void idempotencyKeyIsStableForSameLogicalCommand() {
        String first = http.idempotencyKey("study.publish", 100L, "approved");
        String second = http.idempotencyKey("study.publish", 100L, "approved");
        String different = http.idempotencyKey("study.publish", 101L, "approved");

        assertThat(second).isEqualTo(first);
        assertThat(different).isNotEqualTo(first);
        assertThat(first).startsWith("study:study.publish:");
        assertThat(first).doesNotContain("100");
    }

    @Test
    void legacyBooleanIdempotencyPathUsesDeterministicKeyInsteadOfRandomUuid() {
        Map<String, Long> body = Map.of("aiAssetId", 100L);
        String expectedKey = http.idempotencyKey("/api/v1/system/study/verse-explanations:hide", body);

        server.expect(twice(), requestTo(BASE_URL + "/api/v1/system/study/verse-explanations:hide"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", expectedKey))
                .andRespond(withSuccess("""
                        {"success":true,"data":null,"error":null,"timestamp":"2026-06-08T00:00:00Z","traceId":"trace-1"}
                        """, MediaType.APPLICATION_JSON));

        http.postVoid("/api/v1/system/study/verse-explanations:hide", body, true);
        http.postVoid("/api/v1/system/study/verse-explanations:hide", body, true);
    }

    @Test
    void errorEnvelopeMapsToAiClientExceptionAndAcceptsFields() {
        server.expect(requestTo(BASE_URL + "/api/v1/system/study/verse-explanations:publish"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "success": false,
                                  "data": null,
                                  "error": {
                                    "code": "FORBIDDEN",
                                    "message": "role is not allowed",
                                    "fields": {"reason": "contract-test"}
                                  },
                                  "timestamp": "2026-06-08T00:00:00Z",
                                  "traceId": "trace-1"
                                }
                                """));

        assertThatThrownBy(() -> http.postVoid(
                "/api/v1/system/study/verse-explanations:publish",
                Map.of("aiAssetId", 100L),
                http.idempotencyKey("study.publish", 100L)
        ))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> {
                    AiClientException aiClientException = (AiClientException) exception;
                    assertThat(aiClientException.failureCode()).isEqualTo(FailureCode.FORBIDDEN);
                    assertThat(aiClientException.downstreamService()).isEqualTo("study");
                    assertThat(aiClientException.getMessage()).contains("role is not allowed");
                });
    }

    @Test
    void malformedEnvelopeMapsToResponseMappingFailure() {
        server.expect(requestTo(BASE_URL + "/api/v1/system/study/verse-explanations:publish"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> http.postVoid(
                "/api/v1/system/study/verse-explanations:publish",
                Map.of("aiAssetId", 100L),
                http.idempotencyKey("study.publish", 100L)
        ))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> {
                    AiClientException aiClientException = (AiClientException) exception;
                    assertThat(aiClientException.failureCode()).isEqualTo(FailureCode.RESPONSE_MAPPING_FAILED);
                    assertThat(aiClientException.downstreamService()).isEqualTo("study");
                });
    }

    private record SampleResponse(String value) {
    }
}
