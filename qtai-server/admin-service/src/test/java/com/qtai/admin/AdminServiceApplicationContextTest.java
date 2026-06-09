package com.qtai.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * admin-service 스캐폴드 스모크 — 기본값(도메인/DB 미탑재)으로 스프링 컨텍스트가 로드되는지 검증한다.
 * 컨텍스트 로드 성공 자체가 단언이다(bible-service {@code BibleServiceApplicationContextTest} 패턴).
 */
@SpringBootTest(classes = AdminServiceApplication.class)
class AdminServiceApplicationContextTest {

    @Test
    void contextLoadsWithScaffoldDefaults() {
        // 컨텍스트 로드 성공이 곧 단언.
    }
}
