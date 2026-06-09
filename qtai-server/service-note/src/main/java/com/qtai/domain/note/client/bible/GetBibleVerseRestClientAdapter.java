package com.qtai.domain.note.client.bible;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

/**
 * bible 도메인 {@link GetBibleVerseUseCase}의 service-note 구현 — service-bible HTTP 호출 어댑터.
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): bible은 service-bible 소관이라 service-note에서는 api 계약 타입만
 * 가져와 client 어댑터로 구현한다. 회의록 2026-06-09 §3대로 서비스 간 통신은 RestClient(동기)만 사용한다.
 * 이 어댑터가 기존 {@code GetBibleVerseUseCaseMock}을 대체한다.
 *
 * <p>인증(설계 §5/§81): bible은 요청의 JWT를 공유키로 필터 검증하므로, 노트 요청 컨텍스트에 실린
 * {@code Authorization} 헤더를 그대로 전달한다(유저 서비스 재호출 없음).
 *
 * <p>오류 처리(CLAUDE.md §9): 외부 호출 실패는 광범위 catch 대신 구체 예외({@link RestClientException})만
 * 잡아 공통 예외로 감싼다. bible이 404(구절 없음)를 주면 {@link ErrorCode#BIBLE_VERSE_NOT_FOUND},
 * 그 외 오류는 {@link ErrorCode#EXTERNAL_API_FAILURE}로 변환한다. 본문 텍스트 등 페이로드는 bible의
 * 저작권 정책에 따라 서버가 결정하며, 노트는 메타(id·book·chapter·verse)만 사용한다.
 */
@Component
public class GetBibleVerseRestClientAdapter implements GetBibleVerseUseCase {

    private static final ParameterizedTypeReference<ApiResponse<BibleVerseResponse>> VERSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<BibleVerseResponse>>> VERSE_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<BibleVerseRangeResponse>> RANGE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public GetBibleVerseRestClientAdapter(RestClient.Builder restClientBuilder,
                                          ServiceEndpointsProperties endpoints) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getBibleBaseUrl()).build();
    }

    @Override
    public BibleVerseResponse getVerse(Long verseId) {
        return get(uri -> uri.path("/api/v1/bible/verses/{verseId}").build(verseId), VERSE_TYPE);
    }

    @Override
    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }
        return get(uri -> uri.path("/api/v1/bible/verses/by-ids").queryParam("ids", verseIds).build(),
                VERSE_LIST_TYPE);
    }

    @Override
    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        return get(uri -> {
            uri.path("/api/v1/bible/verses")
                    .queryParam("bookCode", bookCode)
                    .queryParam("chapter", chapter);
            if (verseFrom != null) {
                uri.queryParam("verseFrom", verseFrom);
            }
            if (verseTo != null) {
                uri.queryParam("verseTo", verseTo);
            }
            return uri.build();
        }, RANGE_TYPE);
    }

    private <T> T get(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<ApiResponse<T>> type) {
        try {
            ApiResponse<T> body = restClient.get()
                    .uri(uriFunction)
                    .headers(this::forwardAuthorization)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(type);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    private <T> T unwrap(ApiResponse<T> body) {
        if (body == null || !body.success() || body.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }

    /** 노트 요청에 실린 Authorization 헤더를 bible 호출에 그대로 전달한다(공유키 검증, 유저 서비스 재호출 없음). */
    private void forwardAuthorization(HttpHeaders headers) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }
}
