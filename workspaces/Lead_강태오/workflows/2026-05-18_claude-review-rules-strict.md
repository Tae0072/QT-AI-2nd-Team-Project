# Workflow — 2026-05-18 claude-review-rules-strict

| 항목 | 내용 |
| --- | --- |
| 담당자 | Lead_강태오 |
| 브랜치 | `chore/infra-claude-review-rules` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 — PR 자동화 인프라 강화 |
| 기준 문서 | `Lead_강태오_실행가이드.html` §3 (PR 가이드), §6 (워크플로우/리포트), §7 (검증 명령), §8 (금지 패턴), `00_공통_브랜치_PR_워크플로우_규칙.md` §3 |
| 담당 경로 | `.github/workflows/claude-pr-review.yml`, `workspaces/Lead_강태오/` |

---

## 1. 작업 목표

`.github/workflows/claude-pr-review.yml`의 Claude 프롬프트를 **9개 자동 머지 게이트**로 갈아엎는다. 9개를 모두 통과해야 APPROVE → 자동 squash merge가 되고, 하나라도 실패하면 REQUEST_CHANGES로 차단된다.

PR #8에서 확인된 문제: 기존 프롬프트가 "docs/chore 타입은 테스트 불필요"를 명시했으나 Claude가 `chore`를 `feat`로 재해석해서 REQUEST_CHANGES를 냈다. 또한 가이드의 다른 의무사항(브랜치 규칙·소유권·역할 경계·워크플로우/리포트·금지 패턴·검증 명령)은 룰에 들어 있지 않았다.

본 PR은 사용자 의도(2026-05-18)에 따라 가이드의 모든 의무사항을 룰화한다.

---

## 2. 9개 자동 머지 게이트

| # | 게이트 | 검사 방식 |
| --- | --- | --- |
| 1 | 브랜치 규칙 | `{type}/{scope}-{short-task}` 형식. type은 feature/bugfix/refactor/test/docs/chore/hotfix |
| 2 | 소유권 (CODEOWNERS) | 변경된 모든 경로가 PR 작성자 소유 영역인가 |
| 3 | 역할 / 도메인 경계 | 본인 담당 도메인 밖 침범 0건 |
| 4 | 단위 테스트 | feat/fix/refactor면 단위 테스트가 PR diff에 추가/수정. docs/chore/test 면제 |
| 5 | 통합 테스트 | feature는 통합 테스트 또는 PR 본문에 미포함 사유 명시 |
| 6 | 테스트 실행 (CI 통과) | Claude가 직접 검사 X. 자동 머지 직전 CI polling이 검증 (기존 로직 유지) |
| 7 | 워크플로우 + 리포트 | `workspaces/{담당자}/workflows/YYYY-MM-DD_*.md` + `reports/.../*_report.md` PR diff에 포함 + PR 본문에 경로 명시 |
| 8 | 금지 패턴 | Kafka/SSE/RAG/ChromaDB/개역개정/ESV/NIV/교회 인증/AI 자유 챗봇/AI 찬양 추천/가사·음원/youtubeUrl. 단 "금지를 설명하는 문서성 파일"은 면제 |
| 9 | 검증 명령 | PR 본문에 실제 실행 명령과 결과(PASS/FAIL) 명시 |

추가 코드 품질 코멘트(가독성·NPE·`@Transactional`·deprecated API 등)는 코멘트로만 남기고 REQUEST_CHANGES 사유로 사용 X.

---

## 3. 작업 순서

1. workflow 작성 (이 파일)
2. `.github/workflows/claude-pr-review.yml` 수정
   - `Get PR diff` step 다음에 `Collect PR context` step 추가 → `pr_meta.txt`에 PR title/author/branch/body 저장
   - `Claude PR Review` step의 프롬프트를 9개 게이트로 갈아엎기
   - `Post Review & Auto Merge` step은 그대로 유지 (APPROVE 시 자동 머지 로직)
3. 로컬 검증
   - `git diff --check`
   - yml 문법 검사 — `ruamel` 또는 simple yaml load
   - 금지 패턴 grep (변경 파일 한정)
4. report 작성
5. commit + push
6. PR 생성
7. **이 PR은 옛 룰로 검사되니 Lead 본인 사람 리뷰 APPROVE로 override** → squash merge
8. 머지 후 PR #8에 empty commit push → 새 룰로 재리뷰 → 자동 머지 확인

---

## 4. 검증 계획

| 축 | 명령 | 통과 기준 |
| --- | --- | --- |
| Whitespace | `git diff --check` | 출력 없음 |
| yml 문법 | PowerShell `ConvertFrom-Yaml` 또는 GitHub Actions에서 자동 검증 | 파싱 성공 |
| 변경 범위 | `git status --short` | `.github/workflows/claude-pr-review.yml` + `workspaces/Lead_강태오/` 한정 |
| 금지 패턴 grep | 변경 파일 한정 | 매치 0건 (단 "금지를 설명하는 문서"는 면제) |
| 자동화 (PR 생성 후) | CI 9개 체크 + Claude 리뷰 (옛 룰로 동작) | 사람 리뷰 APPROVE override 필요 예상 |

---

## 5. 예상 리스크

| 리스크 | 대응 |
| --- | --- |
| 새 룰이 너무 엄격해서 정상 PR도 다 막힘 | 본 PR 머지 후 PR #8 재트리거로 검증. 문제 발견 시 후속 PR로 룰 보완 |
| 본 PR 자체가 옛 룰로 검사되어 REQUEST_CHANGES | Lead 본인 사람 리뷰 APPROVE로 override |
| 새 프롬프트가 길어 Claude CLI timeout 늘 수 있음 | 기존 `timeout_minutes: 10` 유지. 문제 시 늘림 |
| 다른 팀원이 익숙해지기 전까지 PR이 자주 막힘 | 가이드(`Lead_강태오_실행가이드.html`) 절차 준수하면 9개 게이트 모두 자동 통과. 본인이 팀에 공지 권장 |
| PR 본문이 yml 환경변수로 전달될 때 특수문자(`\``, `"`, `$`) escape 문제 | `env:` 블록 + `"$PR_BODY"`로 안전 전달 |
| `Collect PR context` step에서 PR body가 빈 경우 | `pr_meta.txt`가 비어도 Claude는 PR title/branch로 판정 가능. 단 게이트 7/9는 PR body 필수라 빈 본문은 REQUEST_CHANGES |

---

## 6. 참고

- 기존 yml: `.github/workflows/claude-pr-review.yml` (현 dev HEAD `28416b9`)
- PR #8 (옛 룰로 REQUEST_CHANGES): https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/8
- 가이드: `2nd-Team-Project/개발자별_일정표/Lead_강태오_실행가이드.html`
- 13도메인 ↔ 팀원 매핑은 `qt_ai_domain.md` 메모리 참고
