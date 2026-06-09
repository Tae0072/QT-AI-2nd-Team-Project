package com.qtai.bible;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * bible-service 스캐폴드 스모크 — skeleton 기본값(persistence/inbound 비활성)으로 컨텍스트가 로드되는지 검증.
 * 컨텍스트 로드 자체가 단언이다(ai-service `AiServiceApplicationContextTest` 패턴).
 */
@SpringBootTest(classes = BibleServiceApplication.class)
class BibleServiceApplicationContextTest {

    @Test
    void contextLoadsWithDisabledSkeletonDefaults() {
        // 컨텍스트 로드 성공이 곧 단언.
    }
}
