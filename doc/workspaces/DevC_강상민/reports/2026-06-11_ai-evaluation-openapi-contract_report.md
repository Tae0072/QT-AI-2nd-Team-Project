# Report — 2026-06-11 ai-evaluation-openapi-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-evaluation-openapi-contract` |
| PR 대상 | `dev` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-evaluation-openapi-contract.md` |
| 관련 F-ID | F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-evaluation-openapi-contract_report.md` |

## 작업 결과

PR #499로 `dev`에 병합된 AI 평가 셋/케이스 관리자 API를 `qtai-server/apis/api-v1/openapi.yaml`에 반영했다. backend 구현 코드는 변경하지 않았고, controller/DTO 계약 기준으로 path, request schema, response schema, enum, 공통 error response를 추가했다.

## 변경 요약

1. AI 평가 셋 목록/생성/상세/활성화/은퇴 API 명세를 추가했다.
2. AI 평가 케이스 목록/생성/상세/approve/reject API 명세를 추가했다.
3. asset 기반 evaluation candidate 생성 API 명세를 추가했다.
4. 평가 셋/케이스 request, response, list response, status response schema를 추가했다.
5. `AiEvaluationType`, `AiEvaluationSetStatus`, `AiEvaluationCaseStatus`, `AiEvaluationSourceType` enum schema를 추가했다.
6. audit log 조회 enum에 `EVAL_CASE_APPROVE`, `EVAL_CASE_REJECT`, `AI_EVALUATION_CASE`를 추가했다.

## 정합성 반영

| 항목 | 반영 |
| --- | --- |
| 요청 JSON 필드 | `expectedPolicyJson`, `inputJson`, `expectedOutputJson`은 object schema로 명시 |
| 응답 JSON 필드 | 실제 Java DTO가 `String`이므로 `expectedPolicyJson`, `inputJson`, `expectedOutputJson`은 JSON 문자열로 명시 |
| 권한 | 생성/조회/셋 상태 변경/asset candidate는 `CONTENT_CREATOR/REVIEWER/SUPER_ADMIN`, approve/reject는 `REVIEWER/SUPER_ADMIN`으로 설명 |
| 민감정보 | asset candidate 명세에 prompt/provider raw response, validation reference text, secret, token, password 미복사 정책 명시 |
| 오류 응답 | 기존 공통 `BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `InternalServerError` 재사용 |

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/apis/api-v1/openapi.yaml` | AI 평가 관리자 API path/schema 추가 |
| `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-evaluation-openapi-contract.md` | 작업 workflow 작성 |
| `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-evaluation-openapi-contract_report.md` | 작업 report 작성 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| YAML/OpenAPI 필수 path/schema 파싱 검증 | 성공, 9개 path와 18개 schema 존재 확인 |
| OpenAPI 내부 `$ref` 해소 검증 | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiEvaluation*"` | 성공 |

## 생략한 검증

| 명령 | 사유 |
| --- | --- |
| `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | 저장소 루트와 `qtai-server` 아래에 `.spectral.yaml`이 없어 실행하지 않음. 대신 YAML 파싱, 필수 path/schema 존재, `$ref` 해소 검증을 수행함 |

## 후속 작업

- PR 본문에 F-ID, workflow/report 경로, 추가된 endpoint 요약을 명시한다.
- 저장소 표준 `.spectral.yaml` 위치가 확정되면 OpenAPI lint 검증을 복구한다.
