# Workflow — 2026-06-11 ai-evaluation-openapi-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-evaluation-openapi-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14 |
| 트리거 | PR #499로 병합된 AI 평가 셋/케이스 관리자 API의 OpenAPI 명세 반영 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md`, `2026-06-11_ai-evaluation-cases.md` |
| 해당 경로 | `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

PR #499에서 구현된 관리자 AI 평가 셋/케이스 API를 `qtai-server/apis/api-v1/openapi.yaml`에 반영한다. 구현 코드는 변경하지 않고, 현재 controller/DTO 계약과 일치하는 path, request body, response schema, error response를 문서화한다.

## 범위

- `GET/POST /api/v1/admin/ai/evaluation-sets` 명세 추가
- `GET /api/v1/admin/ai/evaluation-sets/{setId}` 명세 추가
- `POST /api/v1/admin/ai/evaluation-sets/{setId}/activate` 명세 추가
- `POST /api/v1/admin/ai/evaluation-sets/{setId}/retire` 명세 추가
- `GET/POST /api/v1/admin/ai/evaluation-sets/{setId}/cases` 명세 추가
- `GET /api/v1/admin/ai/evaluation-cases/{caseId}` 명세 추가
- `POST /api/v1/admin/ai/evaluation-cases/{caseId}/approve` 명세 추가
- `POST /api/v1/admin/ai/evaluation-cases/{caseId}/reject` 명세 추가
- `POST /api/v1/admin/ai/assets/{assetId}/evaluation-candidates` 명세 추가
- 평가 셋/케이스 request, response, list response, status response, enum schema 추가
- 완료 report 작성

## 제외 범위

- backend controller/service/test 구현 변경
- 평가 셋/케이스 DB 마이그레이션 변경
- AI 평가 서비스 책임 분리, 상태 정책 변경, audit 정책 변경
- 관리자 웹 UI 변경
- PR 생성과 push

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 관리자 AI 평가 API path와 schema 추가 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-evaluation-openapi-contract_report.md` | 변경 내용, 검증 결과, 후속 작업 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `docs/ai-evaluation-openapi-contract` 브랜치를 생성한다.
2. `AdminAiEvaluationController`와 `domain.ai.api.admin.evaluation.dto` record를 기준으로 실제 endpoint와 필드를 확인한다.
3. `openapi.yaml`의 기존 `Admin AI` path 주변에 evaluation path를 추가한다.
4. components schemas에 평가 셋/케이스 request, response, list response, status response, enum을 추가한다.
5. 요청 JSON 필드는 object schema로, 응답 JSON 필드는 Java DTO와 맞춰 string schema로 작성한다.
6. error response는 기존 공통 `BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `InternalServerError`를 재사용한다.
7. OpenAPI 문서 검증과 관련 controller test를 실행한다.
8. report를 작성하고 Conventional Commits 형식으로 커밋한다.

## 테스트/검증 보강 목록

| 검증 | 내용 |
| --- | --- |
| `git diff --check` | OpenAPI/report 문서 공백 오류 확인 |
| `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | Spectral 규칙 검증. ruleset이 없으면 실행 불가 사유 기록 |
| YAML/OpenAPI 파싱 | Spectral 실행이 불가하면 OpenAPI YAML 구조 파싱으로 대체 검증 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"` | 문서 반영 대상 controller 회귀 확인 |

## 수용 기준

- [x] AI 평가 셋/케이스 관리자 API endpoint가 OpenAPI에 명시된다.
- [x] 요청/응답 schema가 실제 controller/DTO 계약과 일치한다.
- [x] 응답 JSON 필드는 문자열로 명시되어 실제 Java DTO와 불일치하지 않는다.
- [x] 권한 설명이 생성/조회/상태 변경과 approve/reject 차이를 반영한다.
- [x] workflow/report가 작성된다.
- [x] 지정 검증 결과가 report에 기록된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `openapi.yaml` 단일 계약 파일과 문서 report에 집중되어 있다.
- path와 schema 명세는 서로 참조 관계가 있어 한 작업자가 일관되게 편집하는 편이 안전하다.
- backend 구현 변경이 없고 테스트 병렬화 이점이 낮다.

### 위임 가능 작업

| Worker | 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 직접 실행 | `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/**` |

### 직접 실행 판단

메인 에이전트가 직접 실행한다.

## 검증 계획

```powershell
git diff --check
npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"
```

`.spectral.yaml`이 저장소에 없으면 Spectral은 생략하고, YAML/OpenAPI 파싱 검증 결과와 생략 사유를 report에 기록한다.

## 실제 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| YAML/OpenAPI 필수 path/schema 파싱 검증 | 성공 |
| OpenAPI 내부 `$ref` 해소 검증 | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiEvaluation*"` | 성공 |
| `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | `.spectral.yaml` 부재로 생략 |

## 후속 작업으로 남길 항목

- PR 생성 시 F-ID, workflow/report 경로, OpenAPI 변경 요약을 PR 본문에 명시
- 필요 시 `apis/ai-service/openapi.yaml`과의 역할 경계 점검
