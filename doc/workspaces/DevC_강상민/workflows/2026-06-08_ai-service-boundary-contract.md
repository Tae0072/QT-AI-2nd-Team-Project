# Workflow - 2026-06-08 ai-service-boundary-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-boundary-contract` |
| 기준 브랜치 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 기준 문서 | `03_아키텍처_정의서.md`, `07_요구사항_정의서.md`, `04_API_명세서.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/client/**`, `qtai-server/apis/ai-service/openapi.yaml` |

## 목표

AI 도메인을 `ai-service`로 분리하기 전에 AI가 외부 서비스에 요구하는 계약을 고정한다. 이번 작업은 구현체 교체가 아니라 경계 정의가 목적이다. 초기 구현체는 `qtai-server`를 호출하고, QT 분리 이후에는 `QtContextClient` 구현체의 base URL만 `qt-service`로 바꿀 수 있어야 한다.

## AI 외부 의존 API 목록

| 의존 대상 | 현재 호출 대상 | 향후 호출 대상 | 필요한 기능 | AI client interface |
| --- | --- | --- | --- | --- |
| QT | `qtai-server` | `qt-service` | QT 본문 컨텍스트 조회, 오늘 QT passage 존재 여부 조회 | `QtContextClient` |
| Bible | `qtai-server` | `bible-service` 또는 `qtai-server` | verseId 목록 또는 범위 기반 성경 구절 조회 | `BibleVerseClient` |
| Study | `qtai-server` | `study-service` 또는 `qtai-server` | 승인된 해설 publish, 승인 해설 숨김/비공개 처리 | `StudyPublishClient` |
| Audit | `qtai-server` | `audit-service` 또는 `qtai-server` | AI 생성/검증/관리자 작업 감사 로그 기록 | `AuditLogClient` |
| Admin/Auth | `qtai-server` | `admin-service` 또는 `qtai-server` | 관리자 활성 상태 및 세부 권한 검증 | `AdminAuthClient` |

## QT 분리 담당자와 맞출 항목

1. AI가 QT 본문 컨텍스트를 어디로 호출하는가?
   - 초기: `qtai-server`
   - QT 분리 후: `qt-service`
   - AI 코드는 `QtContextClient`만 의존하고, 구현체 설정으로 대상 URL을 교체한다.

2. QT context 응답 DTO 고정 필드
   - `passageId`
   - `bibleBook`
   - `chapter`
   - `startVerse`
   - `endVerse`
   - `passageReference`
   - `title`
   - `summary`
   - `passageContext`

## ai-service 소유 DB

| 테이블 | 소유 서비스 | 비고 |
| --- | --- | --- |
| `ai_generation_jobs` | `ai-service` | AI 생성 작업 상태와 재시도 기준 |
| `ai_generated_assets` | `ai-service` | 검증 전/후 AI 산출물 원본 및 상태 |
| `ai_validation_logs` | `ai-service` | 자동/자문/관리자 검증 로그 |
| `ai_prompt_versions` | `ai-service` | 생성 지시 버전과 해시 |
| `ai_validation_checklist_versions` | `ai-service` | 검증 체크리스트 registry |
| `validation_reference_jobs` | `ai-service` | 검증 참고자료 인덱싱/만료 작업 |

## qtai/study 소유 데이터

| 데이터 | 소유 서비스 | 비고 |
| --- | --- | --- |
| 사용자에게 실제 노출되는 승인 해설 read model | `qtai-server` 또는 `study-service` | AI 승인 후 publish API로 반영 |
| QT 본문 | `qtai-server` 또는 `qt-service` | AI는 직접 DB 조회 금지 |
| 성경 구절 | `qtai-server` 또는 `bible-service` | AI는 직접 DB 조회 금지 |

## 하지 말아야 할 것

- `ai-service`에서 QT/Study/Bible DB를 직접 조회하지 않는다.
- `ai-service`에서 다른 서비스 Entity를 공유하지 않는다.
- QT 분리 완료를 기다렸다가 AI 작업을 시작하지 않는다.
- Flutter/Admin Web이 바로 `ai-service`를 직접 호출하도록 바꾸지 않는다.
- AI provider secret, token, prompt 원문을 공개 응답이나 로그에 노출하지 않는다.

## 산출물

