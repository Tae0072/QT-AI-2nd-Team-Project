# 2026-06-09 MSA Phase 1 — bible-service 라우트 컷오버 (Inc2b)

## 목표
게이트웨이가 `/api/v1/bible/**`를 bible-service로 라우팅하도록 전환한다(실제 트래픽 컷오버). 게이트웨이 우회 2차 방어선용 `X-Gateway-Token`을 주입하고, 라우트 순서·필터 회귀를 테스트로 고정한다.

## 배경
- 설계: `bible-service-Inc2-라우트컷오버-설계_2026-06-09.md`. Inc2a(inbound 슬라이스, 머지됨)로 서비스 서빙 준비를 검증했고, 본 단계가 트래픽 전환.
- 리뷰 후속: Inc2a 리뷰에서 "Inc2b에 X-Gateway-Token 불일치 회귀 테스트 추가" 요청 → 본 PR에 포함.

## 작업 내용
1. **게이트웨이 bible 라우트** (`service-gateway/application.yml`) — `id: bible-service`, `Path=/api/v1/bible/**`, `uri=${GATEWAY_BIBLE_URI:http://localhost:8082}`. **monolith catch-all(`/api/v1/**`)보다 앞 순서**(Spring Cloud Gateway 순차 평가). 필터: CircuitBreaker(`bibleCb` → `/__fallback`) + `AddRequestHeader=X-Gateway-Token, ${GATEWAY_BIBLE_SHARED_TOKEN:unset}`.
   - 토큰 기본값은 비어있지 않은 sentinel(`unset`) — AddRequestHeader는 빈 값 바인딩 불가. 서비스 측 토큰 미설정 시 무시되므로 2차 방어선은 양측 설정 시에만 활성.
2. **라우트 회귀 테스트** (`GatewayRouteTest`) — ① bible 라우트가 Path + CircuitBreaker + AddRequestHeader로 적재 ② **bible 라우트가 monolith보다 앞 순서**(순서 회귀 방지).
3. **토큰 통합 회귀 테스트** (`BibleGatewayTokenSliceTest`, bible-service) — `qtai.bible.gateway.shared-token` 설정 + inbound 활성 MockMvc: 신원 헤더 + 일치 토큰 → 200, 불일치/누락 토큰 → 401(M0002). 필터 단위 테스트와 별개로 컨트롤러까지 도달하는 통합 경로 회귀.

## 범위
- 브랜치: `feature/msa-bible-gateway-route` (base: `dev`)
- 변경: 게이트웨이 application.yml(라우트) + GatewayRouteTest(+2) + bible-service 토큰 회귀 테스트(신규). 도메인/코어 무변경.
- 관련: bible 추출 Inc2b — 실제 라우트 컷오버

## 검증
- `gradlew :service-gateway:test` — **0 failures (18건)**: GatewayRouteTest 3(monolith + bible 필터 + bible 순서) + JwtAuthenticationFilterTest 13 + contextLoads/폴백 2
- `gradlew :bible-service:test` — **0 failures (15건)**: BibleGatewayTokenSliceTest 3(신규) + 슬라이스 2 + 필터 7 + persistence 1 + 캐시 1 + contextLoads 1
- 전체 `./gradlew build`·통합 테스트는 CI.

## 미해결 / 후속
- **배포 활성화**: bible-service `QTAI_BIBLE_INBOUND_ENABLED=true`/`PERSISTENCE_ENABLED=true`/DB URL(공유 DB)/`QTAI_BIBLE_GATEWAY_SHARED_TOKEN`, 게이트웨이 `GATEWAY_BIBLE_URI`/`GATEWAY_BIBLE_SHARED_TOKEN`(서비스와 동일 값). **토큰 양측 동기화 점검 필수**(불일치 시 전량 401).
- 롤백: 게이트웨이 라우트 제거/URI 변경만으로 모놀리식 복귀(모놀리식 bible 엔드포인트 Inc5까지 병존).
- Inc3(소비자 HTTP 어댑터) → Inc4(DB 분리·시드 이전·glossary_terms FK 제거) → Inc5(모놀리식 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
