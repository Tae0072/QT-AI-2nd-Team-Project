package com.qtai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MSA API Gateway (Spring Cloud Gateway).
 *
 * <p>모든 사용자 요청의 진입점. JWT 1차 검증·라우팅을 담당한다(인증 필터는 후속 증분).
 * 현재는 Strangler 단계로 {@code /api/v1/**}를 모놀리식 {@code qtai-server}로 전달한다.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
