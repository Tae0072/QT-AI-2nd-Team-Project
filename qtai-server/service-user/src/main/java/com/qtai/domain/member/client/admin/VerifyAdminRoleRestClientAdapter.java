package com.qtai.domain.member.client.admin;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * admin 도메인 {@link VerifyAdminRoleUseCase}의 service-user 구현 — admin-server HTTP 호출 어댑터.
 *
 * <p>admin은 admin-server 소유라 service-user는 api 계약만 가져와 client 어댑터로 호출한다(CLAUDE.md §4).
 * 이 어댑터가 기존 {@code VerifyAdminRoleUseCaseMock}(전부 ADMIN_USER_NOT_FOUND를 던지던 임시 구현)을 대체한다.
 * 관리자 카카오 로그인(task 3) 등에서 "이 회원이 활성 관리자인가/필요 역할을 갖는가"를 본 어댑터로 검증한다.
 *
 * <p>인증(시스템 경로): 관리자 검증 시점엔 전달할 사용자 JWT가 없으므로(로그인 진행 중·배치 호출) {@link SystemTokenProvider}로
 * 단명 SYSTEM_BATCH 토큰을 발급해 Bearer로 호출한다. 수신({@code GET /api/v1/system/admin/verify})은 admin-server의
 * {@code /api/v1/system/**} = SYSTEM_BATCH 전용이며, admin-server가 시스템 토큰으로 검증한다. 토큰·시크릿은 로그에 남기지 않는다(§7·§9).
 * 시크릿({@code security.jwt.system-secret}) 미설정 환경에선 {@link SystemTokenProvider} 빈이 없으므로 {@link ObjectProvider}로 주입하고,
 * 발급 불가 시 {@link ErrorCode#EXTERNAL_API_FAILURE}로 실패한다.
 *
 * <p>오류 역매핑(CLAUDE.md §9): admin 검증 실패 3종은 모두 403이라 HTTP 상태만으론 구분되지 않으므로,
 * 응답 envelope의 {@code error.code}(AD0001/AD0002/AD0003)를 읽어 각각
 * {@link ErrorCode#ADMIN_USER_NOT_FOUND}/{@link ErrorCode#ADMIN_USER_DISABLED}/{@link ErrorCode#ADMIN_ROLE_INSUFFICIENT}로
 * 복원한다(in-process 예외 계약 보존). 그 외 오류·연결 실패는 {@link ErrorCode#EXTERNAL_API_FAILURE}로 감싼다.
 */
@Component
public class VerifyAdminRoleRestClientAdapter implements VerifyAdminRoleUseCase {

    private static final ParameterizedTypeReference<ApiResponse<AdminUserInfo>> ADMIN_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /** 수신측 admin 오류 코드 → 호출측 ErrorCode 역매핑(전부 403이라 코드로 구분). */
    private static final Map<String, ErrorCode> ERROR_BY_CODE = Map.of(
            ErrorCode.ADMIN_USER_NOT_FOUND.getCode(), ErrorCode.ADMIN_USER_NOT_FOUND,
            ErrorCode.ADMIN_USER_DISABLED.getCode(), ErrorCode.ADMIN_USER_DISABLED,
            ErrorCode.ADMIN_ROLE_INSUFFICIENT.getCode(), ErrorCode.ADMIN_ROLE_INSUFFICIENT);

    private final RestClient restClient;
    /** 시스템 토큰 발급기 — security.jwt.system-secret 미설정 시 null(호출 시점에 실패 처리). */
    private final SystemTokenProvider systemTokenProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public VerifyAdminRoleRestClientAdapter(RestClient.Builder restClientBuilder,
                                            ServiceEndpointsProperties endpoints,
                                            ObjectProvider<SystemTokenProvider> systemTokenProvider,
                                            ObjectMapper objectMapper) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable(), objectMapper);
    }

    /** 단위 테스트용 — 해소된 {@link SystemTokenProvider}(또는 null)를 직접 주입한다. */
    VerifyAdminRoleRestClientAdapter(RestClient.Builder restClientBuilder,
                                     ServiceEndpointsProperties endpoints,
                                     SystemTokenProvider systemTokenProvider,
                                     ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getAdminBaseUrl()).build();
        this.systemTokenProvider = systemTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public AdminUserInfo getActiveAdmin(Long memberId) {
        return verify(memberId, null);
    }

    @Override
    public AdminUserInfo verifyRole(Long memberId, String requiredRole) {
        return verify(memberId, requiredRole == null ? null : List.of(requiredRole));
    }

    @Override
    public AdminUserInfo verifyAnyRole(Long memberId, Collection<String> requiredRoles) {
        return verify(memberId, requiredRoles);
    }

    private AdminUserInfo verify(Long memberId, Collection<String> requiredRoles) {
        String systemToken = issueSystemToken();
        try {
            ApiResponse<AdminUserInfo> body = restClient.get()
                    .uri(uri -> {
                        uri.path("/api/v1/system/admin/verify").queryParam("memberId", memberId);
                        if (requiredRoles != null && !requiredRoles.isEmpty()) {
                            uri.queryParam("requiredRoles", requiredRoles.toArray());
                        }
                        return uri.build();
                    })
                    .headers(headers -> headers.setBearerAuth(systemToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(mapError(response));
                    })
                    .body(ADMIN_TYPE);
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    /** 응답 envelope의 error.code를 읽어 admin 오류 ErrorCode로 복원한다. 읽기 실패·미지정은 EXTERNAL_API_FAILURE. */
    private ErrorCode mapError(ClientHttpResponse response) {
        String code = readErrorCode(response);
        if (code == null) {
            return ErrorCode.EXTERNAL_API_FAILURE;
        }
        return ERROR_BY_CODE.getOrDefault(code, ErrorCode.EXTERNAL_API_FAILURE);
    }

    private String readErrorCode(ClientHttpResponse response) {
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode code = root.path("error").path("code");
            return code.isTextual() ? code.asText() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private String issueSystemToken() {
        if (systemTokenProvider == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return systemTokenProvider.issueSystemToken();
    }

    private AdminUserInfo unwrap(ApiResponse<AdminUserInfo> body) {
        if (body == null || !body.success() || body.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }
}
