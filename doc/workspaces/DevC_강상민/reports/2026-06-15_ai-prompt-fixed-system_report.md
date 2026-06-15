# Report - 2026-06-15 ai-prompt-fixed-system

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-prompt-fixed-system` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-prompt-fixed-system.md` |
| migration | `V48__fix_explanation_system_prompt.sql` |
| 실행 방식 | workflow-spec-runner 기준 직접 실행 |

## 변경 요약

- EXPLANATION 프롬프트 등록 모달에서 `시스템 프롬프트` 입력칸을 제거했다.
- admin-web create payload에서 `systemPrompt` 전송을 제거했다.
- admin API request의 `systemPrompt` 검증을 optional로 완화했다.
- `AiPromptManagementService`는 create와 content hash 계산 시 서버 기본 system prompt를 사용한다.
- `admin-server`와 `service-ai`의 `ExplanationGenerationJobHandler`는 provider 요청 system prompt로 `AiPromptVersion.defaultSystemPrompt()`를 사용한다.
- `service-ai`의 `AiPromptVersion`에도 admin-server와 동일한 default prompt helper를 추가했다.
- V48 migration으로 기존 EXPLANATION row의 `system_prompt`와 `content_hash`를 기본 system prompt 기준으로 정규화한다.

## 변경 파일

- `admin-web/src/pages/AiPromptVersionsPage.tsx`
- `admin-web/src/api/aiPromptVersions.ts`
- `admin-web/scripts/admin-page-contracts.test.mjs`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`
- `qtai-server/admin-server/src/main/resources/db/migration/V48__fix_explanation_system_prompt.sql`
- 관련 테스트와 workflow/report 문서

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `npm.cmd --prefix admin-web run typecheck` | 통과 |
| `npm.cmd --prefix admin-web test` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.web.AdminAiPromptVersionControllerTest` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiPromptManagementServiceTest` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest` | 통과 |
| `.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest` | 1차 컴파일 실패 후 helper 추가, 재실행 통과 |
| `.\gradlew :admin-server:bootJar :service-ai:bootJar` | 통과 |
| `git diff --check` | 통과, CRLF 변환 경고만 출력 |

## 브라우저 확인

- `http://localhost:5173/ai-prompt-versions` 등록 모달을 열어 확인했다.
- 모달 내부에 `시스템 프롬프트` 라벨과 `EXPLANATION 시스템 프롬프트` placeholder가 없음을 확인했다.
- `추가 생성 지시사항` 입력은 유지됨을 확인했다.

## 수용 기준 확인

- 등록 모달에 `시스템 프롬프트` 입력칸이 없다.
- create payload는 `systemPrompt` 없이 전송된다.
- create API는 `systemPrompt` 없는 요청을 허용한다.
- 생성/평가 provider 요청은 항상 `AiPromptVersion.defaultSystemPrompt()`를 사용한다.
- V48 migration은 기존 EXPLANATION row를 기본 system prompt와 재계산된 hash로 정규화한다.
- response DTO와 상세 화면은 system prompt를 읽기 전용으로 계속 확인할 수 있다.

## 남은 항목

- 로컬 DB에는 V48 SQL을 직접 적용하지 않았다. docker/local Flyway 비활성 환경에서 즉시 데이터 확인이 필요하면 동일 SQL을 로컬 MySQL에 1회 적용해야 한다.
- `systemPrompt` response field와 `system_prompt` DB column 제거는 public contract 영향이 있어 별도 PR에서 검토한다.
- 검사용 프롬프트와 validation checklist는 이번 범위에서 제외했다.
