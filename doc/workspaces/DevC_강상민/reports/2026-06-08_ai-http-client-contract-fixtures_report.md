# Report - 2026-06-08 ai-http-client-contract-fixtures

## 개요

- 작업명: `ai-http-client-contract-fixtures`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-http-client-contract-fixtures.md`
- 작업 브랜치: `test/ai-http-client-contract-fixtures`
- 실행 방식: 직접 실행
- 관련 F-ID: F-02, F-14, F-15
- 목적: provider endpoint 오픈 전 AI outbound HTTP client 계약을 fixture catalog로 고정

## 변경 요약

- `http-client-contract-fixtures.json`에 success/error/malformed envelope, request body, F-15 차단 사유 fixture를 추가했다.
- `AiHttpClientAdapterContractTest`가 fixture를 읽어 MockWebServer 응답과 request body 기대값을 구성하도록 변경했다.
- Today QT `STALE_FALLBACK`, `EMPTY`, provider `error.fields`, F-15 `blockedReason`/`blocked_reason` 검증을 추가했다.

## 제외 범위

provider Controller, 실제 endpoint 연결, production code, OpenAPI, DB/migration, service-token/JWKS/gateway, Pact/Spring Cloud Contract는 변경하지 않았다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest` | FAIL: 멀티프로젝트에서 `lib-common:test`에도 필터가 적용되어 해당 테스트 없음 |
| `.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest --rerun-tasks` | PASS |
| `git diff --check` | PASS |
| fixture 금지 번역본/출처 명칭 검색 | PASS |
| workflow/report 미완료 표시 문구 검색 | PASS |

## REQUEST_CHANGES 대응

- `http-client-contract-fixtures.json`이 PR diff에 포함된 상태를 재확인했다.
- F-15 `blockedReason`/`blocked_reason` fixture는 단순 필드 존재 확인이 아니라 camel/snake 값을 동일 정책 모델로 정규화하는 행위 단언으로 보강했다.
- `statusResponse`는 provider가 `ApiResponse` envelope 생성 전에 실패하는 경우의 HTTP status fallback mapping 검증용이므로 유지하고, 테스트 코드에 보존 사유를 명시했다.
- `Idempotency-Key` 단일 필드 단언은 유지했고, audit 요청 body는 fixture 전체 비교와 별도로 `actorType=SYSTEM_BATCH` 단언을 추가했다.
- Today QT `MISS` 케이스와 `loadFixtures` 정적 캐싱은 리뷰 참고 항목으로 분류하여 후속 작업으로 남겼다.

## PR diff fixture 포함 재확인

- PR #356 changed files 기준 `qtai-server/src/test/resources/contracts/ai-provider/http-client-contract-fixtures.json`이 포함되어 있다.
- 원격 브랜치 기준 `git diff --name-status origin/dev...origin/test/ai-http-client-contract-fixtures` 결과에서 해당 파일은 `A` 상태로 확인된다.
- GitHub PR file patch 기준 해당 파일은 `@@ -0,0 +1,220 @@` 신규 파일로 확인된다.
- 최신 보완 커밋 diff만 보면 fixture JSON이 보이지 않을 수 있으나, PR 전체 Files changed에는 이전 커밋에서 추가된 fixture JSON이 포함되어 있다.

## 후속 작업

- provider endpoint가 열리면 이번 fixture catalog 기준으로 smoke test를 별도 PR에서 작성한다.
- Today QT `MISS` fixture/test와 `loadFixtures` 정적 캐싱은 별도 테스트 정리 PR에서 처리한다.
