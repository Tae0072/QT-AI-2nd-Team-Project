# Report — 2026-05-18 claude-review-rules-strict

| 항목 | 내용 |
| --- | --- |
| 담당자 | Lead_강태오 |
| 브랜치 | `chore/infra-claude-review-rules` |
| PR 대상 | `dev` |
| PR 링크 | (PR 생성 후 보강) |
| 커밋 | (commit 후 보강) |
| Workflow | `workspaces/Lead_강태오/workflows/2026-05-18_claude-review-rules-strict.md` |

---

## 1. 변경 내용

| 분류 | 파일 | 변경 |
| --- | --- | --- |
| 수정 | `.github/workflows/claude-pr-review.yml` | +119/-23 — Claude 프롬프트를 9개 자동 머지 게이트로 재작성. PR meta(title/author/branch/body)와 변경 파일 목록을 별도 step으로 수집해 Claude에 함께 전달 |
| 신규 | `workspaces/Lead_강태오/workflows/2026-05-18_claude-review-rules-strict.md` | 사전 workflow |
| 신규 | `workspaces/Lead_강태오/reports/2026-05-18_claude-review-rules-strict_report.md` | 이 파일 |

`.github/CODEOWNERS`, `BRANCHING.md` 워킹 트리 modified는 본 PR 무관(다른 자동 도구 작업)이라 staging 제외.

---

## 2. 9개 자동 머지 게이트 (요약)

| # | 게이트 | 핵심 검사 |
| --- | --- | --- |
| 1 | 브랜치 규칙 | `{type}/{scope}-{short-task}` 형식 + type 7종 안 |
| 2 | 소유권 (CODEOWNERS) | 변경 경로가 작성자 영역. Lead는 모든 영역 가능 |
| 3 | 역할 / 도메인 경계 | 다른 도메인 직접 import 0건, 타인 워크스페이스 0건 |
| 4 | 단위 테스트 | feat/fix/refactor면 단위 테스트 PR diff에 포함. docs/chore/test 면제 |
| 5 | 통합 테스트 | feature는 통합 테스트 또는 PR 본문 미포함 사유 명시 |
| 6 | 테스트 실행 (CI 통과) | Claude 직접 검사 X. 자동 머지 직전 CI polling이 별도 검증 |
| 7 | 워크플로우 + 리포트 | `workspaces/{담당자}/workflows/*` + `reports/*_report.md` 둘 다 PR diff에 + PR 본문 경로 명시 |
| 8 | 금지 패턴 | Kafka/SSE/RAG/ChromaDB/개역개정·ESV·NIV/교회 인증/AI 자유 챗봇·찬양/가사·음원·youtubeUrl. "차단 기준 설명" 문서성 파일은 면제 |
| 9 | 검증 명령 | PR 본문에 실제 명령과 결과(PASS/FAIL) 명시 |

추가 코드 품질 코멘트(가독성·NPE·deprecated 등)는 코멘트로만 남기고 REQUEST_CHANGES 사유 X.

---

## 3. 검증 결과

| 축 | 명령 | 결과 |
| --- | --- | --- |
| Whitespace | `git diff --check` | ✅ 출력 없음 |
| yml 문법 (PowerShell) | `python -c "import yaml; yaml.safe_load(open('.github/workflows/claude-pr-review.yml'))"` | 🟡 호스트에 PyYAML 미설치라 검증 skip. GitHub Actions에서 yml 파싱되어 워크플로우 실행되면 통과 확인 |
| yml Read 검증 | Claude 측 Read tool로 직접 라인 단위 확인 | ✅ literal block scalar 인덴트 일관, 한글 인코딩 UTF-8 정상 |
| 변경 범위 한정 | `git status --short` | ✅ `.github/workflows/claude-pr-review.yml` + `workspaces/Lead_강태오/` 한정 (CODEOWNERS/BRANCHING.md modified는 staging 제외) |
| 금지 패턴 grep (변경 파일) | 수동 검토 | ✅ yml 안의 Gate 8 설명은 **차단 기준 설명**(문서성)이라 면제. workflow/report .md 안의 인용도 동일 |
| 줄바꿈 | Write tool로 작성된 파일은 LF | ✅ |

---

## 4. PR과 함께 보고할 자기 검증 (9개 게이트 자체 적용)

본 PR이 새 룰 기준으로도 통과하는지 자가 검사.

