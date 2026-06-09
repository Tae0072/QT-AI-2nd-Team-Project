package com.qtai.domain.ai.client.deepseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient.DeepSeekGenerationRequest;
import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient.DeepSeekGenerationResponse;

class DeepSeekGenerationClientHttpAdapterTest {

    private static final String BASE_URL = "https://deepseek.example";
    private static final String API_KEY = "redacted-generation-client-value";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private MockRestServiceServer server;
    private DeepSeekGenerationClientHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();
        adapter = new DeepSeekGenerationClientHttpAdapter(
                restTemplate,
                objectMapper,
                BASE_URL + "/",
                API_KEY
        );
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void completeSendsOpenAiCompatibleRequestAndParsesResponse() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("""
                        {
                          "model": "deepseek-test-model",
                          "messages": [
                            {"role": "system", "content": "Allowed system instruction"},
                            {"role": "user", "content": "Allowed user prompt"}
                          ],
                          "max_tokens": 512,
                          "temperature": 0.2,
                          "stream": false
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-contract",
                          "model": "deepseek-test-model",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "Allowed generated content"
                              },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 10,
                            "completion_tokens": 20,
                            "total_tokens": 30
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DeepSeekGenerationResponse response = adapter.complete(new DeepSeekGenerationRequest(
                "deepseek-test-model",
                "Allowed system instruction",
                "Allowed user prompt",
                512,
                0.2D
        ));

        assertThat(response.content()).isEqualTo("Allowed generated content");
        assertThat(response.promptTokens()).isEqualTo(10);
        assertThat(response.completionTokens()).isEqualTo(20);
        assertThat(response.totalTokens()).isEqualTo(30);
        assertThat(response.model()).isEqualTo("deepseek-test-model");
    }

    @Test
    void completeOmitsOptionalSystemPromptAndGenerationOptionsWhenMissing() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "model": "deepseek-test-model",
                          "messages": [
                            {"role": "user", "content": "Allowed user prompt"}
                          ],
                          "stream": false
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {"message": {"content": "Allowed generated content"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        DeepSeekGenerationResponse response = adapter.complete(new DeepSeekGenerationRequest(
                "deepseek-test-model",
                null,
                "Allowed user prompt",
                null,
                null
        ));

        assertThat(response.content()).isEqualTo("Allowed generated content");
        assertThat(response.model()).isEqualTo("deepseek-test-model");
        assertThat(response.promptTokens()).isNull();
        assertThat(response.completionTokens()).isNull();
        assertThat(response.totalTokens()).isNull();
    }

    @Test
    void validationFailuresMapToAiClientException() {
        assertThatThrownBy(() -> adapter.complete(null))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> adapter.complete(new DeepSeekGenerationRequest(
                " ",
                null,
                "Allowed user prompt",
                null,
                null
        ))).isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> adapter.complete(new DeepSeekGenerationRequest(
                "deepseek-test-model",
                null,
                " ",
                null,
                null
        ))).isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> adapter.complete(new DeepSeekGenerationRequest(
                "deepseek-test-model",
                null,
                "Allowed user prompt",
                0,
                null
        ))).isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.VALIDATION_FAILED));
        assertThatThrownBy(() -> adapter.complete(new DeepSeekGenerationRequest(
                "deepseek-test-model",
                null,
                "Allowed user prompt",
                null,
                2.1D
        ))).isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.VALIDATION_FAILED));
    }

    @Test
    void httpStatusFailuresMapToAiClientException() {
        assertStatusMapsToFailure(HttpStatus.UNAUTHORIZED, FailureCode.UNAUTHORIZED);
        assertStatusMapsToFailure(HttpStatus.FORBIDDEN, FailureCode.FORBIDDEN);
        assertStatusMapsToFailure(HttpStatus.TOO_MANY_REQUESTS, FailureCode.RATE_LIMITED);
        assertStatusMapsToFailure(HttpStatus.INTERNAL_SERVER_ERROR, FailureCode.DOWNSTREAM_ERROR);
    }

    @Test
    void resourceAccessFailureMapsToTimeout() {
        server.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(request -> {
                    throw new ResourceAccessException("timeout");
                });

        assertThatThrownBy(() -> adapter.complete(validRequest()))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.TIMEOUT));
    }

    @Test
    void malformedResponseMapsToResponseMappingFailed() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.complete(validRequest()))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.RESPONSE_MAPPING_FAILED));
    }

    @Test
    void missingAssistantContentMapsToResponseMappingFailed() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":" "}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.complete(validRequest()))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, FailureCode.RESPONSE_MAPPING_FAILED));
    }

    private void assertStatusMapsToFailure(HttpStatus status, FailureCode failureCode) {
        server.reset();
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(status.is5xxServerError() ? withServerError() : withStatus(status));

        assertThatThrownBy(() -> adapter.complete(validRequest()))
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> assertFailure(exception, failureCode));
    }

    private static DeepSeekGenerationRequest validRequest() {
        return new DeepSeekGenerationRequest(
                "deepseek-test-model",
                "Allowed system instruction",
                "Allowed user prompt",
                512,
                0.2D
        );
    }

    private static void assertFailure(Throwable exception, FailureCode expectedFailureCode) {
        AiClientException aiClientException = (AiClientException) exception;
        assertThat(aiClientException.failureCode()).isEqualTo(expectedFailureCode);
        assertThat(aiClientException.downstreamService()).isEqualTo("deepseek");
    }
}
