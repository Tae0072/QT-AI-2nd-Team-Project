package com.qtai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QT-AI API Gateway.
 *
 * <p>역할: JWT 검증, 라우팅, Rate Limit, OAuth (Google) 콜백 처리.
 * <p>참조: apis/bff/openapi.yaml (2nd-Team-Project)
 * <p>Owner: 강태오 (Lead · DevOps)
 *
 * <p>주의 (DECISIONS.md):
 * - 독립 auth-service 신규 구현 금지. JWT 발급·검증은 본 Gateway Auth 모듈에서 처리한다.
 * - Access Token 30분(1800s), Refresh Token 14일. RS256.
 * - v2.0 Modular Monolith 전환 시 본 모듈은 qtai-server/gatewayauth 패키지로 통합 예정 (ADR-0001).
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
