package com.qtai.domain.report.client.ai;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.ServiceCallAuthForwarder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CheckAiQaRequestExistsRestClientAdapter implements CheckAiQaRequestExistsClient {

    private final RestClient restClient;

    public CheckAiQaRequestExistsRestClientAdapter(RestClient.Builder restClientBuilder,
                                                  ServiceEndpointsProperties endpoints) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getAiBaseUrl()).build();
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
                    .onStatus(status -> status.value() == 403 || status.value() == 404, (request, response) -> {
                        throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }
}
