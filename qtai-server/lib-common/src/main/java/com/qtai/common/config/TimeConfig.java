package com.qtai.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 공통 시간 설정.
 *
 * <p>시스템 시계를 빈으로 등록해 도메인 서비스가 주입받게 한다(테스트에서 시간 고정 가능).
 * 서버 기준 시간대는 Asia/Seoul (QT 공개/노출 시각 정책과 일치).
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
