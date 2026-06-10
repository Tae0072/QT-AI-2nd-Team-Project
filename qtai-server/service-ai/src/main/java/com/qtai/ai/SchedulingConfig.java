package com.qtai.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 사전 생성/검증 파이프라인의 스케줄링 활성화 설정.
 *
 * <p>ai 도메인의 {@code @Scheduled} 워커(생성 job 폴링)·시더(00:05 해설 job 시딩)는
 * {@link EnableScheduling}이 있어야 동작한다. 이 설정을 {@code ai.scheduling.enabled=true}로
 * 게이트해 두면, 테스트·기동 검증 환경(기본 false)에서는 스케줄러가 외부 시스템(LLM·DB)을
 * 호출하지 않아 부팅이 안정적이고, 운영에서는 env로 켤 수 있다.
 *
 * <p>주의: 워커/시더 빈 자체는 항상 생성된다(@Component). 여기서는 스케줄링 트리거만 제어한다.
 */
@Configuration
@ConditionalOnProperty(name = "ai.scheduling.enabled", havingValue = "true")
@EnableScheduling
public class SchedulingConfig {
}
