package com.qtai.domain.member.client.sharing;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.sharing.api.PurgeMemberSharingDataUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * sharing 도메인 {@link PurgeMemberSharingDataUseCase}의 service-user 구현 — service-note HTTP 호출 어댑터.
 *
 * <p>보존기간 만료 회원 정리 배치(SYSTEM_BATCH)가 나눔 데이터를 삭제할 때 쓴다. sharing은 service-note 소관이라
 * api 계약만 가져와 client 어댑터로 구현한다(CLAUDE.md §4). 시스템 토큰 발급·오류 매핑은
 * {@link PurgeMemberNoteDataRestClientAdapter}와 동일 패턴. 토큰·시크릿 미로깅(§7·§9).
 */
@Component
public class PurgeMemberSharingDataRestClientAdapter implements PurgeMemberSharingDataUseCase {

    private static final ParameterizedTypeReference<ApiResponse<Integer>> COUNT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final SystemTokenProvider systemTokenProvider;

    @Autowired
    public PurgeMemberSharingDataRestClientAdapter(RestClient.Builder restClientBuilder,
                                                   ServiceEndpointsProperties endpoints,
                                                   ObjectProvider<SystemTokenProvider> systemTokenProvider) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable());
    }

    PurgeMemberSharingDataRestClientAdapter(RestClient.Builder restClientBuilder,
                                            ServiceEndpointsProperties endpoints,
                                            SystemTokenProvider systemTokenProvider) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getNoteBaseUrl()).build();
        this.systemTokenProvider = systemTokenProvider;
    }

    @Override
    public int purgeByMemberId(Long memberId) {
        String systemToken = issueSystemToken();
        try {
            ApiResponse<Integer> body = restClient.post()
                    .uri(uri -> uri.path("/api/v1/sharing/purge").queryParam("memberId", memberId).build())
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(COUNT_TYPE);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    private String issueSystemToken() {
        if (systemTokenProvider == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return systemTokenProvider.issueSystemToken();
    }

    private int unwrap(ApiResponse<Integer> body) {
        if (body == null || !body.success() || body.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }
}
