package com.qtai.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 폴백 엔드포인트의 공개 동작(503 + 표준 envelope) 회귀 보호.
 * Circuit Breaker가 열렸을 때 클라이언트가 받는 응답을 직접 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayFallbackControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    @DisplayName("/__fallback은 503 + 표준 에러 envelope(code·timestamp·traceId)을 반환한다")
    void fallbackReturns503StandardEnvelope() {
        webTestClient.get().uri("/__fallback")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("C0006")
                .jsonPath("$.error.message").exists()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.traceId").exists();
    }
}
