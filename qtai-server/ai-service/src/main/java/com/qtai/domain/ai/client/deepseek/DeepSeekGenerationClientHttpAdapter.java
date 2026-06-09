package com.qtai.domain.ai.client.deepseek;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient.DeepSeekGenerationRequest;
import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient.DeepSeekGenerationResponse;

public final class DeepSeekGenerationClientHttpAdapter implements DeepSeekGenerationClient {

    private static final String DOWNSTREAM_SERVICE = "deepseek";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public DeepSeekGenerationClientHttpAdapter(
            String baseUrl,
            String apiKey,
            int timeoutMs,
            ObjectMapper objectMapper
    ) {
        this(createRestTemplate(timeoutMs, objectMapper), objectMapper, baseUrl, apiKey);
    }

    DeepSeekGenerationClientHttpAdapter(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            String baseUrl,
            String apiKey
    ) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.baseUrl = trimTrailingSlash(requireText(baseUrl, "baseUrl"));
        this.apiKey = requireText(apiKey, "apiKey");
    }

    @Override
    public DeepSeekGenerationResponse complete(DeepSeekGenerationRequest request) throws AiClientException {
        validateRequest(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + CHAT_COMPLETIONS_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(request), headers),
                    String.class
            );
            return parseResponse(response.getBody(), request.model());
        } catch (AiClientException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw mapStatusException(exception);
        } catch (ResourceAccessException exception) {
            throw new AiClientException(
                    FailureCode.TIMEOUT,
                    DOWNSTREAM_SERVICE,
                    "DeepSeek request timed out",
                    exception
            );
        } catch (RestClientException exception) {
            throw new AiClientException(
                    FailureCode.DOWNSTREAM_ERROR,
                    DOWNSTREAM_SERVICE,
                    "DeepSeek request failed",
                    exception
            );
        }
    }

    private Map<String, Object> requestBody(DeepSeekGenerationRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.model());
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

    private static List<Map<String, String>> messages(DeepSeekGenerationRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (!isBlank(request.systemPrompt())) {
            messages.add(message("system", request.systemPrompt()));
        }
        messages.add(message("user", request.userPrompt()));
        return messages;
    }

    private static Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private DeepSeekGenerationResponse parseResponse(String body, String fallbackModel) {
        JsonNode root = readJsonObject(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw responseMappingFailed();
        }
        String content = choices.path(0).path("message").path("content").asText(null);
        if (isBlank(content)) {
            throw responseMappingFailed();
        }
        JsonNode usage = root.path("usage");
        return new DeepSeekGenerationResponse(
                content,
                integerOrNull(usage.path("prompt_tokens")),
                integerOrNull(usage.path("completion_tokens")),
                integerOrNull(usage.path("total_tokens")),
                defaultIfBlank(root.path("model").asText(null), fallbackModel)
        );
    }

    private JsonNode readJsonObject(String body) {
        if (isBlank(body)) {
            throw responseMappingFailed();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.isObject()) {
                throw responseMappingFailed();
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new AiClientException(
                    FailureCode.RESPONSE_MAPPING_FAILED,
                    DOWNSTREAM_SERVICE,
                    "DeepSeek response mapping failed",
                    exception
            );
        }
    }

    private static void validateRequest(DeepSeekGenerationRequest request) {
        if (request == null) {
            throw validationFailed("request must not be null");
        }
        if (isBlank(request.model())) {
            throw validationFailed("model must not be blank");
        }
        if (isBlank(request.userPrompt())) {
            throw validationFailed("userPrompt must not be blank");
        }
        if (request.maxTokens() != null && request.maxTokens() < 1) {
            throw validationFailed("maxTokens must be positive");
        }
        if (request.temperature() != null && (request.temperature() < 0.0D || request.temperature() > 2.0D)) {
            throw validationFailed("temperature must be between 0.0 and 2.0");
        }
    }

    private static AiClientException validationFailed(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, DOWNSTREAM_SERVICE, message);
    }

    private static AiClientException responseMappingFailed() {
        return new AiClientException(
                FailureCode.RESPONSE_MAPPING_FAILED,
                DOWNSTREAM_SERVICE,
                "DeepSeek response mapping failed"
        );
    }

    private static AiClientException mapStatusException(HttpStatusCodeException exception) {
        int statusCode = exception.getStatusCode().value();
        FailureCode failureCode = switch (statusCode) {
            case 401 -> FailureCode.UNAUTHORIZED;
            case 403 -> FailureCode.FORBIDDEN;
            case 429 -> FailureCode.RATE_LIMITED;
            default -> exception.getStatusCode().is5xxServerError()
                    ? FailureCode.DOWNSTREAM_ERROR
                    : FailureCode.DOWNSTREAM_ERROR;
        };
        return new AiClientException(
                failureCode,
                DOWNSTREAM_SERVICE,
                "DeepSeek request failed with status " + statusCode,
                exception
        );
    }

    private static Integer integerOrNull(JsonNode node) {
        if (node.isNumber()) {
            return node.intValue();
        }
        return null;
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

    private static RestTemplate createRestTemplate(int timeoutMs, ObjectMapper objectMapper) {
        if (timeoutMs < 1) {
            throw new IllegalArgumentException("timeoutMs must be positive");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> converter.setObjectMapper(objectMapper));
        return restTemplate;
    }
}
