package com.qtai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * service-ai 부팅 스모크 — 스프링 컨텍스트가 정상 로드되는지 검증.
 *
 * <p>테스트는 H2 인메모리에 스키마를 생성해야 하므로 ddl-auto를 create-drop으로 덮어쓴다
 * (운영 기본값은 validate).
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AiServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
