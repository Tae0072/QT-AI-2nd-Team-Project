# 2026-06-09 bible-service persistence 활성 검증(Inc1b) — 결과 보고

## 요약
bible-service 스캐폴드의 DB-per-service persistence 구성을 H2(MODE=MySQL)로 활성화해 검증했다. 전용 DataSource/EMF/리포지토리가 동작하고 스키마·라운드트립이 됨을 확인 — Inc2(라우트 컷오버)의 전제 충족. 테스트 1파일 추가, main 무변경.

## 산출물

| 파일 | 설명 |
|------|------|
| `bible-service/.../BibleServicePersistenceTest.java` | (신규) persistence 활성(H2, MODE=MySQL, create-drop, flyway off) → 리포지토리 라운드트립 단언 |

## 변경 성격
- **구성 검증(테스트 전용)**: 기존 `BibleServicePersistenceConfiguration`을 켜서 전용 EMF/리포지토리 동작을 확인. main 코드·구성 무변경.
- **테스트 전략**: 운영은 MySQL + Flyway. 테스트는 MySQL 전용 DDL 구문의 H2 비호환을 피해 Flyway off + Hibernate create-drop으로 엔티티 기반 스키마 생성(ai-service 패턴). `BibleBook` protected 생성자라 JDBC 삽입 후 리포지토리 조회.
- 도메인 리포지토리(BibleBookRepository) 조회 메서드 2종(findByCode, findAllByOrderByDisplayOrderAsc) 실동작 확인 → 스캐폴드 단계의 도메인 테스트 공백을 일부 해소.

## 리뷰 권장 보강(비차단)
- **H2 DB명 무작위 suffix**: `@DynamicPropertySource`로 DB명에 UUID 부여 → 테스트 클래스 간 인메모리 DB 간섭 차단.
- **부정 경로 단언**: `findByCode("XXX") → empty` 추가.
- **internal 직접 의존 사유 명시**: 테스트 Javadoc에 bible-service가 bible 도메인 소유 모듈이라 자기 internal 접근은 경계 위반이 아니며, 이 직접 의존은 Inc1b 한정(Inc3에서 소비자 경로는 api/UseCase로 전환)임을 명시.

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (10건)**: persistence 1 + 필터 7 + 캐시 1 + contextLoads 1
- H2 전용 EMF 기동·스키마 생성·리포지토리 라운드트립(+부정 경로) 동작 확인.

## 미해결
- Inc2(라우트 분기 + 게이트웨이 토큰 주입 + 컨트롤러 슬라이스) → Inc3(소비자 어댑터) → Inc4(DB 분리) → Inc5(모놀리식 제거)
