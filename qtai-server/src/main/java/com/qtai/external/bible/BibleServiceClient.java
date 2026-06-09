package com.qtai.external.bible;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * bible-service HTTP 클라이언트 (MSA Inc3, mode=http일 때만 활성).
 *
 * <p>내부 SYSTEM 서비스-to-서비스 호출 — 사용자 컨텍스트 없이 {@code X-Gateway-Token}만 주입한다
 * (bible-service의 SYSTEM 주체 인증). 성공 응답은 표준 envelope({@link ApiResponse})를 언랩해 data를 반환한다.
 * <b>오류 응답(4xx/5xx)은 error 코드를 {@link ErrorCode}로 역매핑한 {@link BusinessException}으로 변환</b>해
 * in-process 호출과 동일한 예외 계약을 유지한다(소비자 동작 무변경). 매핑 불가·통신 오류는 EXTERNAL_API_FAILURE.
 */
public class BibleServiceClient {

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

    private final RestClient restClient;
    private final String gatewayToken;
    private final ObjectMapper objectMapper;

    public BibleServiceClient(RestClient restClient, String gatewayToken, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.gatewayToken = gatewayToken;
        this.objectMapper = objectMapper;
    }

    public List<BibleBookResponse> listBibleBooks() {
        return unwrap(restClient.get()
                .uri("/api/v1/bible/books")
                .header(HEADER_GATEWAY_TOKEN, gatewayToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(BOOK_LIST));
    }

    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        return unwrap(restClient.get()
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
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(VERSE_RANGE));
    }

    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        return unwrap(restClient.get()
                .uri(builder -> builder.path("/api/v1/bible/verses/by-ids")
                        .queryParam("ids", verseIds.toArray())
                        .build())
                .header(HEADER_GATEWAY_TOKEN, gatewayToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(VERSE_LIST));
    }

    private void handleError(org.springframework.http.HttpRequest request,
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
