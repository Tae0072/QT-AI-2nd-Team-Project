# Report — 2026-05-18 lead-pipeline-rehearsal

| 항목 | 내용 |
| --- | --- |
| 담당자 | Lead_강태오 |
| 브랜치 | `chore/infra-workspaces-bootstrap` |
| PR 대상 | `dev` |
| PR 링크 | https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/8 |
| 커밋 | `1eef3c2` (Co-Author: Claude Opus 4.7 — Lead 본인 별도 세션 작업) + `219e25b` (본 작업) |
| Workflow | `workspaces/Lead_강태오/workflows/2026-05-18_lead-pipeline-rehearsal.md` |

---

## 1. 변경 내용

| 분류 | 파일 | 변경 |
| --- | --- | --- |
| 신규 | `workspaces/Lead_강태오/README.md` | 122 줄 추가 — Lead 역할, 담당 도메인 3개(`domain.member` / `domain.qt` / `domain.admin`), 대표 branch scope, 폴더 구조, 흔들리지 않는 본인 기준, 매일 작업 순서, 주차별 집중 범위, 우선 액션, 저장소 분리 원칙 |
| 신규 | `workspaces/Lead_강태오/workflows/2026-05-18_lead-pipeline-rehearsal.md` | 78 줄 추가 — 이 작업의 사전 workflow (자기참조) |
| 신규 | `workspaces/Lead_강태오/reports/2026-05-18_lead-pipeline-rehearsal_report.md` | 이 파일 자체 |

- 변경 범위는 모두 `workspaces/Lead_강태오/` 안. 다른 팀원 폴더 / 빌드 / 런타임 / 테스트 / CI 영향 0
- 새 파일 줄바꿈: 모두 **LF** (122 LF / 0 CRLF). dev 기존 파일에 CRLF 섞여 있으나 본 PR 범위 밖

---

## 2. 검증 결과

| 축 | 명령 | 결과 |
| --- | --- | --- |
| Whitespace | `git diff --check` | ✅ 출력 없음 |
| 금지 패턴 grep (`workspaces/Lead_강태오/` 한정) | `Select-String -Pattern "Kafka\|SseEmitter\|/ai/sessions\|RAG\|ChromaDB\|Elasticsearch\|개역개정\|ESV\|NIV\|church verification\|churchAuth\|church_cert\|youtubeUrl\|recommend.*song"` | 🟡 8건 매치 — **모두 "금지를 설명하는 문구"**. README §5 / §8, workflow §3 / §4의 차단 기준 인용. `qt_ai_forbidden` 메모리 예외(`.github/pull_request_template.md`처럼 금지 항목을 체크리스트로 설명하는 문서성 파일은 검사 제외)와 같은 성격. 자동 Guard가 잡으면 사람 리뷰로 override 또는 grep 룰 보정 후속 PR |
| 변경 범위 한정 | `git diff --cached --stat` | ✅ `workspaces/Lead_강태오/` 안 2 파일 200줄 추가만 staged |
| 줄바꿈 검사 | `Get-Content -Encoding Byte` 직접 카운트 | ✅ 새 파일 모두 LF. `.gitattributes` 부재 상태에서도 일관됨 |

---

## 3. PR에 함께 포함된 추가 commit (사실 보고)

본 PR에는 본 작업(`219e25b`) 외에 `1eef3c2` commit이 함께 포함되어 있다. 처음에는 워킹 트리에 의도하지 않은 자동 변경으로 인식했으나, PR 생성 직전 정체가 명확해졌다.

| commit | author | 시각 (KST) | 내용 |
| --- | --- | --- | --- |
| `1eef3c2` | Tae0072 (Co-Author: Claude Opus 4.7) | 2026-05-18 16:00:31 | `ci: 서비스/모듈 부재 시 build·test 단계 skip — workspace reset 견고화` — `services/*/build.gradle.kts`와 `apps/mobile/pubspec.yaml` 부재 시 `setup-java` 캐시·`flutter-test` working-directory가 죽는 문제 해결. `fail-fast: false` 추가 |
| `219e25b` | Tae0072 | 2026-05-18 16:13:03 | 본 작업 — `workspaces/Lead_강태오/` README + workflow + report |

