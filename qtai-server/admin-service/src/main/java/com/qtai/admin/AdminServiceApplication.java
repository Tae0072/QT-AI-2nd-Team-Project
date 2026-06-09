package com.qtai.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * admin-service 진입점 (MSA Phase 1, 관리자 웹 백엔드 + 감사).
 *
 * <p>스캐폴드 단계 — 독립 부팅과 health 만 제공한다. persistence(전용 DataSource/EMF)와 도메인 빈
 * (admin/audit)은 후속 작업에서 {@code @ConditionalOnProperty}로 게이트된 설정으로 추가한다
 * (bible-service/ai-service skeleton 패턴).
 */
@SpringBootApplication
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
