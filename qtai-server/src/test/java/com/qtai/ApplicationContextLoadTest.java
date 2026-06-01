package com.qtai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 풀 컨텍스트 로드 가드.
 *
 * <p>배경: MigrationCoverageTest(#150)는 "엔티티↔Flyway 테이블" 정합성을 정적으로 막지만,
 * "다른 도메인 UseCase를 주입받는데 구현 빈이 없다"(= #147류 누락 빈)처럼 스프링 와이어링이
 * 깨진 경우는 잡지 못한다. 이 테스트는 전체 ApplicationContext를 실제로 기동해, 빈 누락·중복·
 * 순환 의존·설정 바인딩 오류가 PR 단계에서 드러나게 한다.
 *
 * <p>외부 의존성 없이 동작한다: test 프로파일은 H2(create-drop, Flyway off) + Redis 지연 연결 +
 * DeepSeek 더미 설정값을 사용하므로, 컨텍스트 기동만으로는 외부 네트워크에 접속하지 않는다.
 *
 * <p>한계: 런타임 동작이 아니라 "기동 가능 여부"만 검증한다. 컬럼 단위 스키마 정합성은
 * 후속 Testcontainers(MySQL) Flyway migrate+validate로 보강한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextLoadTest {

    /** 컨텍스트가 정상 기동되면(=모든 빈 와이어링 성공) 통과한다. */
    @Test
    void 애플리케이션_컨텍스트가_정상_로드된다() {
    }
}
