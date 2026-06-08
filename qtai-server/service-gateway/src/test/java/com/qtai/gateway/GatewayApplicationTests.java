package com.qtai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 게이트웨이 컨텍스트·라우트 설정이 정상 로드되는지 검증하는 스모크 테스트.
 * 부팅 실패(잘못된 route 설정 등)를 PR 단계에서 잡는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        // ApplicationContext + Spring Cloud Gateway route 정의 로드 성공이면 통과.
    }
}
