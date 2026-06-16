package com.qtai.bible;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * service-bible 부팅 스모크 — 스프링 컨텍스트가 정상 로드되는지 검증.
 *
 * <p>테스트는 H2 인메모리에 스키마를 생성해야 하므로 ddl-auto를 create-drop으로 덮어쓴다
 * (운영 기본값은 validate).
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class BibleServiceApplicationTest {

    @Autowired
    private Clock clock;

    @Test
    void contextLoads() {
    }

    @Test
    void clockBeanUsesAsiaSeoulZone() {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
