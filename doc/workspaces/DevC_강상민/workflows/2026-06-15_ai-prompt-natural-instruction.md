# Workflow - 2026-06-15 ai-prompt-natural-instruction

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevC 강상민 |
| 작업 브랜치 | `feature/ai-prompt-natural-instruction` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 - 관리자 AI 프롬프트 운영 개선 |
| 트리거 | EXPLANATION 생성 프롬프트에서 관리자 입력값을 전체 템플릿이 아니라 자연어 추가 지시사항으로 운영해야 함 |
| 기준 문서 | `AGENTS.md`, `doc/프로젝트문서/04_API_명세서.md`, `doc/프로젝트문서/09_Git_규칙.md`, `doc/프로젝트문서/18_코드_품질_게이트.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`, `qtai-server/admin-server/src/main/resources/db/migration/**`, `admin-web/src/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

관리자 화면의 `사용자 프롬프트 템플릿` 입력을 `추가 생성 지시사항` 개념으로 전환한다. 생성 job은 본문, 구절, 참고자료, 대상 정보를 서버에서 고정 프롬프트로 조립하고, DB에 저장된 관리자 입력값은 자연어 지시사항으로만 덧붙인다.

가능한 프롬프트 문구는 한글화하되, 기존 API 필드명과 DB 컬럼명은 유지해서 public contract와 운영 데이터 구조를 흔들지 않는다.

## 범위

- `admin-server`와 `service-ai`의 EXPLANATION 생성 사용자 프롬프트 조립 방식을 동일하게 변경한다.
- `AiPromptVersion` 기본 system prompt와 기본 user prompt 값을 한글 운영 문구로 변경한다.
- 기존 placeholder 템플릿이 남은 prompt version은 legacy replacement 경로로 계속 동작하게 한다.
- `V47__convert_explanation_prompt_to_natural_instruction.sql`을 추가해 기존 seed/default EXPLANATION 행만 한글 system prompt와 자연어 지시사항으로 전환한다.
- 관리자 웹의 등록/상세 UI 라벨과 placeholder를 `추가 생성 지시사항` 기준으로 변경한다.
- workflow와 report 문서를 작성한다.

## 제외 범위

- V46 migration 파일은 생성하거나 수정하지 않는다.
- 검사용 프롬프트, validation checklist, `AiReviewValidationService`는 변경하지 않는다.
- public API 경로, DTO wire field, DB column rename은 하지 않는다.
- Bible 금지 번역본 seed/test/fixture/response 데이터와 prompt/provider raw response, secret/token/password 예시는 추가하지 않는다.
- 생성 결과 평가 화면의 기능 변경은 하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V47__convert_explanation_prompt_to_natural_instruction.sql` | 기존 seed/default EXPLANATION prompt를 한글 system prompt와 자연어 지시사항으로 전환 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | 관리자 서버 EXPLANATION 생성 prompt 조립 방식 변경 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | AI 서비스 EXPLANATION 생성 prompt 조립 방식 변경 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java` | 관리자 서버 기본 prompt 문구 한글화 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java` | AI 서비스 기본 prompt 문구 한글화 |
| Modify | `admin-web/src/pages/AiPromptVersionsPage.tsx` | 관리자 UI 라벨과 placeholder 변경 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | 자연어 지시사항과 legacy placeholder 조립 검증 |
| Test | `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | 자연어 지시사항과 legacy placeholder 조립 검증 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiPromptManagementServiceTest.java` | prompt 생성/조회 계약 유지 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-15_ai-prompt-natural-instruction_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `origin/dev` 기준 `feature/ai-prompt-natural-instruction` 브랜치에서 작업한다.
2. 기존 `ExplanationGenerationJobHandler` 테스트에 자연어 지시사항 prompt 조립 기대값과 legacy placeholder 기대값을 먼저 추가한다.
3. `ExplanationGenerationJobHandler`에 고정 한글 context prompt builder, 추가 지시사항 block, legacy placeholder 감지/치환 경로를 구현한다.
4. `AiPromptVersion` 기본 system prompt와 기본 user prompt 값을 한글 문구로 변경한다.
5. `V47__convert_explanation_prompt_to_natural_instruction.sql`을 추가하고, 기존 seed/default 행만 갱신하도록 조건을 제한한다.
6. 관리자 UI 라벨과 placeholder를 `추가 생성 지시사항`으로 변경하되 request/response 필드는 `userPromptTemplate` 그대로 유지한다.
7. 관련 테스트와 타입 검사를 실행한다.
8. 로컬 화면 확인이 가능하면 `/ai-prompt-versions`에서 등록/상세 문구가 바뀌었는지 확인한다.
9. report 문서에 변경 내용, 검증 결과, 미실행 검증 사유를 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | 자연어 지시사항이 고정 한글 context 뒤에 추가되고 placeholder 없이 동작함 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | legacy placeholder 템플릿은 기존 치환 방식으로 동작함 |
| `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | admin-server와 동일한 생성 prompt 동작을 보장함 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiPromptManagementServiceTest.java` | API 응답 필드명과 생성/조회 계약이 유지됨 |

## 수용 기준

- [ ] 관리자 입력값은 placeholder 템플릿이 아니라 자연어 추가 지시사항으로 생성 prompt에 포함된다.
- [ ] 생성 prompt에는 대상 정보, QT 본문, 구절, 참고자료가 서버 고정 한글 문구로 포함된다.
- [ ] legacy placeholder 템플릿 prompt version은 기존 치환 방식으로 깨지지 않는다.
- [ ] V47 migration은 기존 seed/default EXPLANATION 행만 갱신하고 커스텀 버전을 덮어쓰지 않는다.
- [ ] `/ai-prompt-versions` 등록/상세 화면이 `추가 생성 지시사항` 용어를 사용한다.
- [ ] 지정된 Gradle, npm, `git diff --check` 검증이 통과하거나 미실행 사유가 report에 기록된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- backend 양쪽 모듈, migration, admin-web 문구, 테스트 기대값이 같은 의미 변경에 묶여 있다.
- prompt 조립 정책과 seed 전환 조건을 한 흐름으로 검증해야 해서 병렬 편집보다 직접 실행이 안전하다.
- 사용자 요청이 한글 문구와 운영 의미 전환을 포함하므로 최종 일관성 확인이 중요하다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, backend 구현, UI 수정, migration 추가, 테스트, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm --prefix admin-web run typecheck
npm --prefix admin-web test
.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest
.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest
.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiPromptManagementServiceTest
.\gradlew :admin-server:bootJar :service-ai:bootJar
git diff --check
```

브라우저 검증은 `http://localhost:5173/ai-prompt-versions`에서 등록 drawer와 상세 drawer의 라벨, placeholder, 기존 ACTIVE prompt 표시를 확인한다.

## 후속 작업으로 남길 항목

- DB 컬럼명과 DTO 필드명을 `userInstruction` 계열로 바꾸는 public contract 변경은 별도 PR에서 검토한다.
- 검사용 프롬프트와 validation checklist 한글화는 별도 범위로 분리한다.
