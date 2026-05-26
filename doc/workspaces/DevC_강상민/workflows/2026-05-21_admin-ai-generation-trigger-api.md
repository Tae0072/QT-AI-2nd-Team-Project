# Workflow — 2026-05-21 admin-ai-generation-trigger-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-06, F-14 |
| 트리거 | "관리자 AI 생성 트리거 API 만들기" 작업. 범위 선택: 관리자 재생성 API만 구현 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 해당 경로 | 아래 해당 경로 목록 참고 |

## 기준 문서

- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/05_시퀀스_다이어그램.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `CODE_CONVENTION.md`
- `.github/CODEOWNERS`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-usecase-contracts.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_ai-failure-retry-policy.md`

## 해당 경로

- `qtai-server/src/main/java/com/qtai/domain/ai/web/**`
- `qtai-server/src/main/java/com/qtai/domain/ai/api/**`
- `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`
- `qtai-server/src/test/java/com/qtai/domain/ai/**`
- `qtai-server/src/test/java/com/qtai/domain/ai/web/**`

## 작업 목표

관리자 웹에서 AI 산출물 재생성을 요청할 수 있는 HTTP API를 만든다. 대상 API는 `POST /api/v1/admin/ai/assets/{assetId}/regenerate`이며, 요청을 받으면 기존 산출물과 검증 로그를 덮어쓰지 않고 새 `ai_generation_jobs` 작업을 `QUEUED` 상태로 등록한다.

이번 작업은 관리자 트리거 API의 접수와 작업 큐잉까지가 목표다. 실제 DeepSeek 호출, 배치 실행, 산출물 생성, 검증 로그 등록은 후속 작업으로 분리한다.

## 범위

- `POST /api/v1/admin/ai/assets/{assetId}/regenerate` 관리자 API를 구현한다.
- 요청 DTO는 API 명세의 `reason`, `promptVersionId`를 기준으로 둔다.
- 응답 DTO는 `generationJobId`, `status`, `createdAt`을 반환한다.
- 관리자 권한은 일반 `ADMIN` 역할과 `admin_users.admin_role=REVIEWER` 또는 `SUPER_ADMIN` 기준을 모두 만족해야 한다.
- 재생성 가능 상태는 `REJECTED`, `HIDDEN`으로 제한한다.
- `VALIDATING`은 검증 중이므로 재생성을 차단한다.
- `APPROVED`는 기존 노출본 보호를 위해 이번 작업에서는 재생성을 차단한다.
- 동일 대상에 `QUEUED` 또는 `RUNNING` 생성 작업이 있으면 중복 생성하지 않는다.
- 재생성 요청은 감사 로그 기록 대상임을 AI 코드와 후속 항목에 남긴다.
- `domain.audit.api` 계약 변경과 실제 `audit_logs` 기록 연결은 audit 소유자/Lead 확인 후 별도 작업으로 분리한다.
- `domain.ai` 내부 구현은 다른 도메인의 `internal`, `web`, `repository`를 직접 import하지 않는다.

## 제외 범위

- `POST /api/v1/system/ai/generation-jobs` HTTP API 구현은 제외한다.
- 실제 DeepSeek 또는 외부 LLM 호출은 제외한다.
- AI 산출물 payload 생성, 자동 검증, 검증 로그 등록은 제외한다.
- 관리자 웹 UI 구현은 제외한다.
- `APPROVED` 산출물의 강제 재생성 또는 기존 노출본 교체 정책은 이번 작업에서 구현하지 않는다.
- `domain.audit.api` 계약 수정, `audit_logs` 저장 구현, 감사 로그 조회 API 구현은 제외한다.
- prompt 원문, provider raw response, 검증 참조 원문은 요청·응답에 포함하지 않는다. 향후 감사 로그 연동 시에도 포함하지 않는다.

## 정책 결정

| 구분 | 기준 |
| --- | --- |
| API | `POST /api/v1/admin/ai/assets/{assetId}/regenerate` |
| 권한 | `ADMIN` + `REVIEWER` 또는 `SUPER_ADMIN` |
| 요청 | `{ "reason": "...", "promptVersionId": 3 }` |
| 성공 응답 | `{ "generationJobId": 101, "status": "QUEUED", "createdAt": "..." }` |
| 허용 상태 | `REJECTED`, `HIDDEN` |
| 차단 상태 | `VALIDATING`, `APPROVED` |
| 중복 차단 | 같은 target, asset type, prompt version 기준의 `QUEUED` 또는 `RUNNING` job |
| 감사 로그 | 이번 PR에서는 계약 변경 없이 `AI_REGENERATE_REQUEST` 기록 필요성만 TODO/후속으로 남김 |
| 민감/보호 데이터 | prompt 원문, provider raw response, 검증 참조 원문, secret, token 저장 금지 |

## 구현 순서

1. 현재 `domain.ai`의 `ReviewAiAssetUseCase`, `CreateAiGenerationJobUseCase`, `AiGeneratedAsset`, `AiGenerationJob` 계약을 확인한다.
2. API 명세와 현재 코드의 gap을 확인한다. 특히 문서는 `promptVersionId`를 쓰지만 현재 `CreateAiGenerationJobCommand`는 `promptVersion` 문자열을 사용한다.
3. 외부 HTTP DTO는 `domain.ai.web`에 두고, UseCase DTO는 `domain.ai.api.dto`에 둔다.
4. `AdminAiAssetController` 또는 기존 AI 관리자 controller를 `domain.ai.web`에 추가해 `/api/v1/admin/ai/assets/{assetId}/regenerate`를 노출한다.
5. `ReviewAiAssetUseCase`에 `REGENERATE` action을 연결하거나, 기존 계약이 과하게 넓어지면 전용 `RegenerateAiAssetUseCase`를 추가한다.
6. 서비스 계층에서 대상 asset 조회, 상태 검증, 중복 job 확인, 새 generation job 등록을 하나의 transaction으로 처리한다.
7. 감사 로그는 `domain.audit.api` 소유자가 계약을 확정한 뒤 `AI_REGENERATE_REQUEST`로 연결하도록 TODO/후속 항목에 남긴다.
8. 예외 응답은 공통 `ApiResponse`와 `ErrorCode` 기준을 따른다. 상태 전이 불가는 `409 INVALID_STATUS_TRANSITION` 계열로 맞춘다.
9. Controller 테스트와 service/domain 테스트를 추가한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | `POST /api/v1/admin/ai/assets/{assetId}/regenerate` 요청/응답 DTO 매핑 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | `ADMIN + REVIEWER/SUPER_ADMIN` 권한만 허용 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | `REJECTED`, `HIDDEN` asset은 새 `QUEUED` job을 만든다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | `VALIDATING`, `APPROVED` asset은 재생성을 차단한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | 기존 `QUEUED` 또는 `RUNNING` job이 있으면 중복 생성하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | UseCase 계약이 command/result record 규칙과 raw provider/reference text 금지 규칙을 유지한다 |

## 수용 기준

- [ ] 관리자 재생성 API가 `/api/v1/admin/ai/assets/{assetId}/regenerate` 경로로 노출된다.
- [ ] 성공 시 새 `ai_generation_jobs`가 `QUEUED` 상태로 생성된다.
- [ ] 기존 `ai_generated_assets`와 `ai_validation_logs`는 덮어쓰지 않는다.
- [ ] `REJECTED`, `HIDDEN` 상태만 재생성 요청을 허용한다.
- [ ] `VALIDATING`, `APPROVED` 상태는 명확한 오류로 차단한다.
- [ ] 같은 대상의 진행 중 생성 작업이 있으면 중복 요청을 차단한다.
- [ ] 관리자 권한은 `ADMIN` 역할과 세부 관리자 권한을 모두 확인한다.
- [ ] 재생성 요청은 감사 로그 기록 대상임이 AI 코드 TODO와 후속 작업에 남는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문, secret, token은 요청·응답에 남지 않고, 향후 감사 로그 연동 금지 항목으로 남는다.
- [ ] 사용자 앱 경로 또는 `/api/v1/ai/**`에서 해설·시뮬레이터 생성이 시작되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 단일 관리자 API와 AI 도메인 내부 상태 검증이 강하게 연결된 좁은 범위다.
- Controller, UseCase, service, test가 같은 정책 결정을 공유하므로 한 흐름에서 직접 구현하는 편이 충돌 가능성이 낮다.
- 실제 DeepSeek 실행, 시스템 API, 관리자 UI는 제외되어 병렬화 이점이 크지 않다.

### 위임 가능한 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 문서 기준 확인, API 구현, 테스트 보강, 검증 명령 실행을 직접 수행한다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server test`
- `./gradlew -p qtai-server build`
- `rg -n "domain\\.[a-z]+\\.internal|domain\\.[a-z]+\\.web" qtai-server/src/main/java/com/qtai/domain/ai`로 금지 import 여부 확인
- 변경 범위가 OpenAPI 파일을 수정하지 않는다면 Spectral lint는 생략하고, 수정한 경우 `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`을 실행한다.

## 후속 작업으로 넘길 항목

- `POST /api/v1/system/ai/generation-jobs` 시스템 API 구현
- 실제 DeepSeek 호출과 산출물 생성 파이프라인 연결
- AI 검증 로그 자동 등록 및 검증 체크리스트 연동
- `APPROVED` 산출물의 조건부 재생성 정책과 기존 노출본 교체 정책
- 관리자 웹 화면에서 재생성 버튼과 실패/대기 상태 표시
- `promptVersionId`와 `ai_prompt_versions` 저장 구조 확정
- Lead/audit 소유자와 `WriteAuditLogUseCase` 계약 확정 후 `AI_REGENERATE_REQUEST` 감사 로그 기록 연결
