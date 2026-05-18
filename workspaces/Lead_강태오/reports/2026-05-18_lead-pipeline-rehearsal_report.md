# Report — 2026-05-18 lead-pipeline-rehearsal

| 항목 | 내용 |
| --- | --- |
| 담당자 | Lead_강태오 |
| 브랜치 | `chore/infra-workspaces-bootstrap` |
| PR 대상 | `dev` |
| PR 링크 | (PR 생성 후 보강) |
| 커밋 | (commit 후 보강) |
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

## 3. PR에 포함하지 않은 변경 (사실 보고)

작업 중 working tree에 본 PR 의도와 무관한 변경이 두 번 자동으로 발생했다.

| 시점 | 파일 | 내용 | 처리 |
| --- | --- | --- | --- |
| `git status` 1차 시점 | `.github/workflows/ci.yml` | `services/*/build.gradle.kts` 와 `apps/mobile/pubspec.yaml` 부재 시 빌드 step 을 skip 하는 가드 추가 (32+/14-) | 본 PR에 포함 안 함. 이후 working tree에서 자동 사라짐 (다른 도구가 restore) |
| `git add` 후 시점 | `.github/CODEOWNERS` | `services/bible-service/` Owner를 `@xogurrh012`(김태혁)로 활성화 (2+/2-) | 본 PR에 포함 안 함. unstaged 상태 유지 |

추정 원인: 작업 중 백그라운드에 다음 프로세스가 떠 있었다.

- `claude` × 18 (가장 오래된 것 09:00, 가장 최근 15:53)
- `Codex` / `codex` × 6 (09:13 시작 다수, 일부 15:21)
- `cloudcode_cli` × 1 (15:21)

이 중 하나가 background 작업으로 파일을 자동 수정했을 가능성이 매우 높다. 본 PR과 무관한 변경이라 `git add workspaces/Lead_강태오/` 로 의도된 영역만 staging 했다.

후속 권장:

- 이 자동 변경의 출처를 명확히 식별하고, 정상 작업이면 별도 PR로 분리 / 잘못된 변경이면 `git restore .github/CODEOWNERS` 로 되돌리기
- 백그라운드 도구가 git 상태를 흔드는 빈도가 잦으면 작업 중 해당 도구를 일시 중단

---

## 4. CI / 자동 리뷰 결과 (PR 생성 후 보강)

| 검사 | 기대 | 실제 결과 |
| --- | --- | --- |
| `Spring Boot Build & Test` (matrix: gateway, bff-aggregator, bible-service, ai-service) | dev `services/*` 그대로 사용. `workspaces/` 변경뿐이라 빌드 영향 없음 → 통과 또는 기존 dev와 동일 결과 | (관찰 후 기입) |
| `Flutter Test` | `apps/mobile/` 미변경 → 통과 또는 기존 dev와 동일 결과 | (관찰 후 기입) |
| `Decisions Guard` (dev ci.yml에 존재) | `workspaces/` 변경은 DECISIONS.md 기준 검사 대상 X → 통과 기대 | (관찰 후 기입) |
| Claude PR Auto Review | `workspaces/Lead_강태오/` 신규 문서 추가. 코드 변경 없음 → APPROVE 기대. 단 본문 안의 v3.1 기준 인용을 v2.x 기준(DECISIONS.md)과 혼동해 REQUEST_CHANGES 가능성도 있음 | (관찰 후 기입) |
| Requirements Guard | dev `.github/workflows/` 에 부재 확인 → 본 PR에서 동작 안 함 | 해당 없음 |
| 자동 머지 | `claude-pr-review.yml`이 APPROVE 시 squash merge 시도 | (관찰 후 기입) |

---

## 5. 남은 리스크

| 리스크 | 후속 대응 |
| --- | --- |
| dev `.github/pull_request_template.md`가 v2.x 기준이라 PR 본문 자동 로드 체크리스트(Kafka envelope `data`, SSE `/turns`, PostgreSQL / ZooKeeper / Tempo 등)가 v3.1과 어긋남 | 본 PR에서는 PR 본문에 별도 자체 설명 작성. v3.1 정렬은 팀 합의 후 별도 PR |
| dev `workspaces/_template.md`는 v2.x 양식, 본 PR README/workflow/report는 v3.1 가이드 양식 → 양식 불일치 | 본 PR에서 `_template.md` 미수정. v3.1 통일은 후속 PR |
| dev `BRANCHING.md`는 PR base를 `master`로, 가이드 v3.1은 `dev`로 지정 → 절차 모순 | 본 PR은 가이드 v3.1을 우선해 `--base dev`. `BRANCHING.md` 정합 작업은 후속 PR |
| 백그라운드 AI 도구(claude × 18, Codex × 6, cloudcode_cli × 1)가 파일을 자동 수정 → 작업 중 안정성 저하 | 작업 시 백그라운드 작업 인지하고 `git add` 시 의도된 경로만 명시. 빈도 잦으면 일시 중단 |
| dev 자체가 v2.x 구조(`services/` 6개 + ChromaDB / Kafka / K8s / Helm / SSE 명시)라 v3.1과 광범위 충돌 | Lead 단독 결정 보류. 각 팀원이 본인 영역 작업하면서 점진 정합 (사용자 결정 2026-05-18) |
| `workspaces/Lead_강태오/` 새 파일은 LF, dev 기존 파일 일부 CRLF → 추후 줄바꿈 차분 발생 가능 | 별도 PR로 `.gitattributes` 도입 검토 |

---

## 6. 다음 작업

| 우선순위 | 항목 |
| --- | --- |
| 1 | 본 PR의 CI / 자동 리뷰 결과 관찰 → 본 report §4에 보강 |
| 2 | 자동화 결과에 따라 후속 조정 (예: PR 템플릿 정합, Requirements Guard 도입 여부, 자동 머지 정책 확인) |
| 3 | 팀원 PR이 올라오기 시작하면 가이드 §0 우선 액션 4번(API · 화면 · ERD 충돌 우선 확인) 수행 |
| 4 | (W1 Day2 이후) 가이드 일정상 도메인 패키지 경계 준비 → DB / API 계약 정리 → 품질 게이트 자동화 → Foundation 5/5 판정 |

---

## 7. 참고

- 본인 가이드: `2nd-Team-Project/개발자별_일정표/Lead_강태오_실행가이드.html`
- 공식 일정표: `2nd-Team-Project/개발자별_일정표/Lead_강태오_공식일정표.md`
- 공통 워크플로우 규칙: `2nd-Team-Project/개발자별_일정표/00_공통_브랜치_PR_워크플로우_규칙.md`
- W1 Day1 DoD: "`dev` 기준 PR 흐름과 기본 빌드 위치가 문서와 맞다" + "금지 기준 위반 0건" + "PR 본문에 검증 명령과 결과 기록"