- AI 외부 의존 API 목록: 이 문서의 `AI 외부 의존 API 목록`
- ai-service OpenAPI 초안: `qtai-server/apis/ai-service/openapi.yaml`
- ai-service DB 소유 테이블 목록: 이 문서의 `ai-service 소유 DB`
- AI client interface 초안:
  - `QtContextClient`
  - `BibleVerseClient`
  - `StudyPublishClient`
  - `AuditLogClient`
  - `AdminAuthClient`

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
.\gradlew.bat compileJava
```

## REQUEST_CHANGES 반영 계획

- `BibleVerseClient`, `StudyPublishClient`, `AuditLogClient`, `AdminAuthClient` mock 구현체를 추가한다.
- 새 client 4종은 `AiBoundaryClientContractTest`에서 mock 계약을 검증한다.
- `AdminAuthClient`의 관리자 권한 값은 `String` 대신 AI client 전용 `AdminRole` enum으로 고정한다.
- `qtai-server/apis/ai-service/openapi.yaml` 응답은 공통 `ApiResponse` envelope 형태로 명시한다.
- 모든 ai-service write POST 계약에는 `Idempotency-Key` 헤더를 요구한다.
- Spectral lint는 현재 저장소에 `.spectral.yaml` ruleset이 없어 정식 실행할 수 없으며, 이번 PR에서는 YAML 파싱 검증과 ruleset 부재 사유를 기록한다.

## REQUEST_CHANGES 2차 반영 계획

- AI client mock은 `local/test` profile, `qtai.ai.client.mock.enabled=true`, `@ConditionalOnMissingBean` 조건을 모두 만족할 때만 bean으로 등록한다.
- AI client mock에는 `@Primary`를 부여하지 않아 실제 구현체보다 우선 주입되지 않도록 한다.
- F-15 Q&A `BLOCKED` 응답에는 `blockedReason`, `blockedReasonCategory`를 명시한다.
- `blockedReasonCategory`는 `VALUE_JUDGMENT`, `COUNSELING`, `FAITH_EVALUATION` 세 값으로 고정하고, 세부 차단 사유와 category 매핑을 OpenAPI에 고정한다.
- 모든 AI boundary client method는 공통 `AiClientException` 실패 모델을 명시한다.
- `AiClientException.FailureCode`는 retry 가능한 실패와 retry 불가능한 실패를 구분해 향후 timeout/retry/circuit breaker 구현 기준으로 사용한다.
- QT 오늘 본문 상태 계약에는 `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY` cacheStatus를 포함한다.
- ai-service OpenAPI `ErrorBody`에는 표준 확장 후보인 `fields`를 nullable object로 명시한다.

## System endpoint 계약 동기화 (2026-06-08)

AI가 MSA 분리 후 호출할 provider service 내부 endpoint 계약을 아래와 같이 고정한다. 이 계약은 AI outbound dependency이며, `qtai-server/apis/ai-service/openapi.yaml`의 `paths`에는 노출하지 않고 `x-ai-outbound-system-endpoints` vendor extension으로 관리한다.

### 공통 규약

- Base prefix: `/api/v1/system/**`
- 인증: `Authorization: Bearer {service-token}` + `SYSTEM_BATCH` 권한
- 응답 envelope: 기존 `ApiResponse<T>`
- 쓰기 endpoint: `Idempotency-Key` 헤더 필수
- 관측성: `traceparent` 요청 헤더 전파, 응답 `traceId` 반영
- 에러 변환: provider의 `ApiResponse.error(code,message)`를 AI 쪽 `AiClientException`으로 매핑
- Bible 데이터 가드: 한글성경 88.json과 KJV만 허용하고, 금지 번역본/저작권 QT 본문 텍스트는 seed, fixture, response에 포함하지 않는다.

### Endpoint 계약 6종

| 구분 | 제공 서비스 | AI client | method/path | 응답/요청 핵심 |
| --- | --- | --- | --- | --- |
| QT context | today-qt | `QtContextClient.getQtContext` | `GET /api/v1/system/qt/passages/{passageId}/context` | `QtContextResult`, `cacheStatus` 제외 |
| 오늘 QT 상태 | today-qt | `QtContextClient.getTodayQtPassageStatus` | `GET /api/v1/system/qt/passages/today/status?date=YYYY-MM-DD` | `qtDate`, `exists`, `passageId`, `cacheStatus` |
| Bible verse | bible | `BibleVerseClient` | `GET /api/v1/system/bible/verses/{verseId}`, `POST /api/v1/system/bible/verses:batch`, `GET /api/v1/system/bible/verses?book={book}&chapter={c}&startVerse={s}&endVerse={e}` | 단건, 목록, 범위 조회 |
| Study publish/hide | today-qt(study) | `StudyPublishClient` | `POST /api/v1/system/study/verse-explanations:publish`, `POST /api/v1/system/study/verse-explanations:hide` | 승인 해설 publish/hide, `Idempotency-Key` 필수 |
| Audit log | admin-service(audit) | `AuditLogClient` | `POST /api/v1/system/audit/logs` | 감사 로그 기록, `Idempotency-Key` 필수 |
| Admin/Auth | admin-service | `AdminAuthClient` | `GET /api/v1/system/admin/auth/active`, `GET /api/v1/system/admin/auth/verify`, `GET /api/v1/system/admin/auth/verify-any` | `AdminAuthResult`, role enum `OPERATOR`, `REVIEWER`, `CONTENT_CREATOR`, `SUPER_ADMIN` |

### QT context DTO 확정

`QtContextResult` 필드는 `passageId`, `bibleBook`, `chapter`, `startVerse`, `endVerse`, `passageReference`, `title`, `summary`, `passageContext`로 고정한다. `passageContext`는 본문 원문 전체가 아니라 AI 생성/검증에 필요한 허용된 메타/context 블록이다.

오늘 QT 상태 조회의 `cacheStatus` enum은 `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY`로 고정한다. QT context 조회 응답에는 `cacheStatus`를 포함하지 않는다.
