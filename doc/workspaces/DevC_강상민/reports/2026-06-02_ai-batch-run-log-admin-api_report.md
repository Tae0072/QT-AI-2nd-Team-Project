# AI Batch 실행 로그 관리자 조회 API Report

## Summary

- 브랜치: `feature/ai-batch-run-log-admin-api`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-batch-run-log-admin-api.md`
- 목표: `ai_batch_run_logs`에 저장된 AI batch 실행 로그를 관리자 웹에서 목록 조회할 수 있게 한다.

## 구현 내용

- 신규 관리자 API `GET /api/v1/admin/ai/batch-run-logs`를 추가했다.
- 조회 필터는 `batchName`, `status`, `from`, `to`, `page`, `size`로 제한했다.
- `from/to`는 KST 기준 날짜(`yyyy-MM-dd`)로 받고 `createdAt` 기준으로 필터링한다.
  - `from`: 해당 날짜 00:00 inclusive
  - `to`: 해당 날짜 다음 날 00:00 exclusive
- 응답은 기존 관리자 목록 API와 같은 page envelope를 사용한다.
  - sort: `createdAt,desc,id,desc`
  - `createdAt`은 `Asia/Seoul` 기준 `OffsetDateTime`으로 반환한다.
- 권한은 `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`으로 고정했다.
  - controller에서 인증/authority를 확인한다.
  - service layer에서도 `memberRole/adminRole`을 재검증한다.
- OpenAPI와 `04_API_명세서.md`에 신규 endpoint와 응답 schema를 반영했다.

## 제외한 내용

- `GET /api/v1/admin/ai/monitoring` 운영 집계 API는 구현하지 않았다.
- 신규 DB schema/migration은 추가하지 않았다.
- scheduler, worker, batch run log 저장 로직은 변경하지 않았다.
- 신규 audit write는 추가하지 않았다.
- 원시 provider response, prompt 원문, secret/token/password 계열 값 저장 또는 복원 경로는 추가하지 않았다.

## TDD 기록

- RED:
  - `.\gradlew.bat test --tests "*AdminAiBatchRunLog*" --tests "*AiUseCaseContractTest"`
  - 신규 UseCase/DTO/controller/service/repository 타입 미존재로 compile fail 확인.
- GREEN:
  - API DTO, UseCase, query repository, query service, controller를 추가했다.
  - `AdminAiAuthentication.requireMonitoring(...)`을 추가해 `OPERATOR/REVIEWER/SUPER_ADMIN` 권한을 허용했다.
  - 날짜 검증에서 `from > to`를 LocalDate 기준으로 차단하도록 조정했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AdminAiBatchRunLog*"` | PASS |
| `.\gradlew.bat test --tests "*AiUseCaseContractTest"` | PASS |
| `.\gradlew.bat test --tests "*AiBatchRun*"` | PASS |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS, CRLF 변환 경고만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | match 없음 |
| `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | PowerShell `npx.ps1` 실행 정책 차단 |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | FAIL, 저장소 루트에 `.spectral.yaml` 없음 |
| `python` PyYAML parse for `qtai-server/apis/api-v1/openapi.yaml` | PASS, OpenAPI `3.0.3` 파싱 및 `/api/v1/admin/ai/batch-run-logs` path 존재 확인 |

## Assumptions

- 이번 PR은 실행 로그 목록 API만 다루며, 운영 집계 dashboard API(`/api/v1/admin/ai/monitoring`)는 후속 PR로 둔다.
- `ai_batch_run_logs` 테이블과 V22 migration은 이미 `dev`에 포함되어 있다.
- `errorMessage`는 저장 단계에서 redaction/truncate된 값만 반환하므로 `OPERATOR/REVIEWER/SUPER_ADMIN` 모두 조회 가능하다.
- 조회 API 자체의 audit 기록은 별도 관리자 감사 정책 PR에서 다룬다.
