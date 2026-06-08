package com.qtai.domain.ai.client.http;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;

public class AiHttpSupport {

    private static final String TRACEPARENT = "traceparent";
    private static final JavaType VOID_TYPE =
            new ObjectMapper().getTypeFactory().constructType(Void.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String downstreamService;
    private final String baseUrl;
    private final String serviceToken;

    public AiHttpSupport(
            ObjectMapper objectMapper,
            AiClientProperties properties,
            AiClientProperties.Service service,
            String downstreamService
    ) {
        this(
                createRestTemplate(properties.timeoutMsFor(service), objectMapper),
                objectMapper,
                requireText(service == null ? null : service.getBaseUrl(), downstreamService + " base-url"),
                requireText(properties.getServiceToken(), "qtai.ai.client.service-token"),
                downstreamService
        );
    }

    AiHttpSupport(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            String baseUrl,
            String serviceToken,
            String downstreamService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(requireText(baseUrl, "baseUrl"));
        this.serviceToken = requireText(serviceToken, "serviceToken");
        this.downstreamService = requireText(downstreamService, "downstreamService");
    }

    public <T> T get(String path, Map<String, ?> queryParameters, JavaType responseType) {
        return exchange(HttpMethod.GET, path, queryParameters, null, responseType, false);
    }

    public <T> T post(String path, Object body, JavaType responseType, boolean idempotencyKeyRequired) {
        return exchange(HttpMethod.POST, path, Map.of(), body, responseType, idempotencyKeyRequired);
    }

    public void postVoid(String path, Object body, boolean idempotencyKeyRequired) {
        exchange(HttpMethod.POST, path, Map.of(), body, VOID_TYPE, idempotencyKeyRequired);
    }

    private <T> T exchange(
            HttpMethod method,
            String path,
            Map<String, ?> queryParameters,
            Object body,
            JavaType responseType,
            boolean idempotencyKeyRequired
    ) {
        HttpHeaders headers = headers(body != null, idempotencyKeyRequired);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri(path, queryParameters),
                    method,
                    entity,
                    String.class
            );
            return parseSuccessEnvelope(response.getBody(), responseType);
        } catch (HttpStatusCodeException exception) {
            throw mapHttpStatusException(exception);
        } catch (ResourceAccessException exception) {
            throw new AiClientException(FailureCode.TIMEOUT, downstreamService,
                    downstreamService + " request timed out", exception);
        } catch (RestClientException exception) {
            throw new AiClientException(FailureCode.DOWNSTREAM_ERROR, downstreamService,
                    downstreamService + " request failed", exception);
        }
    }

    private HttpHeaders headers(boolean hasBody, boolean idempotencyKeyRequired) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (hasBody) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        String traceparent = MDC.get(TRACEPARENT);
        if (hasText(traceparent)) {
            headers.set(TRACEPARENT, traceparent);
        }
        if (idempotencyKeyRequired) {
            headers.set("Idempotency-Key", UUID.randomUUID().toString());
        }
        return headers;
    }

    private String uri(String path, Map<String, ?> queryParameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (queryParameters != null) {
            queryParameters.forEach((name, value) -> {
                if (value != null) {
                    builder.queryParam(name, value);
                }
            });
        }
        return builder.build().encode().toUriString();
    }

    private <T> T parseSuccessEnvelope(String body, JavaType responseType) {
        JsonNode root = readEnvelope(body);
        JsonNode success = root.get("success");
        if (success == null || !success.isBoolean()) {
            throw mappingFailure("response success flag is missing or invalid");
        }
        if (!success.booleanValue()) {
            throw errorFailure(root, FailureCode.DOWNSTREAM_ERROR);
        }
        if (Void.class.equals(responseType.getRawClass())) {
            return null;
        }
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            throw mappingFailure("response data is missing");
        }
        try {
            return objectMapper.readerFor(responseType).readValue(data);
        } catch (IOException exception) {
            throw mappingFailure("response data mapping failed", exception);
        }
    }

    private AiClientException mapHttpStatusException(HttpStatusCodeException exception) {
        JsonNode root = tryReadEnvelope(exception.getResponseBodyAsString());
        if (root != null && root.has("error")) {
            return errorFailure(root, statusFailureCode(exception));
        }
        return new AiClientException(
                statusFailureCode(exception),
                downstreamService,
                defaultIfBlank(exception.getStatusText(), downstreamService + " request failed"),
                exception
        );
    }

    private JsonNode readEnvelope(String body) {
        JsonNode root = tryReadEnvelope(body);
        if (root == null || !root.isObject()) {
            throw mappingFailure("response envelope mapping failed");
        }
        return root;
    }

    private JsonNode tryReadEnvelope(String body) {
        if (!hasText(body)) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private AiClientException errorFailure(JsonNode root, FailureCode fallbackCode) {
        JsonNode error = root.get("error");
        String code = error == null ? null : text(error.get("code"));
        String message = error == null ? null : text(error.get("message"));
        FailureCode failureCode = failureCode(code, fallbackCode);
        return new AiClientException(
                failureCode,
                downstreamService,
                defaultIfBlank(message, downstreamService + " returned error")
        );
    }

    private static FailureCode statusFailureCode(HttpStatusCodeException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401) {
            return FailureCode.UNAUTHORIZED;
        }
        if (status == 403) {
            return FailureCode.FORBIDDEN;
        }
        if (status == 404) {
            return FailureCode.NOT_FOUND;
        }
        if (status == 429) {
            return FailureCode.RATE_LIMITED;
        }
        return FailureCode.DOWNSTREAM_ERROR;
    }

    private static FailureCode failureCode(String code, FailureCode fallbackCode) {
        if (hasText(code)) {
            try {
                return FailureCode.valueOf(code);
            } catch (IllegalArgumentException ignored) {
                return fallbackCode;
            }
        }
        return fallbackCode;
    }

    private AiClientException mappingFailure(String message) {
        return new AiClientException(FailureCode.RESPONSE_MAPPING_FAILED, downstreamService, message);
    }

    private AiClientException mappingFailure(String message, Throwable cause) {
        return new AiClientException(FailureCode.RESPONSE_MAPPING_FAILED, downstreamService, message, cause);
    }

    private static RestTemplate createRestTemplate(int timeoutMs, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        RestTemplate restTemplate = new RestTemplate(factory);
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                jacksonConverter.setObjectMapper(objectMapper);
            }
        }
        return restTemplate;
    }

    private static String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalStateException(fieldName + " must not be blank in http client mode");
        }
        return value;
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String text(JsonNode node) {
        if (node != null && node.isTextual()) {
            return node.asText();
        }
        return null;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
