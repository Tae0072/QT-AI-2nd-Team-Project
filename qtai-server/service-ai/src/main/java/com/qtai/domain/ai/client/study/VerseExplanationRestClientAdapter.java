package com.qtai.domain.ai.client.study;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.study.api.HidePublishedGlossaryTermsUseCase;
import com.qtai.domain.study.api.HidePublishedVerseExplanationUseCase;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.PublishApprovedGlossaryTermsUseCase;
import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * study 도메인 승인 해설 게시/숨김/조회 UseCase의 service-ai 구현 — service-bible(study) HTTP 어댑터.
 *
 * <p>AI 자산 검수(승인 게시·반려 숨김)와 해설 시딩이 study 콘텐츠(verse_explanations)에 승인본을 반영할 때 쓴다.
 * study는 service-bible 소관이라 service-ai에서는 api 계약만 가져와 client 어댑터로 구현한다(CLAUDE.md §4).
 * 한 어댑터가 {@link PublishApprovedVerseExplanationUseCase}·{@link HidePublishedVerseExplanationUseCase}·
 * {@link ListApprovedVerseExplanationUseCase} 세 계약을 구현해 기존 Mock 3종을 한 번에 대체한다(RestClient/토큰 공유).
 *
 * <p>인증(배치 경로): 사용자 JWT가 없으므로 {@link SystemTokenProvider}로 단명 SYSTEM_BATCH 토큰을 발급해 Bearer로
 * 호출한다. 수신 엔드포인트({@code /api/v1/study/verse-explanations**})는 SYSTEM_BATCH 전용이다. 시크릿 미설정 부팅을
 * 위해 {@link ObjectProvider}로 주입하고, 발급 불가 시 호출 시점에 {@link ErrorCode#EXTERNAL_API_FAILURE}로 실패한다
 * (운영 생성자 {@code @Autowired}로 모호성 제거). 광범위 catch 금지 — {@link RestClientException}만 잡는다(§9).
 * 토큰·시크릿은 로그/예외 메시지에 남기지 않는다(§7·§9).
 */
@Component
public class VerseExplanationRestClientAdapter
        implements PublishApprovedVerseExplanationUseCase,
        HidePublishedVerseExplanationUseCase,
        ListApprovedVerseExplanationUseCase,
        PublishApprovedGlossaryTermsUseCase,
        HidePublishedGlossaryTermsUseCase {

    private static final String BASE_PATH = "/api/v1/study/verse-explanations";
    private static final String GLOSSARY_BASE_PATH = "/api/v1/study/glossary-terms";

    private static final ParameterizedTypeReference<ApiResponse<PublishApprovedVerseExplanationResult>> PUBLISH_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<HidePublishedVerseExplanationResult>> HIDE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<PublishApprovedGlossaryTermsResult>>
            GLOSSARY_PUBLISH_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<HidePublishedGlossaryTermsResult>>
            GLOSSARY_HIDE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<ApprovedVerseExplanationResponse>>> LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    /** 시스템 토큰 발급기 — security.jwt.system-secret 미설정 시 null(호출 시점 실패 처리). */
    private final SystemTokenProvider systemTokenProvider;

    @Autowired
    public VerseExplanationRestClientAdapter(RestClient.Builder restClientBuilder,
                                             ServiceEndpointsProperties endpoints,
                                             ObjectProvider<SystemTokenProvider> systemTokenProvider) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable());
    }

    /** 단위 테스트용 — 해소된 {@link SystemTokenProvider}(또는 null)를 직접 주입한다. */
    VerseExplanationRestClientAdapter(RestClient.Builder restClientBuilder,
                                      ServiceEndpointsProperties endpoints,
                                      SystemTokenProvider systemTokenProvider) {
        // study는 service-bible 소속이라 bible base URL을 쓴다.
        this.restClient = restClientBuilder.baseUrl(endpoints.getBibleBaseUrl()).build();
        this.systemTokenProvider = systemTokenProvider;
    }

    @Override
    public PublishApprovedVerseExplanationResult publishApprovedVerseExplanation(
            PublishApprovedVerseExplanationCommand command) {
        String systemToken = issueSystemToken();
        try {
            ApiResponse<PublishApprovedVerseExplanationResult> body = restClient.post()
                    .uri(BASE_PATH)
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(PUBLISH_TYPE);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    @Override
    public HidePublishedVerseExplanationResult hidePublishedVerseExplanation(
            HidePublishedVerseExplanationCommand command) {
        String systemToken = issueSystemToken();
        try {
            ApiResponse<HidePublishedVerseExplanationResult> body = restClient.post()
                    .uri(BASE_PATH + "/hide")
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(HIDE_TYPE);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    @Override
    public PublishApprovedGlossaryTermsResult publishApprovedGlossaryTerms(PublishApprovedGlossaryTermsCommand command) {
        String systemToken = issueSystemToken();
        try {
            ApiResponse<PublishApprovedGlossaryTermsResult> body = restClient.post()
                    .uri(GLOSSARY_BASE_PATH)
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(GLOSSARY_PUBLISH_TYPE);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    @Override
    public HidePublishedGlossaryTermsResult hidePublishedGlossaryTerms(HidePublishedGlossaryTermsCommand command) {
        String systemToken = issueSystemToken();
        try {
            ApiResponse<HidePublishedGlossaryTermsResult> body = restClient.post()
                    .uri(GLOSSARY_BASE_PATH + "/hide")
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(GLOSSARY_HIDE_TYPE);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    @Override
    public List<ApprovedVerseExplanationResponse> listApprovedByVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }
        String systemToken = issueSystemToken();
        try {
            ApiResponse<List<ApprovedVerseExplanationResponse>> body = restClient.get()
                    .uri(uri -> uri.path(BASE_PATH).queryParam("verseIds", verseIds).build())
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(LIST_TYPE);
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
