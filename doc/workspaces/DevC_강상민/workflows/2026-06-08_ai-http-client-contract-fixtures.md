# Workflow - 2026-06-08 ai-http-client-contract-fixtures

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-http-client-contract-fixtures` |
| 기준/작업 브랜치 | `dev` → `test/ai-http-client-contract-fixtures` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | provider endpoint 오픈 전 AI HTTP client 요청/응답 계약을 fixture로 고정해야 함 |
| 기준 문서 | readiness checklist, HTTP adapter foundation, `qtai-server/apis/ai-service/openapi.yaml` |
| 대상 경로 | `qtai-server/src/test/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`AiHttpClientAdapterContractTest`가 inline JSON 대신 공유 fixture catalog를 사용하게 만든다. provider endpoint가 늦어도 동일 JSON 기준으로 success/error envelope, request body, 실패 모델을 검증할 수 있어야 한다.

## 범위

- `src/test/resources/contracts/ai-provider/http-client-contract-fixtures.json` 생성
- `AiHttpClientAdapterContractTest`를 fixture 기반으로 전환
- Today QT `STALE_FALLBACK`/`EMPTY`, provider `error.fields`, malformed envelope, F-15 `blockedReason`/`blocked_reason` 검증 추가
- workflow/report 작성

## 제외 범위

- provider Controller, 실제 endpoint 연결, production code, OpenAPI, DB/migration 변경
- service-token/JWKS/gateway/Docker/Kubernetes 설정
- Pact 또는 Spring Cloud Contract 도입

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-http-client-contract-fixtures.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-http-client-contract-fixtures_report.md` | 결과 기록 |
| Create | `qtai-server/src/test/resources/contracts/ai-provider/http-client-contract-fixtures.json` | fixture catalog |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/client/http/AiHttpClientAdapterContractTest.java` | fixture 기반 계약 테스트 |

## 구현 순서

1. `dev` 최신화 후 작업 브랜치를 생성한다.
2. readiness checklist와 기존 HTTP adapter 계약 테스트를 확인한다.
3. fixture catalog를 만들고 테스트 helper가 classpath에서 읽게 한다.
4. success/error/malformed envelope와 request body 비교를 fixture 기반으로 바꾼다.
5. `STALE_FALLBACK`, `EMPTY`, F-15 차단 사유 fixture 검증을 추가한다.
6. report 작성 후 테스트와 문서/fixture 검증을 실행하고 커밋한다.

## 수용 기준

- [ ] fixture catalog가 생성되고 기존 path/query/header/idempotency 검증이 유지된다.
- [ ] Bible fixture에 실제 본문과 금지 번역본 명칭이 없다.
- [ ] provider Controller, production code, OpenAPI, DB 변경이 없다.
- [ ] 대상 테스트와 `git diff --check`가 통과한다.

## Subagent Decision

Subagent 사용은 권장하지 않는다. fixture catalog와 HTTP adapter 계약 테스트가 강하게 연결되어 직접 실행이 더 안전하며, 병렬 위임 대상은 없다. 메인 에이전트가 전체 작업과 최종 검증을 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest --rerun-tasks
cd ..
git diff --check
```

생성 문서의 미완료 표시 문구와 fixture JSON의 금지 번역본/출처 명칭도 검색한다.
