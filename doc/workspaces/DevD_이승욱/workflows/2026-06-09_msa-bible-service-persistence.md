# 2026-06-09 MSA Phase 1 — bible-service persistence 활성 검증 (Inc1b)

## 목표
bible-service 스캐폴드(Inc1, #머지됨)에서 구조만 잡아둔 **DB-per-service persistence 구성을 실제로 활성화·검증**한다. 전용 DataSource/EMF/JPA 리포지토리가 동작하고 스키마·리포지토리 라운드트립이 되는지 확인해 Inc2(라우트 컷오버)의 전제를 충족한다.

## 배경
- Inc1에서 `BibleServicePersistenceConfiguration`을 `@ConditionalOnProperty(qtai.bible.persistence.enabled)`로 기본 비활성 구성만 추가했고, 활성 경로 테스트는 Inc1b로 미뤘다(리뷰 사유 명시).
- ai-service의 `AiServicePersistenceEnabledContextTest` 패턴 동일: `@SpringBootTest(properties=...)`로 H2(MODE=MySQL) DataSource를 주입해 persistence를 켠다.

## 작업 내용
1. **persistence 활성 테스트** — `BibleServicePersistenceTest`:
   - `qtai.bible.persistence.enabled=true` + H2 `jdbc:h2:mem:...;MODE=MySQL;DATABASE_TO_LOWER=TRUE`, `ddl-auto=create-drop`, `flyway-enabled=false`.
   - 운영은 MySQL + Flyway지만, 테스트는 MySQL 전용 DDL 구문(`UNIQUE KEY name (cols)`)의 H2 비호환을 피하려 **Flyway를 끄고 Hibernate create-drop**으로 엔티티에서 스키마 생성.
   - `BibleBook`은 protected 생성자(빌더 없음)라 데이터는 JdbcTemplate로 삽입 후 `BibleBookRepository.findByCode`/`findAllByOrderByDisplayOrderAsc`로 조회 — 스키마 + 리포지토리 라운드트립 단언.

## 범위
- 브랜치: `feature/msa-bible-service-persistence` (base: `dev`, 스캐폴드 머지 후 분기)
- 변경: 테스트 1파일 추가. main 코드·구성 무변경(기존 persistence 구성을 검증만).
- 관련: bible 추출 Inc1b

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (10건)**: BibleServicePersistenceTest 1(신규) + GatewayHeaderAuthenticationFilterTest 7 + BibleCacheConfigTest 1 + contextLoads 1
- 전용 EMF가 H2로 기동하고 스키마 생성·리포지토리 라운드트립 동작 확인.
- 전체 `./gradlew build`·통합 테스트는 CI.

## 미해결 / 후속
- **Inc2**: 게이트웨이 `/api/v1/bible/**` → bible-service 라우트 분기 + inbound 활성 + 게이트웨이 `X-Gateway-Token` 주입(2차 방어선 활성) + 컨트롤러 슬라이스 테스트.
- Inc3(소비자 HTTP 어댑터) → Inc4(DB 분리·시드 이전·glossary_terms FK 제거) → Inc5(모놀리식 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
