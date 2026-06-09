# 2026-06-09 MSA Phase 1 — bible HTTP 클라이언트 Circuit Breaker (Inc3d)

## 목표
Lead/인프라 승인(2026-06-09) 후, bible HTTP 클라이언트에 Circuit Breaker를 도입해 다운스트림 bible-service 지속 장애 시 fast-fail로 격리한다. 게이트웨이와 동일한 Resilience4j 스택.

## 배경
- 승인: CB 의존성(resilience4j) 도입 승인됨. 모놀리식에 `io.github.resilience4j:resilience4j-circuitbreaker:2.2.0` 추가(spring-cloud BOM 불요, 프로그래매틱).
- 재시도(#403)는 일시 blip을, CB는 지속 장애(fast-fail)를 보완 — 2단 회복탄력성.

## 작업 내용
1. **의존성** — `resilience4j-circuitbreaker:2.2.0`(게이트웨이 Resilience4j와 정합).
2. **CircuitBreaker 빈**(`BibleHttpClientConfiguration`, mode=http 게이트) — COUNT_BASED 슬라이딩 윈도우, 설정값(failure-rate·window·min-calls·wait-duration, 기본 50%/10/5/10s). **실패 기록은 일시 장애(`EXTERNAL_API_FAILURE`)만 — 4xx 역매핑은 무시**(`recordException` 술어). 상태 전이(CLOSED↔OPEN↔HALF_OPEN) INFO 로그.
3. **클라이언트 적용** — `BibleServiceClient`가 `executeWithResilience`에서 CB로 재시도를 감싼다. CB OPEN 시 다운스트림 호출 없이 `CallNotPermittedException`→`BusinessException(EXTERNAL_API_FAILURE)` fast-fail. CB 미설정(테스트 등)이면 재시도만.
4. **설정** — `qtai.bible.client.cb-*` (application.yml).

## 범위
- 브랜치: `feature/msa-bible-circuit-breaker` (base: `dev`)
- 변경: build(의존성) + `BibleServiceClient`(CB 래핑) + config(CB 빈) + properties(CB 설정) + application.yml + 테스트(CB 2). 기본 inprocess라 동작 무변경.
- 관련: bible 추출 Inc3d(컷오버 회복탄력성)

## 검증
- `gradlew :compileJava :test --tests "com.qtai.external.bible.*"` — **BUILD SUCCESSFUL / 0 failures (16건)**
  - `BibleServiceClientTest` 10(+CB 2: 지속 5xx→OPEN·단락 / 4xx→CLOSED 유지) + Configuration 3 + 어댑터 3
- 2층 셀프 점검: CI 게이트(브랜치/커밋/gitleaks/금지) + 리뷰 9기준(#2 예외 누출 없음·#3 CB 정상/4xx-무관 부정경로·관측성 로그) PASS.

## 미해결 / 후속
- **Inc3d 컷오버**(오너 협의 — 승인됨): `mode=http` 전환 + 양측 토큰 동기화 + 소비자(qt/note/study) 계약 테스트.
- Inc4(DB 분리, study FK 제거 — 승인됨) → Inc5(모놀리식 bible 제거). API 명세(04) 동기화.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
