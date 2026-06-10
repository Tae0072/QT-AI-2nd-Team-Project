package com.qtai.domain.ai.client.bible;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

/**
 * bible 도메인 {@link GetBibleVerseUseCase}의 service-ai 구현 — service-bible HTTP 호출 어댑터.
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): bible은 service-bible 소관이라 service-ai에서는 api 계약 타입만
 * 가져와 client 어댑터로 구현한다. 회의록 2026-06-09 §3대로 서비스 간 통신은 RestClient(동기)만 쓴다.
 * 이 어댑터가 기존 {@code GetBibleVerseUseCaseMock}을 대체한다.
 *
 * <p>인증(배치 경로): 해설 생성 job은 스케줄러/배치에서 돌아 전달할 사용자 JWT가 없다. 따라서 모든 서비스가
 * 공유하는 시크릿으로 단명 HS256 SYSTEM_BATCH 토큰({@link SystemTokenProvider#issueSystemToken()})을 발급해
 * {@code Authorization: Bearer} 헤더에 실어 호출한다. service-bible의 {@code JwtAuthenticationFilter}가
 * 이 토큰을 SYSTEM_BATCH로 검증한다(사용자 RS256 경로와 분리). 시스템 토큰/시크릿은 로그에 남기지 않는다(§9).
 *
 * <p>시크릿({@code security.jwt.system-secret})이 설정된 환경에서만 {@link SystemTokenProvider} 빈이
 * 등록되므로 {@link ObjectProvider}로 주입한다. 미설정 환경(테스트·시크릿 누락)에서도 빈은 생성되지만,
 * 실제 호출 시점에 토큰을 발급할 수 없으면 {@link ErrorCode#EXTERNAL_API_FAILURE}로 실패한다.
 *
 * <p>오류 처리(CLAUDE.md §9): 광범위 catch 대신 구체 예외({@link RestClientException})만 잡아 공통 예외로
 * 감싼다. bible이 404(구절 없음)를 주면 {@link ErrorCode#BIBLE_VERSE_NOT_FOUND}, 그 외 오류는
 * {@link ErrorCode#EXTERNAL_API_FAILURE}로 변환한다.
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
    /** 시스템 토큰 발급기 — security.jwt.system-secret 미설정 시 null(호출 시점에 실패 처리). */
    private final SystemTokenProvider systemTokenProvider;

    @Autowired
    public GetBibleVerseRestClientAdapter(RestClient.Builder restClientBuilder,
                                          ServiceEndpointsProperties endpoints,
                                          ObjectProvider<SystemTokenProvider> systemTokenProvider) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable());
    }

    /** 단위 테스트용 — 해소된 {@link SystemTokenProvider}(또는 null)를 직접 주입한다. */
    GetBibleVerseRestClientAdapter(RestClient.Builder restClientBuilder,
                                   ServiceEndpointsProperties endpoints,
                                   SystemTokenProvider systemTokenProvider) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getBibleBaseUrl()).build();
        this.systemTokenProvider = systemTokenProvider;
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
        String systemToken = issueSystemToken();
        try {
            ApiResponse<T> body = restClient.get()
                    .uri(uriFunction)
                    .headers(headers -> headers.setBearerAuth(systemToken))
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

    /** 시스템 토큰을 발급한다. 시크릿 미설정 등으로 발급기가 없으면 외부 호출 불가로 처리한다. */
    private String issueSystemToken() {
        if (systemTokenProvider == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return systemTokenProvider.issueSystemToken();
    }

    private <T> T unwrap(ApiResponse<T> body) {
        if (body == null || !body.success() || body.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }
}