`1eef3c2`는 **Lead 본인이 별도 Claude 세션에서 작성·push한 작업**(commit message body의 Co-Authored-By 라인으로 식별)으로, 우리 작업 시작 전 이미 `origin/chore/infra-workspaces-bootstrap`에 push되어 있었다. `git reset --hard origin/dev`로 잠시 가려졌다가 본 commit 시점에 우리 작업과 함께 PR에 들어왔다.

본 PR과 정합되는 의미 있는 변경이라 그대로 함께 포함했다(사용자 결정 2026-05-18).

### 3.1 PR에 포함하지 않은 자동 변경 (워킹 트리에만 존재)

`.gitignore` 미정의 / 자동 prettifier / IDE 산출물 등으로 워킹 트리에 다음 변경·신규 파일이 떴다. 본 PR과 무관해 staging 제외하고 unstaged 또는 untracked 상태로 두었다.

| 시점 | 경로 | 종류 | 비고 |
| --- | --- | --- | --- |
| 검증 단계 | `.github/CODEOWNERS` | modified | `services/bible-service/` Owner `@xogurrh012`(김태혁) 활성화. 다른 PR에서 처리 |
| 검증 단계 | `BRANCHING.md` | modified | 마크다운 표 padding prettify (의미 변화 0). 별도 PR로 무시 또는 정리 |
| push 후 | `.vscode/` | untracked | IDE 설정. `.gitignore` 미등재 |
| push 후 | `services/{ai,auth,bff,bible,gateway,journal}-service/bin/` × 6 | untracked | Gradle/IDE 산출물. `.gitignore`에 `bin/` 미등재 (`build/`, `out/`만 있음) |

후속: `.gitignore`에 `bin/`, `.vscode/` 추가 별도 PR 권장.

작업 중 떠 있던 백그라운드 프로세스: `claude` × 18, `Codex` / `codex` × 6, `cloudcode_cli` × 1. 다수 Claude 세션이 동시 작업한 흔적이지만, 본 PR과 직접 충돌한 사례는 없었다.

---

## 4. CI / 자동 리뷰 결과 — 모두 정상 동작 확인 ✅

PR #8 (2026-05-18 16:19:21 KST 시작) 자동화 결과.

### 4.1 GitHub Actions Check (9/9 모두 통과)

| 검사 | 결과 | 소요 |
| --- | --- | --- |
| `Decisions Guard` | ✅ PASS | 4s |
| `Flutter Test` | ✅ PASS | 53s |
| `Spring Boot Build & Test (ai-service)` | ✅ PASS | 58s |
| `Spring Boot Build & Test (auth-service)` | ✅ PASS | 41s |
| `Spring Boot Build & Test (bff-aggregator)` | ✅ PASS | 58s |
| `Spring Boot Build & Test (bible-service)` | ✅ PASS | 1m 16s |
| `Spring Boot Build & Test (gateway)` | ✅ PASS | 42s |
| `Spring Boot Build & Test (journal-service)` | ✅ PASS | 48s |
| `claude-review` (workflow 자체 실행) | ✅ PASS | 49s |

`1eef3c2`의 skip 가드 덕분에 `services/*/build.gradle.kts`가 없는 상태에서도 6 서비스 매트릭스 빌드 전부 PASS(빌드 step skip). `apps/mobile/`은 실제 존재하므로 Flutter Test도 PASS.

### 4.2 Claude PR Auto Review 결과: 🔴 CHANGES_REQUESTED

`github-actions[bot]` 명의로 REQUEST_CHANGES 출력. 주요 지적:

| 구분 | 내용 | 본인 판단 |
| --- | --- | --- |
| ✅ 긍정 | CI workflow의 논리적 개선, `fail-fast=false` 안정성, 명확한 가드 로직 | 동의 |
| ⚠️ 개선 | README.md 122줄 / CI 로그 한국어 메시지 / `gradlew` 실행 권한 미체크 | 부분 동의. README 길이는 본인 가이드 §0~9 전체 인용이라 정당. 한국어 메시지는 팀 규칙(한국어 우선)에 부합 |
| ❌ REQUEST_CHANGES 핵심 사유 | "단위/통합 테스트가 전혀 없음" / "chore 타입인데 사실상 feat 성격" | **부적절**. 본 PR은 `workspaces/Lead_강태오/` 문서만 추가 (빌드/런타임/테스트/CI 영향 0). 가이드 §2 "docs/chore 타입은 해당 없음" 명시 |
| 문서화 지적 | README와 CLAUDE.md 중복 / 개인 워크스페이스에 전체 가이드라인 포함 → 책임 범위 불분명 | 동의(부분). CLAUDE.md는 v2.x 기준이고 본 README는 v3.1 정합 목적이라 의도된 일부 중복. 향후 CLAUDE.md를 v3.1로 정렬하는 별도 PR에서 중복 정리 |

### 4.3 자동 머지 차단 — 정상 동작 ✅

- `mergeStateStatus`: `CLEAN` (충돌 없음)
- `mergeable`: `MERGEABLE`
- `reviewDecision`: `CHANGES_REQUESTED`
- `claude-pr-review.yml`의 `if (decision !== 'APPROVE') { ... return; }` 로직대로 자동 머지 skip → **사람 리뷰 없이 머지 안 됨** (의도대로)

### 4.4 Requirements Guard

`.github/workflows/` 에 별도 Requirements Guard 워크플로우 부재 확인. dev v2.x 기준이라 v3.1 금지 패턴(Kafka/SSE/RAG/개역개정 등) 자동 차단 기능은 아직 없음. 후속 PR 대상.

### 4.5 결론

가이드 §0의 본인 우선 액션 3번 ("AI 자유 챗봇·SSE·Kafka·K8s·Helm·RAG 금지 기준을 CI/리뷰 체크로 고정")의 자동화 인프라 자체는 **이미 동작 가능 상태**. 단 Requirements Guard만 v3.1 기준 룰셋으로 추가 도입 필요.

**오늘 W1 Day1 핵심 성과:** PR 자동화 파이프라인(`dev` PR → 9개 체크 → Claude 리뷰 → 자동 머지 차단)이 가이드 의도대로 끝까지 동작함을 실제 PR로 검증 완료.

---

## 5. 남은 리스크

