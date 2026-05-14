package com.qtai.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * AI Service RestClient.
 *
 * <p>참고: AI SSE 자체는 모바일이 직접 Gateway → AI로 호출한다.
 * BFF는 대시보드용 IN_PROGRESS 세션 목록 조회 등 비-스트리밍 호출만 담당.
 */
@Component
public class AiClient {

    private final RestClient client;

    public AiClient(@Value("${bff.ai-service.url:http://ai-service:8085}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Map<?, ?> listInProgressSessions(String bearer) {
        return client.get().uri("/ai/sessions?status=IN_PROGRESS")
                .header("Authorization", bearer)
                .retrieve()
                .body(Map.class);
    }
}
