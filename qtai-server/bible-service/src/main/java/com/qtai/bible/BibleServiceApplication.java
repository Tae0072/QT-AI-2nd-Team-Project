package com.qtai.bible;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * bible-service 진입점 (MSA Phase 1, 읽기 전용 성경 참조 데이터).
 *
 * <p>도메인 빈은 @ConditionalOnProperty로 게이트된 설정에서만 활성화한다(skeleton 단계는 트래픽 오프).
 * persistence({@code qtai.bible.persistence.enabled})·inbound({@code qtai.bible.inbound.enabled}).
 */
@SpringBootApplication
@Import({
        BibleServiceInboundConfiguration.class,
        BibleServicePersistenceConfiguration.class
})
public class BibleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibleServiceApplication.class, args);
    }
}
