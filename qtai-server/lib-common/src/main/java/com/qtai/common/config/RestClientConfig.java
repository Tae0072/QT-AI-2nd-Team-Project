package com.qtai.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 서비스 간 동기 호출용 RestClient 설정.
 *
 * <p>MSA 서비스 간 통신은 모두 RestClient(동기)로 처리한다. 읽기 중심이라
 * Kafka/보상 트랜잭션은 사용하지 않는다(회의록 2026-06-09). 각 서비스는 이 빌더를 주입받아
 * 대상 서비스 baseUrl을 지정해 클라이언트를 만든다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