| Gate | 결과 | 비고 |
| --- | --- | --- |
| 1. 브랜치 규칙 | ✅ | `chore/infra-claude-review-rules` — type=chore, scope=infra, task=claude-review-rules |
| 2. 소유권 | ✅ | 변경 경로 `.github/workflows/`, `workspaces/Lead_강태오/` 모두 Lead 영역 |
| 3. 역할 / 도메인 경계 | ✅ | 다른 도메인 import 없음, 타인 워크스페이스 0건 |
| 4. 단위 테스트 | ✅ (면제) | type=chore, 면제 대상 |
| 5. 통합 테스트 | ✅ (면제) | type=chore, 면제 대상 |
| 6. CI 통과 | (PR 생성 후 확인) | |
| 7. 워크플로우 + 리포트 | ✅ | 본 PR에 workflow와 report 둘 다 포함 + 본 본문에 경로 명시 |
| 8. 금지 패턴 | ✅ | yml 안 Gate 8 설명은 차단 기준 명시(문서성 면제) |
| 9. 검증 명령 | ✅ | §3 검증 결과 표에 명령과 결과 명시 |

**단**, 본 PR은 옛 룰(prompt가 기존 7번 기준)로 검사되므로 PR #8과 동일하게 Claude가 REQUEST_CHANGES 가능성 큼. Lead 본인 사람 리뷰 APPROVE override로 머지 예정.

---

## 5. 머지 후 시나리오

1. 본 PR 머지 → `.github/workflows/claude-pr-review.yml` 새 룰이 dev에 반영
2. PR #8(`chore/infra-workspaces-bootstrap`)에 empty commit push → synchronize 이벤트
3. claude-review가 새 룰로 PR #8 다시 검사 → 9개 게이트 자가 검사 (PR #8도 chore PR, workspaces/Lead_강태오/ 안에만 변경)
4. 모두 PASS면 APPROVE → 자동 머지 → PR #8 dev 반영

자동 머지 흐름 끝까지 동작하면 W1 Day1 PR 자동화 검증 완전 종료.

---

## 6. 남은 리스크

| 리스크 | 대응 |
| --- | --- |
| 새 룰이 너무 엄격해서 정상 PR도 막힘 (특히 Gate 7 워크플로우/리포트 의무) | PR #8 재검증 결과로 룰 보완 필요 시 후속 PR. 가이드 절차 준수하면 자동 통과 |
| Gate 8 금지 패턴 grep에서 "차단 기준 설명" 면제 룰을 Claude가 잘못 판정해 정상 문서를 차단 | Claude가 사람만큼 정확하지 않을 수 있음. 잘못 차단 시 사람 리뷰 override + 룰 보완 |
| PR 본문이 비어있거나 형식 없는 PR (외부 도구 자동 생성 PR 등) | Gate 7/9가 자동 FAIL. 의도된 동작 |
| 다른 팀원이 새 룰을 익히기 전까지 PR이 자주 막힘 | 룰 머지 후 팀에 공지. 가이드 절차 준수하면 통과 |
| Claude CLI timeout (현재 10분) — 프롬프트 길어져 응답 시간 늘 가능성 | 문제 발견 시 timeout 늘림 |
| `Collect PR context` step에서 PR body가 매우 길어 yaml env에 안 들어가는 경우 | GitHub의 일반 PR body는 65536자 이하라 안전. 초과 시 step 자체 실패 → 후속 보완 |

---

## 7. 다음 작업

| 우선순위 | 항목 |
| --- | --- |
| 1 | 본 PR 사람 리뷰 APPROVE override + squash merge (옛 룰로 REQUEST_CHANGES 예상) |
| 2 | PR #8에 empty commit push → 새 룰로 재검증 → 자동 머지 확인 |
| 3 | (결과에 따라) 룰 보완 PR — 예: 새 룰이 다른 정상 PR 잘못 차단하는 경우 |
| 4 | (별도) `.gitignore` 보강 (`bin/`, `.vscode/`) |
| 5 | (별도) `.github/CODEOWNERS`, `BRANCHING.md` 자동 prettify 변경의 출처 식별 후 정리 |
| 6 | (W1 Day2 이후) 도메인 패키지 경계 준비, 가이드 일정 진행 |

---

## 8. 참고

- workflow: `workspaces/Lead_강태오/workflows/2026-05-18_claude-review-rules-strict.md`
- report: `workspaces/Lead_강태오/reports/2026-05-18_claude-review-rules-strict_report.md`
- 본 PR 후 영향 받는 PR: #8 (https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/8)
- 가이드: `2nd-Team-Project/개발자별_일정표/Lead_강태오_실행가이드.html`
