package com.qtai.domain.report.client.ai;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.ServiceCallAuthForwarder;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class CheckAiQaRequestExistsRestClientAdapter implements CheckAiQaRequestExistsClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    @Autowired
    public CheckAiQaRequestExistsRestClientAdapter(RestClient.Builder restClientBuilder,
                                                  ServiceEndpointsProperties endpoints) {
        this(restClientBuilder, endpoints, true);
    }

    CheckAiQaRequestExistsRestClientAdapter(RestClient.Builder restClientBuilder,
                                           ServiceEndpointsProperties endpoints,
                                           boolean applyTimeouts) {
        RestClient.Builder builder = restClientBuilder.baseUrl(endpoints.getAiBaseUrl());
        if (applyTimeouts) {
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(CONNECT_TIMEOUT)
                    .withReadTimeout(READ_TIMEOUT);
            builder = builder.requestFactory(ClientHttpRequestFactories.get(settings));
        }
        this.restClient = builder.build();
    }

    @Override
    public boolean exists(Long memberId, Long requestId) {
        if (memberId == null || requestId == null || requestId < 1) {
            return false;
        }
        try {
            restClient.get()
                    .uri("/api/v1/ai/qa-requests/{requestId}", requestId)
                    .headers(ServiceCallAuthForwarder::forward)
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (request, response) -> {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED);
                    })
                    .onStatus(status -> status.value() == 403 || status.value() == 404, (request, response) -> {
                        throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("AI Q&A 신고 대상 확인 실패: requestId={}, status={}",
                                requestId, response.getStatusCode());
                        throw new BusinessException(
                                ErrorCode.EXTERNAL_API_FAILURE,
                                "AI Q&A 신고 대상 확인 호출에 실패했습니다.");
                    })
                    .toBodilessEntity();
            return true;
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.REPORT_TARGET_NOT_FOUND) {
                return false;
            }
            throw e;
        } catch (RestClientException e) {
            log.warn("AI Q&A 신고 대상 확인 호출 실패: requestId={}, cause={}", requestId, e.toString());
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_FAILURE,
                    "AI Q&A 신고 대상 확인 호출에 실패했습니다.",
                    e);
        }
    }
}
