package com.qtai.external.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

class DeepSeekLlmClientTest {

    private static final String API_KEY = "placeholder-credential";
    private static final String BASE_URL = "https://api.deepseek.com/";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private RestTemplate restTemplate;
    private DeepSeekLlmClient client;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        client = new DeepSeekLlmClient(restTemplate, API_KEY, BASE_URL, DEFAULT_MODEL);
    }

    @Test
    void completeMapsSuccessfulResponseAndBuildsOpenAiCompatibleRequest() {
        when(restTemplate.exchange(
                eq("https://api.deepseek.com/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(successBody("deepseek-v4-flash", "answer", 11, 7, 18)));

        LlmCompletionResponse response = client.complete(new LlmCompletionRequest(
                "deepseek-v4-pro",
                "You are concise.",
                "Explain the historical background.",
                256,
                0.2
        ));

        assertThat(response.content()).isEqualTo("answer");
        assertThat(response.model()).isEqualTo("deepseek-v4-flash");
        assertThat(response.promptTokens()).isEqualTo(11);
        assertThat(response.completionTokens()).isEqualTo(7);
        assertThat(response.totalTokens()).isEqualTo(18);

        HttpEntity<?> entity = capturedRequestEntity();
        assertThat(entity.getHeaders().getContentType().toString()).isEqualTo("application/json");
        assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + API_KEY);

        Map<String, Object> body = requestBody(entity);
        assertThat(body).containsEntry("model", "deepseek-v4-pro");
        assertThat(body).containsEntry("max_tokens", 256);
        assertThat(body).containsEntry("temperature", 0.2);
        assertThat(body).containsEntry("stream", false);

        List<?> messages = (List<?>) body.get("messages");
        assertThat(messages).hasSize(2);
        assertThat(requestMessage(messages.get(0)))
                .containsEntry("role", "system")
                .containsEntry("content", "You are concise.");
        assertThat(requestMessage(messages.get(1)))
                .containsEntry("role", "user")
                .containsEntry("content", "Explain the historical background.");
    }

    @Test
    void completeUsesDefaultModelWhenRequestModelIsBlank() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(successBody(DEFAULT_MODEL, "answer", null, null, null)));

        client.complete(new LlmCompletionRequest(" ", null, "Question", null, null));

        Map<String, Object> body = requestBody(capturedRequestEntity());
        assertThat(body).containsEntry("model", DEFAULT_MODEL);
    }

    @Test
    void completeRejectsBlankPromptBeforeCallingProvider() {
        assertThatThrownBy(() -> client.complete(new LlmCompletionRequest(null, null, " ", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        verify(restTemplate, never()).exchange(any(String.class), any(), any(), eq(Map.class));
    }

    @Test
    void completeRejectsBlankApiKeyBeforeCallingProvider() {
        DeepSeekLlmClient clientWithoutApiKey = new DeepSeekLlmClient(
                restTemplate,
                " ",
                BASE_URL,
                DEFAULT_MODEL
        );

        assertThatThrownBy(() -> clientWithoutApiKey.complete(
                new LlmCompletionRequest(null, null, "Question", null, null)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));

        verify(restTemplate, never()).exchange(any(String.class), any(), any(), eq(Map.class));
    }

    @Test
    void completeRejectsEmptyProviderBody() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> client.complete(new LlmCompletionRequest(null, null, "Question", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void completeRejectsEmptyChoices() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("choices", List.of())));

        assertThatThrownBy(() -> client.complete(new LlmCompletionRequest(null, null, "Question", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void completeRejectsBlankContent() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(successBody(DEFAULT_MODEL, " ", null, null, null)));

        assertThatThrownBy(() -> client.complete(new LlmCompletionRequest(null, null, "Question", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void completeMapsProviderHttpErrorsToInternalErrorWithoutLeakingSensitiveValues() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "untrusted-provider-payload with Question and placeholder-credential".getBytes(),
                        null
                ));

        assertThatThrownBy(() -> client.complete(
                new LlmCompletionRequest(null, null, "Question", null, null)
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
            assertThat(exception.getMessage()).doesNotContain(
                    "placeholder-credential",
                    "Question",
                    "untrusted-provider-payload"
            );
        });
    }

    @Test
    void completeMapsRateLimitAndServerErrorsToInternalError() {
        assertProviderErrorIsInternal(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));
        assertProviderErrorIsInternal(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        assertProviderErrorIsInternal(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void completeMapsTimeoutToInternalError() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        assertThatThrownBy(() -> client.complete(new LlmCompletionRequest(null, null, "Question", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    private void assertProviderErrorIsInternal(RuntimeException providerException) {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(providerException);

        assertThatThrownBy(() -> client.complete(new LlmCompletionRequest(null, null, "Question", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));

        Mockito.reset(restTemplate);
    }

    private HttpEntity<?> capturedRequestEntity() {
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                any(String.class),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(Map.class)
        );
        return entityCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requestBody(HttpEntity<?> entity) {
        return (Map<String, Object>) entity.getBody();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requestMessage(Object message) {
        return (Map<String, Object>) message;
    }

    private static Map<String, Object> successBody(
            String model,
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        return Map.of(
                "model", model,
                "choices", List.of(Map.of(
                        "message", Map.of(
                                "role", "assistant",
                                "content", content
                        )
                )),
                "usage", usage(promptTokens, completionTokens, totalTokens)
        );
    }

    private static Map<String, Object> usage(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        java.util.HashMap<String, Object> usage = new java.util.HashMap<>();
        if (promptTokens != null) {
            usage.put("prompt_tokens", promptTokens);
        }
        if (completionTokens != null) {
            usage.put("completion_tokens", completionTokens);
        }
        if (totalTokens != null) {
            usage.put("total_tokens", totalTokens);
        }
        return usage;
    }
}
