package com.qtai.domain.sharing.client.member;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.ServiceCallAuthForwarder;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * member 도메인 {@link GetMemberUseCase}의 service-note 구현 — service-user HTTP 호출 어댑터.
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): member는 service-user 소관이라 service-note(sharing)는 api 계약 타입만
 * 가져와 client 어댑터로 구현한다. 회의록 2026-06-09 §3대로 통신은 RestClient(동기)만 사용한다.
 * 이 어댑터가 기존 member client mock을 대체한다.
 *
 * <p>인증(설계 §5/§81): 나눔 피드·댓글 조회는 사용자 요청 맥락이라 요청 JWT를 그대로 전달한다
 * ({@link ServiceCallAuthForwarder}). 오류(CLAUDE.md §9): {@link RestClientException}만 구체 캐치 →
 * 공통 예외(404→{@link ErrorCode#MEMBER_NOT_FOUND}, 그 외→{@link ErrorCode#EXTERNAL_API_FAILURE}).
 *
 * <p>프라이버시: 서비스 간 호출은 <b>공개 프로필만</b> 제공한다. {@link #getMember(Long)}(전체 프로필 계약)도
 * service-user의 공개 엔드포인트({@code GET /api/v1/members/{id}})를 호출해 공개 필드(id·nickname·
 * profileImageUrl)만 채우고 비공개 필드(email·status·role 등)는 null로 둔다. sharing은 닉네임만 사용한다.
 */
@Component
public class MemberRestClientAdapter implements GetMemberUseCase {

    private static final ParameterizedTypeReference<ApiResponse<MemberPublicResponse>> PUBLIC_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<MemberPublicResponse>>> PUBLIC_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public MemberRestClientAdapter(RestClient.Builder restClientBuilder,
                                   ServiceEndpointsProperties endpoints) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getUserBaseUrl()).build();
    }

    @Override
    public MemberResponse getMember(Long memberId) {
        // 서비스 간 호출은 공개 뷰만 제공(프라이버시). 비공개 필드(email·status·role 등)는 null.
        MemberPublicResponse pub = getMemberPublic(memberId);
        return new MemberResponse(
                pub.id(), pub.nickname(), null, pub.profileImageUrl(), null, null, null, null);
    }

    @Override
    public MemberPublicResponse getMemberPublic(Long memberId) {
        return get(uri -> uri.path("/api/v1/members/{id}").build(memberId), PUBLIC_TYPE);
    }

    @Override
    public List<MemberPublicResponse> getActivePublicProfiles(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return get(uri -> uri.path("/api/v1/members").queryParam("ids", memberIds).build(), PUBLIC_LIST_TYPE);
    }

    @Override
    public List<MemberPublicResponse> resolveActiveByNicknames(Collection<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            return List.of();
        }
        return get(uri -> uri.path("/api/v1/members/by-nicknames").queryParam("nicknames", nicknames).build(),
                PUBLIC_LIST_TYPE);
    }

    private <T> T get(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<ApiResponse<T>> type) {
        try {
            ApiResponse<T> body = restClient.get()
                    .uri(uriFunction)
                    .headers(ServiceCallAuthForwarder::forward)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
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
}
