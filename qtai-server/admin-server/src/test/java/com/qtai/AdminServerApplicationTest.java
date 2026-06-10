package com.qtai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * admin-server 부팅 스모크 — 모놀리식 통째 복사 후 admin 컨트롤러만 남긴 상태에서 컨텍스트가 정상 로드되는지 검증.
 *
 * <p>test 프로파일: H2(MySQL 모드) + ddl-auto create-drop + Flyway 비활성 + JWT 키 런타임 주입
 * (JwtTestKeysContextCustomizerFactory). 단일 DB 공유 구조이므로 전 도메인 엔티티/서비스가 함께 로드된다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminServerApplicationTest {

    @Test
    void contextLoads() {
    }
}
