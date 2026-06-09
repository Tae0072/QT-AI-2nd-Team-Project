# 2026-06-09 MSA Phase 1 — bible-service 스캐폴드 (Inc1)

## 목표
첫 **사용자 서비스 추출**의 1단계로, 읽기 전용 성경 참조 도메인을 독립 `bible-service`(Spring Boot 앱)로 스캐폴드한다. **트래픽 없음(skeleton)** — 모놀리식 bible 도메인과 병존하며, DB 소유권·구동 구조만 확립한다. 라우트 컷오버는 Inc2.

## 배경
- 설계: `doc/workspaces/DevD_이승욱/bible-service-추출-설계_2026-06-09.md`(증분 5단계).
- bible는 **완전한 리프 도메인** — 15파일, `com.qtai.common`(ApiResponse/예외)만 의존, 크로스 도메인·모놀리식 설정 의존 0 → 첫 추출 대상으로 적합.
- 패턴은 강상민의 `ai-service` 스캐폴드(#361~363)와 동일: `@ConditionalOnProperty`로 persistence/inbound를 **기본 비활성**으로 두고, 자동설정(DataSource/JPA/Flyway)을 exclude해 skeleton이 빈 컨텍스트로 로드되게 한다.

## 작업 내용
1. **모듈 신설** — `settings.gradle.kts`에 `:bible-service`. `build.gradle.kts`는 ai-service 미러(lib-common + web/actuator/validation/data-jpa + security-core + mysql/flyway, test: h2). Flyway는 persistence 구성이 직접 호출하므로 `implementation`, lombok(도메인 사용) 추가.
2. **도메인 코드 이전** — 모놀리식 `com.qtai.domain.bible`(api/internal/web, 15파일)을 verbatim 복사. 패키지 동일 — 별도 앱이라 충돌 없음. 모놀리식 원본은 Inc5까지 **제거하지 않음**(Strangler 병존).
3. **앱·구성** — `BibleServiceApplication`(@Import 구성), `BibleServicePersistenceConfiguration`(gated `qtai.bible.persistence.enabled`, 전용 DataSource/EMF/Flyway → DB-per-service 구조), `BibleServicePersistenceProperties`, `BibleServiceInboundConfiguration`(gated `qtai.bible.inbound.enabled`, BibleController/BibleService import).
4. **설정·마이그레이션** — `application.yml`(자동설정 exclude + 게이트 prop 기본 false, port 8082). `db/migration/V1__create_bible_owned_tables.sql`(bible_books·bible_verses, 모놀리식 V2 기준). **크로스 도메인 FK(`study.glossary_terms.bible_verse_id`)는 본 서비스에 두지 않음**.
5. **스모크 테스트** — `BibleServiceApplicationContextTest` — skeleton 기본값(비활성)으로 컨텍스트 로드 단언.

## 범위
- 브랜치: `feature/msa-bible-service-scaffold` (base: `dev`)
- 변경: `:bible-service` 신규 모듈(도메인 복사 15 + 인프라 4 + build/yml/migration/test) + settings. 모놀리식·다른 서비스·코어 무변경.
- PR 크기: 서비스 스캐폴드라 파일 수가 많음(ai-service 스캐폴드와 동일 성격) — 기능 응집 단위라 분할하지 않음.

## 리뷰 후속 보강(머지 전)
초기 스캐폴드 리뷰 3건 보강:
- **(c) 캐시 누락** — bible 도메인의 `@Cacheable("bibleBooks")`가 동작하도록 `BibleCacheConfig`(@EnableCaching + Caffeine, 모놀리식 CacheConfig의 bible 캐시 이전) 추가. 없으면 @Cacheable이 조용히 무시되어 캐싱 동작 차이. starter-cache + caffeine 의존.
- **(b) 무인증 노출 방지** — inbound 활성 시 `/api/v1/bible/**`가 무인증 노출되는 위험 차단. `GatewayHeaderAuthenticationFilter`(게이트웨이 주입 `X-Member-Id` 없으면 401, actuator 예외)를 inbound 구성과 **한 단위로 게이트**해 "켤 때 보안 누락"을 방지. full Spring Security 대신 경량 필터(자동설정 미간섭).
- **(a) 테스트 보강** — contextLoads 외 단위 테스트 추가: 필터 deny/allow/actuator 3건 + CacheManager 빈 1건.

## 검증
- `gradlew :bible-service:build` — **BUILD SUCCESSFUL / 0 failures (5건)**: contextLoads 1 + GatewayHeaderAuthenticationFilterTest 3(헤더없음 401·M0002 / 헤더있음 통과 / actuator 예외) + BibleCacheConfigTest 1(bibleBooks 캐시 등록)
- skeleton 기본값(persistence/inbound 비활성)에서 자동설정 exclude로 DataSource 없이 컨텍스트 로드 확인.
- 금지 데이터: 마이그레이션·문서의 번역본 언급은 모두 `금지`/`KJV·KRV` 문맥(requirements-guard 제외 패턴) — 본문 데이터 미포함.
- 전체 `./gradlew build`(타 모듈 포함)·통합 테스트는 CI.

## 미해결 / 후속
- **Inc1b**: persistence 활성(H2) 리포지토리 스모크 + 도메인 정책 테스트(ai-service 패턴).
- **Inc2**: 게이트웨이 `/api/v1/bible/**` → bible-service 라우트 분기 + inbound 활성 + 스모크.
- **Inc3**: 모놀리식 소비자(qt·note·study·ai)를 bible HTTP 어댑터로 전환 + 계약 테스트.
- **Inc4**: DB-per-service(시드 이전, `glossary_terms` FK 제거 — study 오너 협의).
- **Inc5**: 모놀리식 bible 도메인 제거(Strangler 완료).
- 협의: bible 오너(이지윤)·study 오너(김태혁)·Lead.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
