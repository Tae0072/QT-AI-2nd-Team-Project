package com.qtai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 설정.
 *
 * <p>{@code @Scheduled} 기반 배치(예: 미션 진행률 재계산 04:00 KST)를 동작시키기 위해 필요하다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
