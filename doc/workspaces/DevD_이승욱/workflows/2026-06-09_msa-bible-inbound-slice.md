# 2026-06-09 MSA Phase 1 — bible-service inbound 슬라이스 검증 (Inc2a)

## 목표
라우트 컷오버(Inc2b) 전에 **bible-service가 받을 준비가 됐는지** 검증한다. `inbound` 활성 시 `BibleController`가 인증 필터(`GatewayHeaderAuthenticationFilter`) 뒤에서 정상 서빙하는지 슬라이스 테스트로 실증. 이 단계 게이트웨이는 여전히 모놀리식으로 라우팅(트래픽 없음).

## 배경
- 설계: `bible-service-Inc2-라우트컷오버-설계_2026-06-09.md`(컷오버 순서 — 서비스 준비 → 게이트웨이 전환).
- 안전한 Strangler: 서비스가 서빙 가능함을 먼저 검증한 뒤 Inc2b에서 트래픽을 돌린다. 순서를 지켜야 bible 엔드포인트 공백이 없다.

## 작업 내용
1. **inbound 슬라이스 테스트** — `BibleInboundSliceTest`:
   - `qtai.bible.inbound.enabled=true` + persistence 활성(H2, MODE=MySQL, create-drop, flyway off, DB명 무작위)으로 `BibleController`·`BibleService`·필터를 함께 기동(`@AutoConfigureMockMvc`).
   - `GET /api/v1/bible/books` + `X-Member-Id`/`X-Member-Role` → **200** + 표준 envelope + data 1건(JDBC 삽입한 책).
   - 헤더 없음 → **401** + M0002 (deny-by-default 필터 실증).
2. 코드 변경 없음 — 스캐폴드(Inc1)에 이미 구현된 controller/filter/service를 활성화해 검증만.

## 범위
- 브랜치: `feature/msa-bible-inbound-slice` (base: `dev`)
- 변경: 테스트 1파일 + 설계/workflow/report 문서. main 코드·구성 무변경.
- 관련: bible 추출 Inc2a (Inc2b 게이트웨이 라우트 전환의 전제)

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (12건)**: BibleInboundSliceTest 2(신규) + GatewayHeaderAuthenticationFilterTest 7 + BibleServicePersistenceTest 1 + BibleCacheConfigTest 1 + contextLoads 1
- 컨트롤러 슬라이스로 BibleController+BibleService+필터 통합 동작 확인 → 스캐폴드 단계 도메인 테스트 공백 추가 해소.

## 미해결 / 후속
- **Inc2b**: 게이트웨이 `/api/v1/bible/**` → bible-service 라우트(catch-all 이전 우선순위) + `X-Gateway-Token` 주입 + CircuitBreaker 폴백 + 라우트 순서 회귀 테스트. **실제 트래픽 컷오버**.
- Inc3(소비자 HTTP 어댑터) → Inc4(DB 분리·시드 이전·glossary_terms FK 제거) → Inc5(모놀리식 제거).
- 배포: Inc2b 전 bible-service 활성 설정(`QTAI_BIBLE_INBOUND_ENABLED`/`PERSISTENCE_ENABLED`/DB URL=공유 DB/`GATEWAY_SHARED_TOKEN`) 필요.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
