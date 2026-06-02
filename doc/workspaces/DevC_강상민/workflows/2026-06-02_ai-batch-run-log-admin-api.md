# AI Batch 실행 로그 관리자 조회 API Workflow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-batch-run-log-admin-api` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-06, F-14 |
| 저장 대상 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-batch-run-log-admin-api.md` |
| 리포트 대상 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-batch-run-log-admin-api_report.md` |
| 트리거 | AI batch run log DB 저장 후속: 관리자 웹 조회 API |

## 작업 목표

`ai_batch_run_logs`에 저장된 AI batch 실행 로그를 관리자 웹에서 목록 조회할 수 있게 한다. 신규 API는 원시 실행 로그 목록 전용으로 두고, 기존 SSoT의 `GET /api/v1/admin/ai/monitoring` 운영 집계 API는 후속 PR로 분리한다.

## 공개 인터페이스

- 신규 HTTP API: `GET /api/v1/admin/ai/batch-run-logs`
- Query params: `batchName`, `status`, `from`, `to`, `page`, `size`
- 응답은 기존 관리자 목록 API와 같은 page envelope를 사용한다.
- 권한은 `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`으로 고정한다.
- OpenAPI와 `04_API_명세서.md`에 신규 목록 API를 반영한다.
- 신규 DB schema, migration, batch 동작 변경은 없다.

## 범위

- `batchName`은 `AI_DAILY_QT_VERSE_EXPLANATION_SEED`, `AI_GENERATION_WORKER_POLL`만 허용한다.
- `status`는 `SUCCEEDED`, `PARTIAL_FAILED`, `FAILED`만 허용한다.
- `from/to`는 KST 기준 `yyyy-MM-dd`로 받고 `createdAt` 기준 기간 필터로 적용한다.
- `from`은 inclusive, `to`는 해당 날짜 다음 날 00:00 exclusive로 적용한다.
- `page` 기본값은 `0`, `size` 기본값은 `20`, 최대 `100`이다.
- sort는 `createdAt,desc,id,desc`로 고정한다.
- `createdAt`은 `Asia/Seoul` 기준 `OffsetDateTime`으로 변환해 응답한다.
- `errorMessage`는 저장 단계에서 redaction/truncate된 값을 그대로 반환한다.

## 제외 범위

- `GET /api/v1/admin/ai/monitoring` 집계 API 구현 없음.
- 신규 batch run log 저장 로직, scheduler, worker 변경 없음.
- 신규 DB migration 없음.
- 신규 audit write 없음.
- 원시 provider response, prompt 원문, secret/token/password 계열 값 저장 또는 복원 없음.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/ListAdminAiBatchRunLogsUseCase.java` | 관리자 batch run log 목록 조회 UseCase |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/*AdminAiBatchRunLog*.java` | query, response, item DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiBatchRunLogQueryService.java` | 권한/입력 검증과 page 응답 조립 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiBatchRunLogQueryRepository.java` | `ai_batch_run_logs` 필터/정렬/page 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAuthentication.java` | OPERATOR/REVIEWER/SUPER_ADMIN 조회 권한 resolver 추가 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiBatchRunLogController.java` | `/api/v1/admin/ai/batch-run-logs` controller |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 신규 API path/schema 반영 |
| Modify | `doc/프로젝트 문서/04_API_명세서.md` | 신규 batch run log 목록 API 명시 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiBatchRunLogControllerTest.java` | 경로, 권한, query mapping |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiBatchRunLogQueryServiceTest.java` | 권한과 입력 검증, page 응답 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiBatchRunLogQueryRepositoryTest.java` | 필터, 정렬, pagination, total count |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 신규 UseCase/DTO 계약 포함 |

## 구현 순서

1. workflow 문서를 저장한다.
2. controller, service, repository, contract 테스트를 먼저 추가하고 실패를 확인한다.
3. api UseCase와 DTO를 추가한다.
4. repository/service/controller 구현을 추가한다.
5. OpenAPI와 `04_API_명세서.md`를 갱신한다.
6. 관련 테스트와 build를 실행한다.
7. report 문서를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AdminAiBatchRunLogControllerTest` | `OPERATOR/REVIEWER/SUPER_ADMIN` 성공, `CONTENT_CREATOR/USER/anonymous` 차단 |
| `AdminAiBatchRunLogControllerTest` | `batchName/status/from/to/page/size` query mapping과 page envelope 응답 |
| `AdminAiBatchRunLogQueryServiceTest` | 권한 재검증, page/size, enum/date validation, `from > to` 차단 |
| `AdminAiBatchRunLogQueryRepositoryTest` | batchName/status/KST 날짜 범위 필터, `createdAt desc, id desc`, total count |
| `AiUseCaseContractTest` | 신규 UseCase와 DTO record contract 포함 |

## 수용 기준

- [ ] `GET /api/v1/admin/ai/batch-run-logs`가 관리자 로그 목록을 반환한다.
- [ ] `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`만 조회할 수 있다.
- [ ] invalid query는 `INVALID_INPUT`으로 처리된다.
- [ ] 응답에 redacted `errorMessage`만 포함되고 원시 민감 데이터 저장/복원 경로가 없다.
- [ ] 신규 DB schema, scheduler, worker 변경이 없다.
- [ ] OpenAPI와 API 명세서가 실제 구현과 일치한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- controller, service, repository, DTO, OpenAPI가 같은 API contract에 강하게 연결되어 있다.
- TDD RED/GREEN 순서를 한 맥락에서 유지해야 query shape 변경 충돌을 줄일 수 있다.
- 파일 수는 많지만 변경 범위가 단일 AI 관리자 조회 API로 제한된다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 저장, TDD 구현, 문서 반영, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AdminAiBatchRunLog*"
.\gradlew.bat test --tests "*AiUseCaseContractTest"
.\gradlew.bat test --tests "*AiBatchRun*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml
```

## 후속 작업으로 남길 항목

- `GET /api/v1/admin/ai/monitoring` 운영 집계 API 구현.
- batch run log retention/정리 배치.
- 관리자 조회 API 자체의 audit logging 정책 확정.
