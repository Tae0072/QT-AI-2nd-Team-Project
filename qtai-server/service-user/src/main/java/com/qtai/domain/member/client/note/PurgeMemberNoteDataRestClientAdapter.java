package com.qtai.domain.member.client.note;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.note.api.PurgeMemberNoteDataUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * note 도메인 {@link PurgeMemberNoteDataUseCase}의 service-user 구현 — service-note HTTP 호출 어댑터.
 *
 * <p>보존기간 만료 회원 정리 배치(SYSTEM_BATCH)가 note 데이터를 삭제할 때 쓴다. note는 service-note 소관이라
 * service-user는 api 계약만 가져와 client 어댑터로 구현한다(CLAUDE.md §4). 이 어댑터가 기존 Mock을 대체한다.
 *
 * <p>인증(배치 경로): 사용자 JWT가 없으므로 {@link SystemTokenProvider}로 단명 SYSTEM_BATCH 토큰을 발급해 Bearer로
 * 호출한다. 수신({@code POST /api/v1/notes/purge})은 SYSTEM_BATCH 전용이다. 시크릿 미설정 부팅을 위해
 * {@link ObjectProvider}로 주입하고 발급 불가 시 호출 시점에 {@link ErrorCode#EXTERNAL_API_FAILURE}로 실패한다
 * (운영 생성자 {@code @Autowired}로 모호성 제거).
 *
 * <p>분산 트랜잭션 없음(회의록 §3): 삭제는 service-note 자체 트랜잭션에서 커밋된다. 호출 실패는 광범위 catch 대신
 * {@link RestClientException}만 잡아 공통 예외로 감싼다(§9) — 배치 오케스트레이터가 회원 단위로 실패를 기록하고
 * 다음 회원을 계속 처리한다(멱등 재실행). 토큰·시크릿은 로그/예외 메시지에 남기지 않는다(§7·§9).
 */
@Component
public class PurgeMemberNoteDataRestClientAdapter implements PurgeMemberNoteDataUseCase {

    private static final ParameterizedTypeReference<ApiResponse<Integer>> COUNT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final SystemTokenProvider systemTokenProvider;

    @Autowired
    public PurgeMemberNoteDataRestClientAdapter(RestClient.Builder restClientBuilder,
                                                ServiceEndpointsProperties endpoints,
                                                ObjectProvider<SystemTokenProvider> systemTokenProvider) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable());
    }

    PurgeMemberNoteDataRestClientAdapter(RestClient.Builder restClientBuilder,
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
                    .uri(uri -> uri.path("/api/v1/notes/purge").queryParam("memberId", memberId).build())
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
