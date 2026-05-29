package com.qtai.external.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

/**
 * DeepSeek OpenAI-compatible API 호출 구현체.
 */
@Component
public class DeepSeekLlmClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;

    @Autowired
    public DeepSeekLlmClient(
            @Value("${external.llm.deepseek.api-key}") String apiKey,
            @Value("${external.llm.deepseek.base-url}") String baseUrl,
            @Value("${external.llm.deepseek.model}") String defaultModel,
            @Value("${external.llm.deepseek.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${external.llm.deepseek.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restTemplate = new RestTemplate(factory);
        this.apiKey = apiKey;
        this.baseUrl = requireText(baseUrl, "baseUrl");
        this.defaultModel = requireText(defaultModel, "defaultModel");
    }

    DeepSeekLlmClient(RestTemplate restTemplate, String apiKey, String baseUrl, String defaultModel) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = requireText(baseUrl, "baseUrl");
        this.defaultModel = requireText(defaultModel, "defaultModel");
    }

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        validateRequest(request);
        if (isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek API key is not configured");
        }

        String model = defaultIfBlank(request.model(), defaultModel);
        Map<String, Object> requestBody = buildRequestBody(request, model);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    completionsUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );
            return parseResponse(response.getBody(), model);
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "DeepSeek API request failed: status=" + exception.getStatusCode().value()
            );
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek API request failed");
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek API request failed");
        }
    }

    private static void validateRequest(LlmCompletionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "request must not be null");
        }
        if (isBlank(request.prompt())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "prompt must not be blank");
        }
    }

    private static Map<String, Object> buildRequestBody(LlmCompletionRequest request, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages(request));
        if (request.maxTokens() != null) {
            body.put("max_tokens", request.maxTokens());
        }
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        body.put("stream", false);
        return body;
    }

    private static List<Map<String, String>> messages(LlmCompletionRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (!isBlank(request.systemPrompt())) {
            messages.add(message("system", request.systemPrompt()));
        }
        messages.add(message("user", request.prompt()));
        return messages;
    }

    private static Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private LlmCompletionResponse parseResponse(Map<?, ?> body, String fallbackModel) {
        if (body == null) {
            throw invalidProviderResponse();
        }

        String content = extractContent(body);
        Map<?, ?> usage = optionalMap(body.get("usage"));

        return new LlmCompletionResponse(
                content,
                integerValue(usage.get("prompt_tokens")),
                integerValue(usage.get("completion_tokens")),
                integerValue(usage.get("total_tokens")),
                defaultIfBlank(stringValue(body.get("model")), fallbackModel)
        );
    }

    private static String extractContent(Map<?, ?> body) {
        Object choicesObject = body.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            throw invalidProviderResponse();
        }
        Map<?, ?> choice = requiredMap(choices.get(0));
        Map<?, ?> message = requiredMap(choice.get("message"));
        String content = stringValue(message.get("content"));
        if (isBlank(content)) {
            throw invalidProviderResponse();
        }
        return content;
    }

    private static Map<?, ?> requiredMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalidProviderResponse();
        }
        return map;
    }

    private static Map<?, ?> optionalMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private static Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value instanceof String text) {
            return text;
        }
        return null;
    }

    private String completionsUrl() {
        return trimTrailingSlash(baseUrl) + CHAT_COMPLETIONS_PATH;
    }

    private static String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException invalidProviderResponse() {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek API response is invalid");
    }
}
