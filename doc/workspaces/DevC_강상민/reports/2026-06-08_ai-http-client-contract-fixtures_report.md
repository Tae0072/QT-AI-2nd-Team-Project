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

## 후속 작업

- provider endpoint가 열리면 이번 fixture catalog 기준으로 smoke test를 별도 PR에서 작성한다.
