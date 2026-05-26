package com.qtai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.ZoneId;

/**
 * JPA Auditing 활성화 — @CreatedDate, @LastModifiedDate 자동 주입.
 * Clock 빈 — 테스트에서 시간 주입이 가능하도록 시스템 시계를 빈으로 등록한다.
 *
 * <p>CLAUDE.md §6 시간 정책(00:00/04:00 KST) 일관성을 위해 Asia/Seoul 타임존 명시.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