| 리스크 | 후속 대응 |
| --- | --- |
| Claude PR Auto Review가 `chore`/문서-only PR에 "테스트 부재"로 REQUEST_CHANGES → 동일 패턴 PR마다 자동 머지 차단됨 | 사람 리뷰로 override 또는 리뷰 룰에 "워크스페이스 / 문서 변경은 테스트 면제" 조건 추가 (별도 PR) |
| dev `.github/pull_request_template.md`가 v2.x 기준이라 PR 본문 자동 로드 체크리스트(Kafka envelope `data`, SSE `/turns`, PostgreSQL / ZooKeeper / Tempo 등)가 v3.1과 어긋남. 본 PR은 `--body-file`로 우회했지만, 다른 팀원 PR에서 그대로 자동 로드되면 혼란 | v3.1 양식 정렬은 팀 합의 후 별도 PR. 단기 우회: 가이드 §3.1대로 `--body-file` 사용 |
| dev `workspaces/_template.md`는 v2.x 양식, 본 PR README/workflow/report는 v3.1 가이드 양식 → 양식 불일치 | 본 PR에서 `_template.md` 미수정. v3.1 통일은 후속 PR |
| dev `BRANCHING.md`는 PR base를 `master`로, 가이드 v3.1은 `dev`로 지정 → 절차 모순 | 본 PR은 가이드 v3.1을 우선해 `--base dev`. `BRANCHING.md` 정합 작업은 후속 PR |
| dev `.github/workflows/` 에 Requirements Guard 부재 → v3.1 금지 패턴(Kafka/SSE/RAG/개역개정 등) 자동 차단 불가 | 가이드 §0 본인 우선 액션 3번. v3.1 룰셋 Guard 워크플로우 추가 별도 PR |
| `.gitignore`에 `bin/`, `.vscode/` 미등재 → 팀원 작업 시 IDE/Gradle 산출물이 untracked로 떠 노이즈 | 별도 PR로 `.gitignore` 보강 |
| 백그라운드 AI 도구(claude × 18, Codex × 6, cloudcode_cli × 1) 다수 세션이 동시 작업 → 의도하지 않은 파일 자동 수정 빈도 잦음 (CODEOWNERS / BRANCHING.md 등 사례) | 작업 시 `git add` 시 의도된 경로만 명시. 빈도 잦으면 일시 중단 |
| dev 자체가 v2.x 구조(`services/` 6개 + ChromaDB / Kafka / K8s / Helm / SSE 명시)라 v3.1과 광범위 충돌 | Lead 단독 결정 보류. 각 팀원이 본인 영역 작업하면서 점진 정합 (사용자 결정 2026-05-18) |
| `workspaces/Lead_강태오/` 새 파일은 LF, dev 기존 파일 일부 CRLF → 추후 줄바꿈 차분 발생 가능 | 별도 PR로 `.gitattributes` 도입 검토 |

---

## 6. 다음 작업

| 우선순위 | 항목 |
| --- | --- |
| 1 | PR #8 머지 여부 결정 — Lead 본인 사람 리뷰로 APPROVE override 후 squash merge, 또는 PR 열린 상태로 보존 (사용자 결정 2026-05-18: 본 정정 commit 후 PR은 그대로 둠) |
| 2 | Claude PR Auto Review 룰을 "워크스페이스/문서 변경은 테스트 면제" 조건으로 보강 — 별도 PR (`.github/workflows/claude-pr-review.yml`) |
| 3 | Requirements Guard 워크플로우 추가 — v3.1 금지 패턴 자동 차단 (Kafka / SSE / RAG / 개역개정 / ESV / NIV / 교회 인증 / AI 자유 챗봇 / AI 찬양 추천 / 가사·음원 / youtubeUrl) |
| 4 | `.gitignore` 보강 (`bin/`, `.vscode/`) — 별도 PR |
| 5 | `_template.md`, `BRANCHING.md`, `README.md`, `CLAUDE.md`, `AGENTS.md`, `DECISIONS.md` v3.1 정합 — 팀 합의 후 별도 PR (현재는 각 팀원이 본인 영역 작업하며 점진 정합) |
| 6 | 팀원 PR 올라오기 시작하면 가이드 §0 우선 액션 4번(API · 화면 · ERD 충돌 우선 확인) 수행 |
| 7 | (W1 Day2 이후) 도메인 패키지 경계 준비 → DB / API 계약 정리 → 품질 게이트 자동화 → Foundation 5/5 판정 |

---

## 7. 참고

- 본인 가이드: `2nd-Team-Project/개발자별_일정표/Lead_강태오_실행가이드.html`
- 공식 일정표: `2nd-Team-Project/개발자별_일정표/Lead_강태오_공식일정표.md`
- 공통 워크플로우 규칙: `2nd-Team-Project/개발자별_일정표/00_공통_브랜치_PR_워크플로우_규칙.md`
- W1 Day1 DoD: "`dev` 기준 PR 흐름과 기본 빌드 위치가 문서와 맞다" + "금지 기준 위반 0건" + "PR 본문에 검증 명령과 결과 기록"
