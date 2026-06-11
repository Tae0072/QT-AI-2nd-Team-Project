package com.qtai.external.bible;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * bible-service HTTP 클라이언트 (MSA Inc3, mode=http일 때만 활성).
 *
 * <p>내부 SYSTEM 서비스-to-서비스 호출 — 사용자 컨텍스트 없이 {@code X-Gateway-Token}만 주입한다
 * (bible-service의 SYSTEM 주체 인증). 성공 응답은 표준 envelope({@link ApiResponse})를 언랩한다.
 *
 * <p><b>오류 처리</b>: 4xx(결정적 오류)는 error 코드를 {@link ErrorCode}로 역매핑한 {@link BusinessException}으로
 * 변환하고 <b>재시도하지 않는다</b>(in-process 예외 계약 보존). 5xx·연결/타임아웃(일시 오류)은 짧은 백오프로
 * 제한 횟수 재시도하고, 소진 시 EXTERNAL_API_FAILURE로 감싼다. (CB는 컷오버 전 별도 도입 — 운영 진입 체크리스트.)
 */
public class BibleServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BibleServiceClient.class);

    private static final Map<String, ErrorCode> ERROR_CODE_BY_CODE =
            java.util.Arrays.stream(ErrorCode.values())
                    .collect(Collectors.toMap(ErrorCode::getCode, Function.identity(), (a, b) -> a));

    private static final TypeReference<ApiResponse<Void>> ERROR_ENVELOPE = new TypeReference<>() {
    };
    private static final ParameterizedTypeReference<ApiResponse<List<BibleBookResponse>>> BOOK_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<BibleVerseRangeResponse>> VERSE_RANGE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<BibleVerseResponse>>> VERSE_LIST =
            new ParameterizedTypeReference<>() {
            };

    static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    /** 재시도 폭주 방지를 위한 상한 — 설정 오류(과대값)로부터 보호. */
    static final int MAX_ATTEMPTS_CAP = 10;

    private final RestClient restClient;
    private final String gatewayToken;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final long retryBackoffMs;
    private final CircuitBreaker circuitBreaker; // nullable — 없으면 재시도만(CB 미적용)

    public BibleServiceClient(RestClient restClient, String gatewayToken, ObjectMapper objectMapper) {
        this(restClient, gatewayToken, objectMapper, 3, 100L, null);
    }

    public BibleServiceClient(RestClient restClient, String gatewayToken, ObjectMapper objectMapper,
                              int maxAttempts, long retryBackoffMs) {
        this(restClient, gatewayToken, objectMapper, maxAttempts, retryBackoffMs, null);
    }

    public BibleServiceClient(RestClient restClient, String gatewayToken, ObjectMapper objectMapper,
                              int maxAttempts, long retryBackoffMs, CircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.gatewayToken = gatewayToken;
        this.objectMapper = objectMapper;
        this.maxAttempts = Math.min(Math.max(1, maxAttempts), MAX_ATTEMPTS_CAP);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        this.circuitBreaker = circuitBreaker;
    }

    public List<BibleBookResponse> listBibleBooks() {
        return executeWithResilience("GET /api/v1/bible/books", () -> unwrap(restClient.get()
                .uri("/api/v1/bible/books")
                .header(HEADER_GATEWAY_TOKEN, gatewayToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                .body(BOOK_LIST)));
    }

    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        return executeWithResilience("GET /api/v1/bible/verses", () -> unwrap(restClient.get()
                .uri(builder -> {
                    builder.path("/api/v1/bible/verses")
                            .queryParam("bookCode", bookCode)
                            .queryParam("chapter", chapter);
                    if (verseFrom != null) {
                        builder.queryParam("verseFrom", verseFrom);
                    }
                    if (verseTo != null) {
                        builder.queryParam("verseTo", verseTo);
                    }
                    return builder.build();
                })
                .header(HEADER_GATEWAY_TOKEN, gatewayToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                .body(VERSE_RANGE)));
    }

    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        // 빈 입력은 네트워크 호출 없이 빈 결과 — in-process BibleService.getVerses(빈)과 동일 계약.
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }
        return executeWithResilience("GET /api/v1/bible/verses/by-ids", () -> unwrap(restClient.get()
                .uri(builder -> builder.path("/api/v1/bible/verses/by-ids")
                        .queryParam("ids", verseIds.toArray())
                        .build())
                .header(HEADER_GATEWAY_TOKEN, gatewayToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                .body(VERSE_LIST)));
    }

    /**
     * Circuit Breaker(설정 시)로 감싼 재시도 실행. CB가 열려 있으면 다운스트림 호출 없이 즉시 fast-fail
     * ({@code EXTERNAL_API_FAILURE})한다. CB는 일시 장애(EXTERNAL_API_FAILURE)만 실패로 기록하고
     * 4xx 역매핑은 무시한다(설정은 {@code BibleHttpClientConfiguration}). CB 미설정이면 재시도만 수행.
     */
    private <T> T executeWithResilience(String operation, Supplier<T> call) {
        if (circuitBreaker == null) {
            return withRetry(operation, call);
        }
        try {
            return circuitBreaker.executeSupplier(() -> withRetry(operation, call));
        } catch (CallNotPermittedException e) {
            log.warn("bible-service Circuit Breaker OPEN — fast-fail [{}]", operation);
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    /** 일시 오류(5xx·연결/타임아웃)만 제한 재시도. 4xx 역매핑({@link BusinessException})은 즉시 전파. */
    private <T> T withRetry(String operation, Supplier<T> call) {
        RuntimeException lastTransient = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (BusinessException e) {
                throw e; // 4xx 결정적 오류 — 재시도 안 함
            } catch (HttpServerErrorException | ResourceAccessException e) {
                lastTransient = e;
                log.debug("bible-service 호출 일시 오류 — 재시도 {}/{} [{}]: {}",
                        attempt, maxAttempts, operation, e.getClass().getSimpleName());
                if (attempt < maxAttempts && retryBackoffMs > 0) {
                    sleep(retryBackoffMs);
                }
            }
        }
        log.warn("bible-service 호출 재시도 소진({}회) [{}]: {}", maxAttempts, operation,
                lastTransient == null ? "unknown" : lastTransient.getClass().getSimpleName());
        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    private void handle4xx(org.springframework.http.HttpRequest request,
                           org.springframework.http.client.ClientHttpResponse response) throws IOException {
        ErrorCode mapped = ErrorCode.EXTERNAL_API_FAILURE;
        try {
            ApiResponse<Void> envelope = objectMapper.readValue(response.getBody(), ERROR_ENVELOPE);
            if (envelope != null && envelope.error() != null) {
                mapped = ERROR_CODE_BY_CODE.getOrDefault(envelope.error().code(), ErrorCode.EXTERNAL_API_FAILURE);
            }
        } catch (Exception ignore) {
            // 본문 파싱 실패 → 일반 외부 호출 실패로 처리(원문 노출 방지)
        }
        throw new BusinessException(mapped);
    }

    private static <T> T unwrap(ApiResponse<T> response) {
        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return response.data();
    }
}
