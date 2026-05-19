# Report — 2026-05-18 infra-quality-gates-add

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강태오 (@Tae0072) |
| 브랜치 | `chore/infra-quality-gates-add` → `dev` |
| PR 링크 | [#11 chore(ci): add gitleaks + spectral-lint + docker-compose-config gates](https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/11) |
| 머지 commit | `21fbfb85733689881a36f4675447d87f395eaf75` (squash merge) |
| 머지 봇 | `github-actions[bot]` (자동 머지 정책 첫 실전 테스트 성공) |
| 머지 시각 | 2026-05-18 15:20:36 UTC |
| 작업 소요 | PR 생성 14:42 → 머지 15:20 (약 38분, 4 commit 반복 수정 포함) |
| 관련 workflow | [2026-05-18_infra-quality-gates-add.md](../workflows/2026-05-18_infra-quality-gates-add.md) |

## 작업 결과 요약

W0 PR 자동화 틀 마련 후 누락된 메모리 25 PR 자동 게이트 1, 2, 5번을 ci.yml에 추가하는 PR. 작업 진행 중 W0 master 직접 push의 부작용으로 발생한 ci.yml + claude-pr-review.yml의 4가지 버그를 발견·수정하여, **PR 자동화 인프라 v3.1 + 자동 머지 정책의 첫 실전 테스트 성공**까지 달성.

## 4 commit 흐름

| commit | 목적 | 결과 |
| --- | --- | --- |
| `fbca1e6` | 누락 게이트 3개 추가 (gitleaks / spectral-lint / docker-compose-config) | 새 게이트 3 success, 기존 4 failure |
| `16a44c3` | spring-build / flutter-test working-directory + requirements-guard 12번 fix | 7 게이트 중 6 success, claude-review만 failure |
| `11e5fb0` | claude-pr-review.yml PROMPT bash escape (env로 export) | claude-review 실행은 됐으나 in_progress 지속 |
| `9daaf3f` | Auto Merge polling self-deadlock fix (THIS_JOB_NAME) | **7/7 success → 자동 squash merge** |

머지 시 squash로 1 commit으로 압축: `21fbfb8 chore(ci): add gitleaks + spectral-lint + docker-compose-config gates (#11)`.

## 발견된 4가지 버그 + 원인 + 해결

### 1. spring-build / flutter-test의 `defaults.run.working-directory` 미존재 폴더 cd 실패

**원인:** W0 commit 2의 ci.yml에서 `working-directory: qtai-server` / `flutter-app` 설정인데 두 폴더 모두 미생성 (메모리 24의 첫 6 PR 중 3번 PR 이전 단계).

**현상:** `cd qtai-server` 시도 → `No such file or directory` → 첫 `run` step부터 실패. `continue-on-error: true`는 한 step만 무시하므로 다음 step도 같은 에러 → job failure.

**해결:**
- `defaults.run.working-directory` 제거
- 각 step에 `[ -d <dir> ] && [ -f <dir>/<file> ] && cd <dir> && ...` 명시적 가드
- `Set up JDK 21` / `Set up Flutter`도 `if: hashFiles('<dir>/<file>') != ''` 조건부 실행

### 2. requirements-guard 12번 규칙의 self-inflicted bug

**원인:** W0 commit 2의 `reports/2026-05-18_pr-automation-setup_report.md` 라인 103에서 ci.yml 12번 규칙을 설명하면서 금지 표현 3개를 따옴표로 인용했다. 이 reports/.md 파일은 `--exclude-dir=docs`에만 면제되어 있고 `workspaces/` 폴더는 검사 대상이라, grep이 검출 → BLOCK + FAILED=1 → exit 1.

**해결:** 12번 규칙 grep에 `--exclude-dir=workspaces` 추가. README "workspaces 빌드·런타임·테스트·CI에 영향 0" 정책에 부합.

### 3. claude-pr-review.yml의 PROMPT bash syntax error (line 127)

**원인:** PROMPT 안에 GitHub Actions context 변수를 직접 박아넣었다. GitHub Actions가 yaml 단계에서 `${{ steps.get_diff.outputs.diff_content }}`를 raw text로 치환하면서 PR diff 내용 (ci.yml 코드 다수 포함)이 bash 스크립트 본문에 그대로 들어감. bash는 큰따옴표 안에서도 `$(...)`, backtick, `$VAR`를 해석하므로, PR diff의 `count=$(find apis ...)` 같은 패턴에서 line 127 syntax error.

**현상 (이미지 로그):**
```
/home/runner/work/_temp/.../*.sh: line 127: syntax error near unexpected token `('
Error: Process completed with exit code 2.
```

**해결:** GitHub Actions context 변수를 `env`로 export 후 bash 변수로 참조 — 표준 escape 패턴.

```yaml
env:
  PR_DIFF: ${{ steps.get_diff.outputs.diff_content }}
  DIFF_MAX_BYTES_INT: ${{ env.DIFF_MAX_BYTES }}
run: |
  PROMPT="...
  ## PR Diff (최대 ${DIFF_MAX_BYTES_INT} bytes)
  ${PR_DIFF}"
```

bash가 변수 한 번만 expand하고 내용 안의 특수문자는 재해석 안 함.

### 4. Auto Merge polling의 self-deadlock

**원인:** claude-pr-review.yml의 `actions/github-script@v7` 코드에서 자기 자신을 필터링하는 변수가 잘못 지정됨:

```js
const THIS_JOB_NAME = 'Claude PR Auto Review';  // workflow name (X)
const ciChecks = check_runs.filter(r => r.name !== THIS_JOB_NAME);
```

GitHub Actions API의 `check_runs.listForRef`는 각 check의 **job name**을 반환하지 (workflow name 아님). 실제 job key는 `claude-review`이고, yaml에 job display name이 명시 안 되어 있으니 check name = `claude-review`. `THIS_JOB_NAME = 'Claude PR Auto Review'`와 불일치.

**현상:** filter에서 자기 자신이 빠지지 않음 → ciChecks에 자기 자신 포함 → 자기 자신은 항상 `status: in_progress` → pending 리스트에 자기 포함 → 영원히 polling → 10분 timeout.

**해결:** `THIS_JOB_NAME = 'claude-review'`로 변경.

## CI 결과 (commit별)

| Check | fbca1e6 | 16a44c3 | 11e5fb0 | **9daaf3f (최종)** |
| --- | :---: | :---: | :---: | :---: |
| qtai-server Build & Test | ❌ | ✅ | ✅ | ✅ |
| Flutter Analyze & Test | ❌ | ✅ | ✅ | ✅ |
| Requirements Guard (v3.1) | ❌ | ✅ | ✅ | ✅ |
| Gitleaks Secret Scan | ✅ | ✅ | ✅ | ✅ |
| OpenAPI Spectral Lint | ✅ | ✅ | ✅ | ✅ |
| Docker Compose Config Validation | ✅ | ✅ | ✅ | ✅ |
| claude-review | ❌ (12s, syntax error) | ❌ (12s, syntax error) | 🔄 (in_progress, self-deadlock) | ✅ (84s) |
| **자동 머지** | — | — | — | **✅ github-actions[bot]** |

## 자동 머지 정책 첫 실전 테스트 — 검증된 동작 흐름

다음 흐름이 정상 작동함을 확인:

1. PR 생성 / commit push 시 `pull_request` 이벤트 트리거
2. `ci.yml` workflow 자동 실행 → 6개 CI 게이트 병렬 검사
3. `claude-pr-review.yml` workflow 자동 실행 → Claude Opus 4.7 모델이 1MB diff 분석 (84초)
4. Claude 리뷰 결과의 마지막 줄 = `APPROVE` 또는 `REQUEST_CHANGES`
5. **APPROVE 시:** PR에 review 등록 (event=APPROVE) → CI 6 게이트 polling (15초 간격, 최대 10분) → 모두 success 확인 → `github.rest.pulls.merge({ merge_method: 'squash' })` 자동 호출 → dev로 squash merge
6. **REQUEST_CHANGES 시:** PR에 review 등록 후 머지 안 함 (작성자가 수정 commit push 시 재실행)
7. **CI 일부 fail 시:** "자동 머지 취소" 코멘트 + 머지 안 함

## 검증된 7개 game

| 게이트 | 상태 | 메모리 25 번호 |
| --- | :---: | :---: |
| spring-build (qtai-server Build & Test) | ✅ 디렉터리 가드 동작 | (기본) |
| flutter-test (Flutter Analyze & Test) | ✅ 디렉터리 가드 동작 | (기본) |
| requirements-guard (v3.1 금지 패턴) | ✅ 15개 패턴 검사, workspaces 면제 | 6번 |
| gitleaks (Secret 누출 검사) | ✅ 전체 히스토리 스캔, 누출 없음 확인 | 1번 |
| spectral-lint (OpenAPI Spectral) | ✅ apis/ skip (디렉터리 가드) | 2번 |
| docker-compose-config | ✅ compose 파일 skip (디렉터리 가드) | 5번 |
| claude-review (Claude PR 자동 리뷰) | ✅ Opus 4.7, 9개 기준, 자동 머지 polling | 8번 |

**메모리 25의 PR 자동 게이트 8종이 실전 운영 가능 상태.**

## 남은 리스크 / 후속 PR

1. **`apis/` 디렉터리 미생성**으로 spectral-lint는 skip 상태 — 메모리 24 첫 6 PR 중 6번 `docs(api): add initial api-v1 openapi contract` 머지 후 실증 검증 필요
2. **`docker-compose.yml` 미생성**으로 docker-compose-config도 skip 상태 — v1 Docker Compose 부트스트랩 PR 이후 실증 검증 필요
3. **`.spectral.yaml` 미정의** — 현재 Spectral 기본 OpenAPI ruleset 사용
4. **PR-B에 4 commit 누적 (점진적 수정)** — 후속 PR은 사전 검증 강화로 1 commit 시도
5. **메모리 23 (09_Git_규칙)의 dev/master 직접 push 금지** — W0 master 직접 push 예외 1회 + dev hard reset 1회 적용. 메모리 23 본문은 유지, 예외 적용 사실은 본 report에 기록
6. **자동 머지 정책 인지 부족 시 팀원 혼란 가능** — pull_request_template.md에 "자동 머지 안내" 섹션 명시. W1 첫 미팅에서 공지 필요

## 다음 작업

### 즉시
- 본 report 머지 후 메모리 23 갱신 검토 (W0 예외 적용 + 자동 머지 정책 첫 테스트 성공 기록)

### W1 정상 작업 시작
1. 메모리 24 첫 6 PR 순서 3번: `chore(server): scaffold modular monolith packages` — qtai-server 13 도메인 패키지 골격 생성 (Lead)
2. 메모리 24 첫 6 PR 순서 4번: `test(server): add domain boundary guards` — ArchUnit + Spring Modulith 도메인 경계 검사 (Lead)
3. 메모리 24 첫 6 PR 순서 5번: `docs(data): add bible source review table` — 88.json (KRV) + KJV.json 출처 검토 (이지윤)
4. 메모리 24 첫 6 PR 순서 6번: `docs(api): add initial api-v1 openapi contract` — apis/ 디렉터리 + OpenAPI 초안 (Lead/김지민) → 자동으로 spectral-lint 실증 검증 작동

각 PR이 자동 머지 정책에 진입하면서 PR 자동화 인프라가 실전 운영 단계로 전환.