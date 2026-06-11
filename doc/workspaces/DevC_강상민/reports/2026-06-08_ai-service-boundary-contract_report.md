# Report - 2026-06-08 ai-service-boundary-contract

## 개요

- 작업명: `ai-service-boundary-contract`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-service-boundary-contract.md`
- 관련 F-ID: F-02, F-14, F-15
- 목적: AI 도메인을 `ai-service`로 분리하기 전에 외부 서비스 의존, DB 소유권, 서비스 간 API 계약 초안을 고정한다.

## 변경 요약

- AI 외부 의존 API 목록과 DB 소유권을 workflow 문서로 정리했다.
- `ai-service` 분리 초안 OpenAPI 문서를 추가했다.
- AI가 외부 서비스와 통신할 때 사용할 client interface 초안을 추가했다.
- 기존 `QtContextClient` 계약을 MSA 분리 기준에 맞춰 보강했다.
- 런타임 동작은 변경하지 않았다. 기존 AI 생성/검증 서비스 구현체는 아직 기존 UseCase 주입 구조를 유지한다.

## 산출물

| 산출물 | 경로 |
| --- | --- |
| AI 외부 의존 API 목록 | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-service-boundary-contract.md` |
| ai-service OpenAPI 초안 | `qtai-server/apis/ai-service/openapi.yaml` |
| ai-service DB 소유 테이블 목록 | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-service-boundary-contract.md` |
| AI client interface 초안 | `qtai-server/src/main/java/com/qtai/domain/ai/client/**` |

## AI 외부 의존 경계

| 의존 대상 | 현재 대상 | 향후 대상 | client interface |
| --- | --- | --- | --- |
| QT | `qtai-server` | `qt-service` | `QtContextClient` |
| Bible | `qtai-server` | `bible-service` 또는 `qtai-server` | `BibleVerseClient` |
| Study | `qtai-server` | `study-service` 또는 `qtai-server` | `StudyPublishClient` |
| Audit | `qtai-server` | `audit-service` 또는 `qtai-server` | `AuditLogClient` |
| Admin/Auth | `qtai-server` | `admin-service` 또는 `qtai-server` | `AdminAuthClient` |

## 구현 내용

- `QtContextClient`
  - `getQtContext(Long viewerId, Long qtPassageId)` 유지
  - `getTodayQtPassageStatus(LocalDate qtDate)` 추가
  - `TodayQtPassageStatus` record 추가
- `QtContextResult`
  - QT 분리 담당자와 맞출 DTO 필드 기준으로 정리
  - `passageId`, `bibleBook`, `chapter`, `startVerse`, `endVerse`, `passageReference`, `title`, `summary`, `passageContext`
- `BibleVerseClient`
  - 단일 verse, verseId 목록, 범위 조회 계약 추가
- `StudyPublishClient`
  - 승인 해설 publish, 승인 해설 hide 계약 추가
- `AuditLogClient`
  - AI 생성/검증/관리자 작업 감사 로그 기록 계약 추가
- `AdminAuthClient`
  - 활성 관리자 조회, 단일 역할 검증, 다중 역할 검증 계약 추가
- `AiQtClientContractTest`
  - QT context mock 계약 검증 보강
  - 오늘 QT passage status 계약 검증 추가
  - AI service boundary client들이 interface인지 검증 추가

## DB 소유권 정리

### ai-service 소유

- `ai_generation_jobs`
- `ai_generated_assets`
- `ai_validation_logs`
- `ai_prompt_versions`
- `ai_validation_checklist_versions`
- `validation_reference_jobs`

### qtai/study 쪽 소유

- 사용자에게 실제 노출되는 승인 해설 read model
- QT 본문
- 성경 구절

## 검증 결과

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
```

- 결과: PASS

```powershell
cd qtai-server
.\gradlew.bat compileJava
```

- 결과: PASS

```powershell
git diff --check
```

- 결과: PASS
- 비고: 일부 파일에서 LF가 CRLF로 변환될 수 있다는 Git 경고만 출력됨

```powershell
python -c "..."
```

- 대상: `qtai-server/apis/ai-service/openapi.yaml`
- 결과: PASS
- 확인 내용: YAML 파싱 성공, `openapi`, `info`, `paths`, `components` top-level key 존재, 총 7개 path 확인

## 실행하지 못한 검증

```powershell
npx @stoplight/spectral-cli lint qtai-server/apis/ai-service/openapi.yaml
```

- 결과: FAIL
- 사유: PowerShell 실행 정책으로 `npx.ps1` 실행 차단
- 후속 확인: `npx.cmd`로 CLI 실행은 가능했으나 저장소에 `.spectral.yaml` ruleset이 없어 중단됨

```powershell
npx.cmd @stoplight/spectral-cli lint qtai-server/apis/ai-service/openapi.yaml --ruleset spectral:oas
```

- 결과: FAIL
- 사유: 현재 환경의 Spectral CLI가 `spectral:oas` 별칭을 로컬 파일 경로로 해석함

## 주의 사항

- 이번 작업은 경계 계약 추가이며, 실제 `ExplanationGenerationJobHandler`, `AiAssetReviewService`의 외부 호출 구현을 새 client interface로 교체하지 않았다.
- `flutter-app/pubspec.lock` 변경은 PR diff에서 제외했다.
- `ai-service`가 QT/Study/Bible DB를 직접 조회하거나 다른 서비스 Entity를 공유하는 구조는 금지한다.
- Flutter/Admin Web이 당장 `ai-service`를 직접 호출하도록 변경하지 않는다.

## 후속 작업

- `QtContextClient`의 실제 HTTP 구현체를 추가하고 초기 base URL은 `qtai-server`로 둔다.
- `BibleVerseClient`, `StudyPublishClient`, `AuditLogClient`, `AdminAuthClient`의 HTTP 구현체를 추가한다.
- `ExplanationGenerationJobHandler`와 `AiAssetReviewService`가 기존 도메인 UseCase 대신 AI client interface를 바라보도록 전환한다.
- 저장소 공통 `.spectral.yaml` ruleset 또는 ai-service 전용 ruleset을 추가한 뒤 OpenAPI lint를 정식 검증에 포함한다.
## REQUEST_CHANGES 반영

- `BibleVerseClientMock`, `StudyPublishClientMock`, `AuditLogClientMock`, `AdminAuthClientMock`를 추가했다.
- `AiBoundaryClientContractTest`로 새 client 4종의 mock 계약을 검증하도록 했다.
- `AdminAuthClient`의 `adminRole`과 검증 요청 role을 `String`에서 `AdminRole` enum으로 변경했다.
- `ai-service` OpenAPI 초안의 성공/오류 응답을 `ApiResponse` envelope 형태로 감쌌다.
- 모든 ai-service write POST 계약에 `Idempotency-Key` 헤더를 추가했다.
- Spectral lint는 저장소 ruleset 부재로 정식 실행할 수 없어, YAML 파싱 검증과 ruleset 부재 사유를 유지한다.
## REQUEST_CHANGES 검증

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
```

- 결과: PASS

```powershell
cd qtai-server
.\gradlew.bat compileJava
.\gradlew.bat testClasses
```

- 결과: PASS

```powershell
git diff --check
```

- 결과: PASS
- 비고: 일부 파일에서 LF가 CRLF로 변환될 수 있다는 Git 경고만 출력됨

## REQUEST_CHANGES 2차 반영

- `AdminAuthClientMock`, `BibleVerseClientMock`, `StudyPublishClientMock`, `AuditLogClientMock`, `GetQtUseCaseMock` bean 등록을 `local/test` profile, `qtai.ai.client.mock.enabled=true`, `@ConditionalOnMissingBean` 조건으로 제한했다.
- mock에는 `@Primary`를 부여하지 않으며, 계약 테스트에서 이 조건을 reflection으로 검증하도록 했다.
- `RequestAiQaResponse`에 `blockedReason`, `blockedReasonCategory`, `generationJobId`를 추가했다.
- F-15 차단 category는 `VALUE_JUDGMENT`, `COUNSELING`, `FAITH_EVALUATION`로 고정하고, 세부 차단 사유와 category 매핑을 OpenAPI에 명시했다.
- `AiClientException`과 `FailureCode`를 추가해 client interface 차원의 공통 실패 모델을 정의했다.
- 모든 AI boundary client method가 `AiClientException`을 선언하도록 했다.
- `QtContextClient.TodayQtPassageStatus`에 `cacheStatus`를 추가하고 `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY` 값을 고정했다.
- ai-service OpenAPI `ErrorBody.fields`를 nullable object로 추가했다.

## REQUEST_CHANGES 2차 검증

```powershell
cd qtai-server
.\gradlew.bat compileJava
```

- 결과: PASS

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
```

- 결과: PASS
