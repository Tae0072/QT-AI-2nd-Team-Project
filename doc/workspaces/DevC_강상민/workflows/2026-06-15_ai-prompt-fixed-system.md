# Workflow - 2026-06-15 ai-prompt-fixed-system

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevC 강상민 |
| 작업 브랜치 | `feature/ai-prompt-fixed-system` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 - 관리자 AI 프롬프트 운영 개선 |
| 트리거 | EXPLANATION 프롬프트 등록 화면에 시스템 프롬프트 입력칸이 남아 있어 운영자가 고정 정책을 오해할 수 있음 |
| 기준 문서 | `AGENTS.md`, `doc/프로젝트문서/04_API_명세서.md`, `doc/프로젝트문서/09_Git_규칙.md`, `doc/프로젝트문서/18_코드_품질_게이트.md` |
| 해당 경로 | `admin-web/src/**`, `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`, `qtai-server/admin-server/src/main/resources/db/migration/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

EXPLANATION 시스템 프롬프트를 관리자 입력값이 아니라 서버 기본값으로 고정한다. 등록 화면에서는 시스템 프롬프트 입력칸을 제거하고, 생성 및 평가 실행 시 provider에 전달되는 system prompt도 서버 기본 system prompt를 사용한다.

기존 response DTO와 DB column은 유지해 운영 이력 조회와 public contract를 깨지 않는다. 상세 화면은 감사/확인용으로 시스템 프롬프트를 읽기 전용 표시한다.

## 범위

- `/ai-prompt-versions` 등록 모달에서 `시스템 프롬프트` 입력을 제거한다.
- create payload에서 `systemPrompt`를 보내지 않도록 admin-web 타입과 submit 로직을 변경한다.
- admin API request의 `systemPrompt` 검증을 optional로 완화하고, 값이 들어와도 생성에는 사용하지 않는다.
- `AiPromptManagementService`는 create와 content hash 계산 시 `AiPromptVersion.defaultSystemPrompt()`를 사용한다.
- `ExplanationGenerationJobHandler`는 DB row의 system prompt가 아니라 서버 기본 system prompt를 provider 요청에 전달한다.
- `V48__fix_explanation_system_prompt.sql`로 기존 EXPLANATION row의 `system_prompt`와 `content_hash`를 기본 system prompt 기준으로 정규화한다.
- 관련 테스트, workflow, report를 작성하고 커밋한다.

## 제외 범위

- `system_prompt` DB column과 response DTO의 `systemPrompt` field는 제거하지 않는다.
- 검사용 프롬프트, validation checklist, `AiReviewValidationService`는 변경하지 않는다.
- 추가 생성 지시사항 UX와 자연어 user prompt 조립 정책은 이전 PR 상태를 유지한다.
- PR 생성/푸시는 이번 작업 범위가 아니다.
- 금지 번역본 seed/test/fixture/response 데이터와 prompt/provider raw response, secret/token/password 예시는 추가하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/AiPromptVersionsPage.tsx` | 등록 모달 system prompt 입력 제거, 상세 라벨 읽기 전용 표시 |
| Modify | `admin-web/src/api/aiPromptVersions.ts` | create payload에서 `systemPrompt` 제거 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiPromptVersionController.java` | `systemPrompt` optional request로 완화 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptManagementService.java` | create/hash에서 서버 기본 system prompt 사용 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | provider system prompt 고정 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | provider system prompt 고정 |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V48__fix_explanation_system_prompt.sql` | 기존 EXPLANATION system prompt와 hash 정규화 |
| Test | 관련 admin/service-ai 테스트 | optional API, hash, provider 요청, UI contract 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-15_ai-prompt-fixed-system_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `origin/dev` 기준 `feature/ai-prompt-fixed-system` 브랜치에서 작업한다.
2. admin-web 등록 form에서 `systemPrompt` form value, Form.Item, submit payload를 제거한다.
3. admin API create request에서 `systemPrompt`를 optional로 바꾸고 controller가 service command에 기본 system prompt를 넘기게 한다.
4. `AiPromptManagementService`의 create/hash 계산이 command system prompt가 아니라 `AiPromptVersion.defaultSystemPrompt()`를 사용하게 한다.
5. admin-server와 service-ai의 `ExplanationGenerationJobHandler` provider request system prompt를 `AiPromptVersion.defaultSystemPrompt()`로 바꾼다.
6. V48 migration을 추가해 기존 EXPLANATION row를 기본 system prompt 기준으로 정규화한다.
7. controller/service/handler/admin-web 테스트 기대값을 갱신한다.
8. 지정된 npm/Gradle 검증과 브라우저 등록 모달 확인을 수행한다.
9. report 작성 후 변경 파일을 stage하고 Conventional Commits 형식으로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가/변경 검증 |
| --- | --- |
| `AdminAiPromptVersionControllerTest` | `systemPrompt` 없는 create payload가 201로 생성됨 |
| `AiPromptManagementServiceTest` | custom system prompt가 들어와도 기본 system prompt로 저장/해시 계산됨 |
| `ExplanationGenerationJobHandlerTest` | prompt version에 custom system prompt가 있어도 provider 요청은 기본 system prompt를 사용함 |
| `admin-page-contracts.test.mjs` | prompt 등록 화면에서 system prompt 입력이 제거된 계약을 확인함 |

## 수용 기준

- [ ] 등록 모달에 `시스템 프롬프트` 입력칸이 없다.
- [ ] create payload는 `systemPrompt` 없이 전송된다.
- [ ] create API는 `systemPrompt` 없는 요청을 허용한다.
- [ ] 생성/평가 provider 요청은 항상 `AiPromptVersion.defaultSystemPrompt()`를 사용한다.
- [ ] 기존 EXPLANATION row는 V48 migration으로 기본 system prompt와 재계산된 hash를 갖는다.
- [ ] response DTO와 상세 화면은 system prompt를 읽기 전용으로 계속 확인할 수 있다.
- [ ] 지정된 테스트와 `git diff --check`가 통과한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- UI, API request, service hash, provider 요청, migration이 같은 system prompt 정책 변경에 강하게 연결되어 있다.
- 잘못 분리하면 hash 계산과 실제 provider 요청 system prompt가 어긋날 수 있다.
- 테스트 기대값을 한 흐름으로 맞춰야 하므로 메인 에이전트 직접 실행이 안전하다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 검증, report 작성, 커밋을 직접 수행한다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
npm.cmd --prefix admin-web test
.\gradlew :admin-server:test --tests com.qtai.domain.ai.web.AdminAiPromptVersionControllerTest
.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiPromptManagementServiceTest
.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest
.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest
.\gradlew :admin-server:bootJar :service-ai:bootJar
git diff --check
```

브라우저 검증은 `http://localhost:5173/ai-prompt-versions` 등록 모달에서 `시스템 프롬프트` 입력이 사라졌는지 확인한다.

## 후속 작업으로 남길 항목

- `systemPrompt` response field와 DB column 제거는 public contract와 migration 영향이 커 별도 PR에서 검토한다.
- 검사용 프롬프트 고정/한글화는 별도 범위로 분리한다.
