package com.qtai.bible;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정 (별도 @Configuration으로 분리 — @DataJpaTest 슬라이스에서 선택 활성 가능).
 *
 * <p>{@code BaseEntity}의 {@code @CreatedDate}/{@code @LastModifiedDate}가 공통 {@link Clock}
 * (Asia/Seoul)을 사용하도록 {@link DateTimeProvider}를 등록한다. 이를 빼면 Auditing이 시스템
 * 기본 시계를 써서 시간 정책이 어긋날 수 있다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
