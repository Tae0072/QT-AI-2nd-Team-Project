# Report - 2026-06-15 ai-prompt-natural-instruction

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-prompt-natural-instruction` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-prompt-natural-instruction.md` |
| migration | `V47__convert_explanation_prompt_to_natural_instruction.sql` |
| 실행 방식 | workflow-spec-runner 기준 직접 실행 |

## 변경 요약

- EXPLANATION 생성 job의 사용자 prompt 조립을 서버 고정 한글 context + 관리자 자연어 추가 지시사항 구조로 변경했다.
- 기존 placeholder 기반 커스텀 prompt version은 legacy replacement 경로로 계속 동작하게 했다.
- `AiPromptVersion` 기본 system prompt와 기본 user prompt 값을 한글 운영 문구로 변경했다.
- V46은 다른 PR에서 사용 중이므로 V47 migration으로 기존 seed/default EXPLANATION 행만 한글 system prompt와 자연어 지시사항으로 전환한다.
- 관리자 `/ai-prompt-versions` 등록/상세 화면의 사용자 노출 용어를 `추가 생성 지시사항`으로 변경했다.
- API field와 DB column은 `userPromptTemplate`, `user_prompt_template` 그대로 유지했다.

## 변경 파일

- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java`
- `qtai-server/admin-server/src/main/resources/db/migration/V47__convert_explanation_prompt_to_natural_instruction.sql`
- `admin-web/src/pages/AiPromptVersionsPage.tsx`
- 관련 테스트와 workflow/report 문서

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `npm.cmd --prefix admin-web run typecheck` | 통과 |
| `npm.cmd --prefix admin-web test` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest` | 통과 |
| `.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiPromptManagementServiceTest` | 통과 |
| `.\gradlew :admin-server:test --tests com.qtai.domain.ai.web.AdminAiPromptVersionControllerTest` | 통과 |
| `.\gradlew :admin-server:bootJar :service-ai:bootJar` | 통과 |
| `git diff --check` | 통과, CRLF 변환 경고만 출력 |

PowerShell 실행 정책 때문에 `npm --prefix ...`는 `npm.ps1` 로딩 단계에서 차단되어 동일 명령을 `npm.cmd`로 재실행했다.

## 브라우저 확인

- `http://localhost:5173/ai-prompt-versions` 등록 모달에서 `추가 생성 지시사항` 라벨, 자연어 placeholder, 서버 자동 context 안내 문구가 표시됨을 확인했다.
- 등록 모달에서 기존 `사용자 프롬프트 템플릿` 라벨이 표시되지 않음을 확인했다.
- ACTIVE 목록 상세 drawer는 조회 중 인증이 풀려 로그인 화면으로 이동해 수동 확인하지 못했다. 상세 drawer 라벨은 동일 파일에서 `추가 생성 지시사항`으로 정적 변경되어 typecheck와 admin page contract test 범위에 포함됐다.

## 수용 기준 확인

- 관리자 입력값은 placeholder 템플릿이 아니라 자연어 추가 지시사항으로 생성 prompt에 포함된다.
- 생성 prompt에는 대상 정보, QT 본문, 구절, 참고자료가 서버 고정 한글 문구로 포함된다.
- legacy placeholder 템플릿 prompt version은 기존 치환 방식으로 유지된다.
- V47 migration은 기존 seed/default EXPLANATION 행만 갱신하고 커스텀 버전을 덮어쓰지 않도록 조건을 제한했다.
- `/ai-prompt-versions` 등록 화면은 `추가 생성 지시사항` 용어를 사용한다.

## 남은 항목

- 로컬 DB에는 V47 SQL을 직접 적용하지 않았다. 현재 변경은 migration 파일로 제공되며, docker/local Flyway 비활성 환경에서 즉시 화면 데이터까지 바꾸려면 동일 SQL을 로컬 MySQL에 1회 적용해야 한다.
- DB column/API field를 `userInstruction` 계열로 rename하는 public contract 변경은 별도 PR 범위다.
- 검사용 프롬프트와 validation checklist 한글화는 이번 작업에서 제외했다.
