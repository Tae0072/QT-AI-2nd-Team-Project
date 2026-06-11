package com.qtai.external.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * {@link DeepSeekLlmClient} 단위 테스트.
 *
 * <p>외부 LLM(DeepSeek) 호출의 응답 파싱·실패 분기를 검증한다(claude-review 커버리지 지적 반영).
 * 패키지 전용 생성자로 {@code RestTemplate}을 주입해 실제 네트워크 없이 테스트한다.
 * 외부 오류는 공통 예외({@link BusinessException})로 감싸야 한다(CLAUDE.md §9).
 */
class DeepSeekLlmClientTest {

    private final LlmCompletionRequest request =
            new LlmCompletionRequest(null, "시스템 지침", "창세기의 시대 배경은?", 256, 0.2);

    private DeepSeekLlmClient client(org.springframework.web.client.RestTemplate restTemplate, String apiKey) {
        return new DeepSeekLlmClient(restTemplate, apiKey, "https://api.deepseek.com", "deepseek-chat");
    }

    @Test
    @DisplayName("정상 응답을 content/usage/model로 파싱한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void parses_successful_response() {
        var restTemplate = mock(org.springframework.web.client.RestTemplate.class);
        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "choices", List.of(Map.of("message", Map.of("content", "해설 본문"))),
                "usage", Map.of("prompt_tokens", 11, "completion_tokens", 22, "total_tokens", 33));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(body));

        LlmCompletionResponse response = client(restTemplate, "sk-test").complete(request);

        assertThat(response.content()).isEqualTo("해설 본문");
        assertThat(response.totalTokens()).isEqualTo(33);
        assertThat(response.model()).isEqualTo("deepseek-chat");
    }

    @Test
    @DisplayName("429는 LLM_RATE_LIMIT BusinessException으로 감싼다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wraps_429_as_rate_limit() {
        var restTemplate = mock(org.springframework.web.client.RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client(restTemplate, "sk-test").complete(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LLM_RATE_LIMIT");
    }

    @Test
    @DisplayName("네트워크 타임아웃(ResourceAccessException)은 LLM_TIMEOUT으로 감싼다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wraps_timeout() {
        var restTemplate = mock(org.springframework.web.client.RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("read timed out"));

        assertThatThrownBy(() -> client(restTemplate, "sk-test").complete(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LLM_TIMEOUT");
    }

    @Test
    @DisplayName("API key 미설정 시 호출 없이 LLM_CONFIGURATION_ERROR로 거부한다")
    void rejects_blank_api_key_without_call() {
        var restTemplate = mock(org.springframework.web.client.RestTemplate.class);

        assertThatThrownBy(() -> client(restTemplate, "  ").complete(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LLM_CONFIGURATION_ERROR");
    }

    @Test
    @DisplayName("choices가 없는 비정상 응답은 LLM_RESPONSE_INVALID로 거부한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void rejects_invalid_provider_response() {
        var restTemplate = mock(org.springframework.web.client.RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(Map.of("model", "deepseek-chat")));

        assertThatThrownBy(() -> client(restTemplate, "sk-test").complete(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LLM_RESPONSE_INVALID");
    }

    @Test
    @DisplayName("외부 오류는 모두 ErrorCode.INTERNAL_ERROR로 감싼다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wraps_with_internal_error_code() {
        var restTemplate = mock(org.springframework.web.client.RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        try {
            client(restTemplate, "sk-test").complete(request);
        } catch (BusinessException e) {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        }
    }
}
