package com.qtai.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * QT-AI BFF Aggregator.
 *
 * <p>역할: UseCase 패턴, Bible/AI 서비스 RestClient 병렬 호출(CompletableFuture),
 *         STOMP WebSocket 알림, 입체 묵상 화면 응답 합성, 관리자 API 라우팅.
 *
 * <p>Owner: 강태오 (Lead).
 *
 * <p>금지 패턴 (AGENTS.md):
 * - 서비스 간 직접 DB 조회/JOIN 금지 → RestClient 또는 Kafka 이벤트로만
 * - WebSocket 인증은 STOMP CONNECT 헤더 (HTTP 핸드셰이크 시점 금지)
 */
@SpringBootApplication
@EnableAsync
public class BffAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffAggregatorApplication.class, args);
    }
}
