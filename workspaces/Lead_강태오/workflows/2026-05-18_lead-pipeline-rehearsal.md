# Workflow — 2026-05-18 lead-pipeline-rehearsal

| 항목 | 내용 |
| --- | --- |
| 담당자 | Lead_강태오 |
| 브랜치 | `chore/infra-workspaces-bootstrap` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 — Lead 워크플로우 의무 자기참조 시작점 |
| 기준 문서 | `07_요구사항_정의서.md` v3.1, `Lead_강태오_공식일정표.md` v3.1-align.1, `00_공통_브랜치_PR_워크플로우_규칙.md` v0.1 |
| 담당 경로 | `workspaces/Lead_강태오/` |

---

## 1. 작업 목표

오늘(2026-05-18, W1 Day1) Lead 본인 첫 산출물로 `workspaces/Lead_강태오/`에 README와 자기참조 workflow/report를 추가한다.

이 PR의 실질 목적은 `Lead_강태오_실행가이드.html`의 **모든 의무 절차**(작업 전 workflow → 구현 → 로컬 검증 → 작업 후 report → push → PR 생성 → CI / 자동 리뷰 → 머지)를 실제로 한 번 끝까지 돌려보면서 **PR 자동화 파이프라인이 의도대로 동작하는지 확인**하는 것이다.

작은 변경으로 시작하는 이유:

- 변경 범위가 크면 자동화 실패 원인을 좁히기 어렵다
- `workspaces/Lead_강태오/`는 빌드·런타임·테스트·CI에 영향 0 (`workspaces/README.md` §2)
- 다른 팀원 영역을 침범하지 않는다 (`workspaces/README.md` §1, 4)
- v3.1 양식을 첫 번째로 실제 파일에 박아둠으로써 이후 본인 작업의 기준점이 된다

---

## 2. 작업 순서

1. **workflow 작성 (이 파일)** — 작업 시작 직전 작성한다 (가이드 §6 의무)
2. `workspaces/Lead_강태오/README.md` 작성 — Lead 역할, 담당 도메인, 대표 branch scope, 폴더 구조, 흔들리지 않는 본인 기준, 매일 작업 순서
3. 로컬 검증 (§3 참고)
4. report 작성 — 변경 내용, 검증 결과, 남은 리스크, 다음 작업 (가이드 §6 의무)
5. `git add workspaces/Lead_강태오/` — 변경 범위를 본인 폴더로 한정
6. Conventional Commits 메시지로 commit
7. `git push -u origin chore/infra-workspaces-bootstrap`
8. `gh pr create --base dev --head chore/infra-workspaces-bootstrap` 으로 PR 생성. 본문에 workflow / report 경로 명시
9. GitHub Actions, (있다면) Requirements Guard, Claude PR Auto Review 결과 관찰
10. 결과를 report 끝의 "CI / 자동 리뷰 결과"에 보강

---

## 3. 검증 계획

| 축 | 명령 / 확인 | 통과 기준 |
| --- | --- | --- |
| Whitespace | `git diff --check` | 출력 없음 |
| 금지 패턴 (`Lead_강태오_실행가이드.html` §8) | `rg -n "Kafka\|SseEmitter\|/ai/sessions\|RAG\|ChromaDB\|Elasticsearch\|개역개정\|ESV\|NIV\|church verification\|churchAuth\|church_cert\|youtubeUrl\|recommend.*song" workspaces/Lead_강태오/` | 매치 0건 (단, "Kafka 금지", "SSE 금지"처럼 **금지를 설명하는 문구**는 허용. `qt_ai_forbidden` 메모리·`18` 게이트 §예외 기준) |
| 변경 범위 한정 | `git status --short` 결과가 `workspaces/Lead_강태오/` 안에만 있음 | 외부 파일 변경 0건 |
| 줄바꿈 (저장소에 `.gitattributes` 없음) | 추가 파일이 LF로 저장됨 (git이 자동 normalize하지 않음) | 추후 줄바꿈 차분 발생 시 별도 PR로 `.gitattributes` 도입 검토 |
| 자동화 (PR 생성 후) | GitHub Actions 4서비스 매트릭스 빌드, Claude PR Auto Review | 통과 또는 실패 사유를 report에 사실 기록 |

검증 명령은 사용자(강태오)가 Windows PowerShell에서 직접 실행한다. 일부 명령은 Claude 측 샌드박스 권한 제약상 호스트에서만 가능하다.

---

## 4. 예상 리스크

| 리스크 | 영향 | 대응 |
| --- | --- | --- |
| dev `.github/pull_request_template.md`가 v2.x 기준 (SSE `/turns`, Kafka envelope `data`, PostgreSQL/ZooKeeper/Tempo 금지만 명시)이라 PR 본문에 v3.1과 어긋난 체크리스트가 자동 로드된다 | PR 본문에 무관한 체크리스트 포함 | 본 PR에서는 템플릿을 수정하지 않는다. PR 본문에 별도 자체 설명을 두고, v2 체크리스트는 "해당 없음"으로 비워둔다 |
| dev `workspaces/_template.md`가 v2.x 양식(`DECISIONS.md` 충돌 체크 + Envelope `data` 키 + PostgreSQL/ZooKeeper/Tempo)이지만 본 PR은 v3.1 가이드 양식 사용 | 양식 정합 불일치 | `_template.md`는 본 PR에서 건드리지 않는다. v3.1 양식으로의 정렬은 별도 PR 또는 팀 합의 후 진행 |
| CI 4서비스 매트릭스 빌드가 `services/*` 변경 없이도 매번 실행되어 시간 소요 큼 | 자동화 결과 확인 시간 길어짐 | 결과 기다림. 실패 시 원인 분석해 report에 기록 |
| Claude PR Auto Review가 `workspaces/` 변경에 대해 코드 리뷰 룰을 잘못 적용 (DECISIONS.md 충돌 등 v2 기준)할 가능성 | 잘못된 REQUEST_CHANGES | 리뷰 코멘트를 그대로 report에 기록. 사람 리뷰로 override 가능 여부 확인 |
| Requirements Guard / Document Guard 워크플로우가 dev에 아직 존재하지 않을 수 있음 | 가이드의 "Requirements Guard" 단계 부재 | 사실 그대로 report에 기록. 후속 PR에서 가드 도입 여부 결정 |
| dev `BRANCHING.md`는 PR base가 `master`라고 명시하지만 가이드 v3.1은 `dev`로 지정 | base 선택 혼란 | 가이드 v3.1을 우선해 `--base dev`로 PR 생성. 충돌 시 report에 기록 |
| 줄바꿈(LF/CRLF) 차이로 인한 의도하지 않은 diff 발생 가능 | 리뷰가 의미 없는 변경을 봐야 함 | `git diff --check`로 사전 확인. 발생 시 별도 `.gitattributes` PR로 분리 |

---

## 5. 참고

- 본인 가이드: `2nd-Team-Project/개발자별_일정표/Lead_강태오_실행가이드.html`
- 본인 공식 일정표: `2nd-Team-Project/개발자별_일정표/Lead_강태오_공식일정표.md`
- 공통 워크플로우 규칙: `2nd-Team-Project/개발자별_일정표/00_공통_브랜치_PR_워크플로우_규칙.md`
- 주차 일정 SSoT: `2nd-Team-Project/00_개발_일정_총괄표.md` v0.1
- W1 Day1 DoD: "`dev` 기준 PR 흐름과 기본 빌드 위치가 문서와 맞다" + "금지 기준 위반 0건" + "PR 본문에 검증 명령과 결과 기록"
