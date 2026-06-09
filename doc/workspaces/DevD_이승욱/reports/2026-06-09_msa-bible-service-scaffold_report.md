# 2026-06-09 bible-service 스캐폴드(Inc1) — 결과 보고

## 요약
MSA Phase 1 첫 단계로 읽기 전용 성경 도메인을 독립 `bible-service`로 스캐폴드했다. **트래픽 없는 skeleton** — persistence/inbound를 `@ConditionalOnProperty`로 기본 비활성해 모놀리식과 병존한다. ai-service 스캐폴드 패턴 동일. DB 소유권·구동 구조만 확립하고 라우트 컷오버는 Inc2.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/settings.gradle.kts` | `:bible-service` 모듈 등록 |
| `bible-service/build.gradle.kts` | lib-common + web/jpa/validation/security/actuator + mysql/flyway, test h2, lombok |
| `bible-service/.../bible/BibleServiceApplication.java` | (신규) 진입점 + 구성 Import |
| `bible-service/.../bible/BibleServicePersistenceConfiguration.java` | (신규) gated 전용 DataSource/EMF/Flyway(DB-per-service 구조) |
| `bible-service/.../bible/BibleServicePersistenceProperties.java` | (신규) `qtai.bible.persistence.*` |
| `bible-service/.../bible/BibleServiceInboundConfiguration.java` | (신규) gated BibleController/Service import |
| `bible-service/.../domain/bible/**` | 모놀리식 bible 도메인 15파일 복사(api/internal/web) |
| `bible-service/src/main/resources/application.yml` | 자동설정 exclude + 게이트 prop 기본 false |
| `bible-service/.../db/migration/V1__create_bible_owned_tables.sql` | bible_books·bible_verses(크로스 FK 제외) |
| `bible-service/.../BibleServiceApplicationContextTest.java` | (신규) skeleton contextLoads 스모크 |

## 변경 성격
- **독립 서비스 스캐폴드(트래픽 오프)**: 도메인 코드를 복사해 standalone 구동 구조를 확립하되, 게이트는 비활성이라 실트래픽·DB 영향 없음. 모놀리식이 계속 bible를 서빙(Strangler 병존).
- **DB 소유권 구조**: 전용 DataSource/EMF/Flyway를 prop 게이트 뒤에 구성 → DB 분리(Inc4) 시 활성화. 마이그레이션에서 **크로스 도메인 FK(`glossary_terms`)를 제외**해 경계를 명확히.
- **리프 검증**: bible는 `com.qtai.common`만 의존해 복사 시 lib-common만으로 컴파일 → 첫 추출 도메인으로 적합함을 확인.
- 모놀리식·코어·다른 서비스 무변경.

## 리뷰 후속 보강(머지 전)

| 지적 | 조치 |
|------|------|
| (c) `@Cacheable` 복사 시 CacheManager 누락 | `BibleCacheConfig`(@EnableCaching + Caffeine `bibleBooks`) 추가 — 없으면 @Cacheable 무시되어 캐싱 동작 차이. starter-cache+caffeine 의존 |
| (b) inbound 활성 시 무인증 노출 | `GatewayHeaderAuthenticationFilter`를 inbound와 한 단위로 게이트 — deny-by-default |
| (b-2) X-Member-Role 미수신 + 우회 2차 방어선 부재 | **2단 방어선**: ① `X-Member-Id`·`X-Member-Role` 2종 모두 필수 ② `qtai.bible.gateway.shared-token` 설정 시 `X-Gateway-Token` 상수시간 검증(우회 차단, env 주입). 게이트웨이 토큰 주입은 Inc2 전제 |
| (a) 단위/슬라이스 테스트 0건(도메인 KPI) | **사유 명시**: 도메인은 verbatim 복사 + skeleton(트래픽 오프)이라 활성화하는 Inc1b/Inc2에서 통합 테스트 일괄 추가. 본 PR은 신규 인프라(필터·캐시)에 단위 테스트 |

## 검증
- `gradlew :bible-service:build` — **BUILD SUCCESSFUL / 0 failures (9건)**: contextLoads 1 + 필터 7(id/role 필수·토큰 2차 방어·actuator) + 캐시 1
- 자동설정 exclude로 skeleton(DataSource 없음)에서 컨텍스트 로드 확인.
- 금지 데이터: 본문 미포함. 번역본 언급은 `금지`/`KJV·KRV` 문맥(requirements-guard 제외).
- 전체 빌드·통합 테스트는 CI.

## 미해결
- Inc1b(persistence 활성 H2 테스트) → Inc2(라우트 분기) → Inc3(소비자 HTTP 어댑터) → Inc4(DB 분리) → Inc5(모놀리식 제거)
- bible 오너(이지윤)·study 오너(김태혁)·Lead 협의
