# 2026-06-09 MSA Phase 1 — bible HTTP 클라이언트 재시도·보강 (Inc3d 준비)

## 목표
`mode=http` 컷오버(Inc3d) 전, bible HTTP 클라이언트의 회복탄력성과 커버리지를 보강한다. 리뷰 권고(빈 입력 방어·범위/Configuration/ListAdapter 테스트·재시도)를 반영하고, Circuit Breaker는 프레임워크 결정 사안이라 설계로 분리한다.

## 배경
- Inc3c-2(external/bible HTTP 클라이언트, #400) 머지 후 빠진 보강 커밋을 cherry-pick + 재시도 추가.
- 리뷰: 빈 입력 방어·커버리지·재시도/CB를 컷오버 전 보강 권고(비차단).

## 작업 내용
1. **빈 입력 방어**(cherry-pick) — `getVerses(빈/null)`은 네트워크 호출 없이 빈 결과(in-process 계약).
2. **재시도(retry)** — `BibleServiceClient`에 일시 오류 재시도: **5xx·연결/타임아웃만** 짧은 백오프로 제한 횟수 재시도, 소진 시 `EXTERNAL_API_FAILURE`. **4xx는 결정적이라 재시도 안 함**(error 코드 역매핑 즉시 전파, in-process 예외 계약 보존). `qtai.bible.client.retry-max-attempts`(기본 3)·`retry-backoff-ms`(기본 100) 설정.
3. **커버리지 보강** — 범위 조회 쿼리·언랩, 빈 입력, `ListBibleBooksHttpAdapter` 위임 테스트(cherry-pick) + **재시도(복구/소진)** + **`BibleHttpClientConfiguration`**(ApplicationContextRunner — mode 게이팅·`@Primary` 어댑터 등록·토큰 누락 fail-fast).
4. (분리) **Circuit Breaker** — 모놀리식(servlet)은 게이트웨이(reactive Resilience4j)와 달리 CB 미도입. 프레임워크 도입은 인프라/Lead 결정 → 운영 진입 체크리스트에 컷오버 전 항목으로 추적.

## 범위
- 브랜치: `feature/msa-bible-http-client-resilience` (base: `dev`)
- 변경: `BibleServiceClient`(retry) + properties/config(retry 설정) + application.yml + 테스트(재시도 2 + Configuration 3) + 보강 cherry-pick. 소비자·bible·코어 무변경. 기본 inprocess라 동작 무변경.
- 관련: bible 추출 Inc3d 준비(컷오버 전 회복탄력성)

## 검증
- `gradlew :compileJava :test --tests "com.qtai.external.bible.*"` — **BUILD SUCCESSFUL / 0 failures (13건)**
  - `BibleServiceClientTest` 7(토큰·books·범위·빈입력·오류 역매핑 + **503 재시도 복구·소진**)
  - `BibleHttpClientConfigurationTest` 3(http=어댑터 등록 / inprocess=미등록 / 토큰 누락 fail-fast)
  - `GetBibleVerseHttpAdapterTest` 2 + `ListBibleBooksHttpAdapterTest` 1
- 기본 inprocess라 회귀 영향 없음(전체는 CI).

## 미해결 / 후속
- **Inc3d 컷오버**(오너 협의): ① Circuit Breaker 도입(프레임워크 결정) ② `mode=http` 전환 + 양측 토큰 동기화 ③ 소비자(qt→note→study) 계약/통합 테스트.
- Inc4(DB 분리) → Inc5(모놀리식 bible 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
