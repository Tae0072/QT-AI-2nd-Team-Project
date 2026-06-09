package com.qtai.domain.ai.client.qt;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

/**
 * qt 도메인 {@link GetQtPassageContentContextUseCase}의 service-ai 구현 — service-bible(qt) HTTP 어댑터.
 *
 * <p>해설 생성 배치·00:05 시딩이 QT 본문 컨텍스트(verseId·제목·공개여부)를 가져올 때 쓴다. qt는 service-bible
 * 소관이라 service-ai에서는 api 계약만 가져와 client 어댑터로 구현한다(CLAUDE.md §4). 이 어댑터가 기존
 * {@code GetQtPassageContentContextUseCaseMock}을 대체한다.
 *
 * <p>인증(배치 경로): 사용자 JWT가 없으므로 {@link SystemTokenProvider}로 단명 SYSTEM_BATCH 토큰을 발급해
 * Bearer로 호출한다. 수신 엔드포인트({@code /api/v1/qt/.../content-context})는 SYSTEM_BATCH 전용이다.
 * 시크릿 미설정 부팅을 위해 {@link ObjectProvider}로 주입하고, 발급 불가 시 호출 시점에
 * {@link ErrorCode#EXTERNAL_API_FAILURE}로 실패한다(운영 생성자 {@code @Autowired}로 모호성 제거).
 *
 * <p>404 처리: {@code findContentContextByDate}는 "해당 날짜 본문 없음"을 {@link Optional#empty()}로 돌려줘야
 * 하므로, {@code exchange}로 404를 직접 분기해 empty로 변환한다(원 계약 의미 보존). {@code getContentContext}는
 * 404를 {@link ErrorCode#QT_PASSAGE_NOT_FOUND}로 올린다. 광범위 catch 금지 — {@link RestClientException}만 잡는다(§9).
 * 토큰·시크릿은 로그/예외 메시지에 남기지 않는다(§7·§9).
 */
@Component
public class GetQtPassageContentContextRestClientAdapter implements GetQtPassageContentContextUseCase {

    private static final ParameterizedTypeReference<ApiResponse<QtPassageContentContext>> CONTEXT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    /** 시스템 토큰 발급기 — security.jwt.system-secret 미설정 시 null(호출 시점 실패 처리). */
    private final SystemTokenProvider systemTokenProvider;

    @Autowired
    public GetQtPassageContentContextRestClientAdapter(RestClient.Builder restClientBuilder,
                                                       ServiceEndpointsProperties endpoints,
                                                       ObjectProvider<SystemTokenProvider> systemTokenProvider) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable());
    }

    /** 단위 테스트용 — 해소된 {@link SystemTokenProvider}(또는 null)를 직접 주입한다. */
    GetQtPassageContentContextRestClientAdapter(RestClient.Builder restClientBuilder,
                                                ServiceEndpointsProperties endpoints,
                                                SystemTokenProvider systemTokenProvider) {
        // qt는 service-bible 소속이라 bible base URL을 쓴다.
        this.restClient = restClientBuilder.baseUrl(endpoints.getBibleBaseUrl()).build();
        this.systemTokenProvider = systemTokenProvider;
    }

    @Override
    public QtPassageContentContext getContentContext(Long qtPassageId) {
        return exchangeForContext(uri -> uri.path("/api/v1/qt/passages/{qtPassageId}/content-context")
                .build(qtPassageId))
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
    }

    @Override
    public Optional<QtPassageContentContext> findContentContextByDate(LocalDate qtDate) {
        return exchangeForContext(uri -> uri.path("/api/v1/qt/content-context")
                .queryParam("qtDate", qtDate)
                .build());
    }

    /**
     * 컨텍스트를 조회한다. 404는 {@link Optional#empty()}로, 그 외 오류는 공통 예외로 변환한다.
     * {@code exchange}로 상태코드를 직접 분기해 404와 정상 응답을 구분한다.
     */
    private Optional<QtPassageContentContext> exchangeForContext(Function<UriBuilder, URI> uriFunction) {
        String systemToken = issueSystemToken();
        try {
            return restClient.get()
                    .uri(uriFunction)
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            return Optional.empty();
                        }
                        if (response.getStatusCode().isError()) {
                            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                        }
                        return Optional.of(unwrap(response.bodyTo(CONTEXT_TYPE)));
                    });
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

    private QtPassageContentContext unwrap(ApiResponse<QtPassageContentContext> body) {
        if (body == null || !body.success() || body.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }
}
