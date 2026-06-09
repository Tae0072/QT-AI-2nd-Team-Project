# 2026-06-09 bible HTTP 클라이언트 재시도·보강(Inc3d 준비) — 결과 보고

## 요약
`mode=http` 컷오버 전 회복탄력성·커버리지 보강. 일시 오류(5xx·연결/타임아웃) 재시도를 추가하고(4xx는 재시도 안 함), 빈 입력 방어·범위/빈입력/ListAdapter/Configuration 테스트를 채웠다. CB는 프레임워크 결정 사안이라 컷오버 항목으로 분리. 기본 inprocess라 동작 무변경.

## 산출물

| 파일 | 설명 |
|------|------|
| `external/bible/BibleServiceClient.java` | 일시 오류(5xx·IO) 백오프 재시도, 4xx 즉시 역매핑(재시도 X), 소진 시 EXTERNAL_API_FAILURE |
| `external/bible/BibleClientProperties.java` | `retry-max-attempts`(기본 3)·`retry-backoff-ms`(기본 100) |
| `external/bible/BibleHttpClientConfiguration.java` | retry 설정 주입 |
| `src/main/resources/application.yml` | retry 토글 |
| `external/bible/BibleServiceClientTest.java` | +재시도 복구·소진 + 범위·빈입력(cherry-pick) |
| `external/bible/BibleHttpClientConfigurationTest.java` | (신규) mode 게이팅·@Primary 등록·토큰 fail-fast (ApplicationContextRunner) |
| `external/bible/ListBibleBooksHttpAdapterTest.java` | (cherry-pick) 위임 |

## 변경 성격
- **회복탄력성(재시도)**: 일시 오류만 제한 재시도(읽기 전용·멱등 GET이라 안전). 4xx 결정적 오류는 즉시 역매핑 전파(in-process 계약 보존).
- **빈 입력 방어**: 빈/null → 빈 결과(불필요 호출·400 회피).
- **커버리지**: 범위 조회·빈 입력·재시도·Configuration(mode 게이팅/@Primary/fail-fast)·ListAdapter 위임 단위 테스트.
- **CB 분리**: 모놀리식 servlet CB 프레임워크 도입은 인프라/Lead 결정 → 운영 진입 체크리스트 추적.
- 기본 inprocess라 동작 무변경(소비자·bible·코어 무변경).

## 리뷰 후속(WARN/INFO 반영)
- **maxAttempts 상한 가드**(WARN): `MAX_ATTEMPTS_CAP=10`으로 설정 오류(과대값) 재시도 폭주 방지.
- **4xx 무재시도 테스트**(WARN): 404는 정확히 1회 호출(재시도 없음)을 `ExpectedCount.once()` + `verify()`로 단언.
- **재시도 로그 호출 식별자**(INFO): `withRetry(operation, ...)`로 소진/재시도 로그에 엔드포인트(예: `GET /api/v1/bible/verses/by-ids`) 포함.

## 검증
- `gradlew :compileJava :test --tests "com.qtai.external.bible.*"` — **0 failures (14건)**: 클라이언트 8(+4xx 무재시도) + Configuration 3 + 어댑터 3

## 미해결
- Inc3d 컷오버(오너 협의): CB 도입 + `mode=http` 전환 + 토큰 동기화 + 소비자 계약 테스트 → Inc4(DB 분리) → Inc5(모놀리식 제거).
