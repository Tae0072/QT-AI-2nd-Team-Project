# Workflow - 2026-06-08 ai-provider-endpoint-readiness

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-provider-endpoint-readiness` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `docs/ai-provider-endpoint-readiness` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | provider endpoint가 아직 열리지 않은 상태에서, 담당자가 구현 전에 AI outbound system endpoint 계약을 바로 확인할 수 있는 readiness checklist가 필요함 |
| 기준 문서 | `2026-06-08_ai-system-endpoint-contract-sync.md`, `2026-06-08_ai-http-client-adapter-foundation.md`, `2026-06-08_ai-msa-schedule.md`, `qtai-server/apis/ai-service/openapi.yaml` |
| 대상 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

상대가 확정한 `/api/v1/system/**` provider endpoint 계약을 변경하지 않고, provider 구현자가 요청/응답/header/실패 케이스를 빠뜨리지 않도록 readiness checklist로 정리한다. 이번 작업은 문서 전용이며 실제 provider Controller, HTTP adapter 코드, OpenAPI, 테스트 코드는 변경하지 않는다.

## 범위

- 공통 system endpoint 규약을 checklist에 정리한다.
- QT, Today QT status, Bible, Study, Audit, Admin/Auth endpoint별 구현 확인 항목을 표로 정리한다.
- AI HTTP adapter가 실제로 보내는 method/path/query/body/header 기준을 명시한다.
- 후속 contract fixture에 포함해야 할 `STALE_FALLBACK`, `EMPTY`, `error.fields`, F-15 `blockedReason`/`blocked_reason`, malformed envelope 케이스를 명시한다.
- 작업 결과와 검증 범위를 report 문서에 기록한다.

## 제외 범위

- provider service Controller 구현
- AI HTTP adapter 구현 또는 수정
- `qtai-server/apis/ai-service/openapi.yaml` 변경
- 테스트 코드 추가 또는 수정
- DB schema, migration, seed 변경
- service-token 발급, JWKS, mTLS, gateway, Docker, Kubernetes 설정
- Pact 또는 Spring Cloud Contract 도입

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-provider-endpoint-readiness.md` | 작업 범위와 실행 기준 기록 |
| Create | `doc/workspaces/DevC_강상민/2026-06-08_ai-provider-endpoint-readiness-checklist.md` | provider 구현자가 확인할 endpoint readiness checklist |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-provider-endpoint-readiness_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `docs/ai-provider-endpoint-readiness` 브랜치에서 작업한다.
2. `qtai-server/apis/ai-service/openapi.yaml`의 `x-ai-outbound-system-endpoints` 계약을 확인한다.
3. HTTP adapter 계약 테스트의 method/path/query/body/header 기준을 확인한다.
4. workflow 문서를 저장한다.
5. provider endpoint readiness checklist 문서를 작성한다.
6. report 문서를 작성한다.
7. 문서 경로, 파일명, placeholder, trailing whitespace를 검증한다.
8. 변경 파일 3개만 stage하고 `docs(ai): provider endpoint readiness checklist 작성`으로 커밋한다.

## 문서 검증 목록

| 문서 | 검증 |
| --- | --- |
| `2026-06-08_ai-provider-endpoint-readiness-checklist.md` | endpoint 6종의 method/path/query/body/header가 OpenAPI outbound 계약과 일치 |
| `2026-06-08_ai-provider-endpoint-readiness-checklist.md` | 쓰기 endpoint의 `Idempotency-Key`, 공통 `Authorization`, `traceparent`, envelope 규약 명시 |
| `2026-06-08_ai-provider-endpoint-readiness-checklist.md` | `STALE_FALLBACK`, `EMPTY`, `error.fields`, F-15 차단 사유, malformed envelope 케이스 명시 |
| `2026-06-08_ai-provider-endpoint-readiness_report.md` | 문서 전용 변경, 제외 범위, 검증 결과 기록 |

## 수용 기준

- [ ] workflow, checklist, report 문서 3개가 생성된다.
- [ ] checklist가 `x-ai-outbound-system-endpoints`의 provider endpoint 계약을 변경 없이 풀어 쓴다.
- [ ] provider endpoint 구현자가 필요한 request/response/header 기준을 한 문서에서 확인할 수 있다.
- [ ] provider endpoint 구현, HTTP adapter 코드, OpenAPI, DB 변경이 없다.
- [ ] 문서에 placeholder와 trailing whitespace가 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- workflow, checklist, report가 같은 endpoint 계약을 공유하므로 한 흐름에서 정합성을 맞추는 편이 안전하다.
- 변경 범위가 문서 3개에 집중되어 병렬화 이점이 작다.
- provider 계약을 변경하지 않는다는 제외 범위를 일관되게 지켜야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, checklist 작성, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
git status --short --branch
git diff --name-only
git diff --check
```

추가로 생성 문서 3개에서 미완료 표시 문구가 남아 있지 않은지 검색한다.

## 후속 작업으로 남길 항목

- checklist 기준으로 request/response fixture를 별도 PR에서 정리한다.
- provider endpoint가 실제로 열리면 `mode=http` 기반 provider smoke test를 별도 PR에서 진행한다.
- Pact 또는 Spring Cloud Contract 도입은 provider endpoint 개설 단계에서 별도 검토한다.
