package com.qtai.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * AI 서비스(ai 도메인)의 부팅 진입점. 사전 생성/검증 + F-15 단발 Q&A만 허용. Kafka는 이 서비스 전용.
 *
 * <p>Day2-5-1: 웹 스켈레톤만. ai 도메인 코드(157파일)·Kafka 워커·스케줄러·LLM external,
 * cross-domain Mock(audit·study·qt·bible·admin), 금지(자유챗봇/SSE/RAG) 부재 보장은 후속 단계.
 * <ul>
 *   <li>component scan: {@code com.qtai} — lib-common 공통 빈 + 도메인 컴포넌트</li>
 *   <li>entity/repository scan: {@code com.qtai.domain}</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.qtai")
@EntityScan(basePackages = "com.qtai.domain")
@EnableJpaRepositories(basePackages = "com.qtai.domain")
@EnableCaching
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
