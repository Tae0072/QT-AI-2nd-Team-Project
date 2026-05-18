# Workflow — 2026-05-18 infra-quality-gates-add

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강태오 (@Tae0072) |
| 브랜치 | `chore/infra-quality-gates-add` (dev에서 분기) |
| PR 대상 | `dev` (정상 PR 절차) |
| 관련 F-ID | 없음 — 인프라/CI 보강 |
| 기준 문서 | 18_코드_품질_게이트.md v2.4, 09_Git_규칙.md v3.1, 메모리 25 (PR 자동 게이트 8종) |
| 담당 경로 | `.github/workflows/ci.yml` |

## 작업 목표

W0 PR 자동화 틀 마련 후 누락된 게이트 3개를 ci.yml에 추가하여 메모리 25의 PR 자동 게이트 8종 중 1~7번을 완성한다.

**현재 ci.yml 게이트 (3개):**
1. `spring-build` (qtai-server Build & Test)
2. `flutter-test` (Flutter Analyze & Test)
3. `requirements-guard` (v3.1 금지 패턴)

**이번 PR로 추가 (3개):**
4. `gitleaks` (Secret 누출 검사)
5. `spectral-lint` (OpenAPI Spectral Lint)
6. `docker-compose-config` (Docker Compose 유효성 검사)

**메모리 25 8번 게이트는 Claude PR 자동 리뷰 — 이미 `.github/workflows/claude-pr-review.yml`에 있음.**

## 배경 — 누락 게이트의 영향

| 게이트 | v3.1 SSoT 근거 | 누락 시 위험 |
| --- | --- | --- |
| gitleaks | 18_코드_품질_게이트.md "Secret 노출 시 즉시 폐기·재발급" | API 키·OAuth secret·JWT private key 누출 미검출 |
| Spectral Lint | 04_API_명세서.md v1.3 (OpenAPI 스펙 SSoT) | API 계약 위반·잘못된 schema·permission 누락 미검출 |
| Docker Compose Config | 14_배포_가이드.md / v1 배포 = Docker Compose | compose 파일 syntax error·서비스 의존성 누락 미검출 |

## 작업 순서

1. ✅ dev sync 완료 (master 최신 SHA `13918135`로 ff)
2. ✅ 새 브랜치 `chore/infra-quality-gates-add` 생성 (dev에서 분기)
3. 🚧 `.github/workflows/ci.yml`에 3 게이트 job 추가
4. 🚧 workflow.md (이 파일) commit + push
5. ⏳ PR 생성: `chore/infra-quality-gates-add` → `dev`
6. ⏳ Claude PR 자동 리뷰 + CI 6 게이트 결과 확인
7. ⏳ APPROVE + CI success → 자동 squash merge → dev (**첫 자동 머지 정책 실전 테스트**)
8. ⏳ report.md 작성 (PR 머지 후)

## 각 게이트 구현 방식

### 4. gitleaks
- Action: `gitleaks/gitleaks-action@v2` (공식)
- `fetch-depth: 0` 필요 (전체 히스토리 스캔)
- 별도 설정 파일 없으면 기본 ruleset (AWS / GitHub / GCP 등 100+ 패턴)
- 향후 `.gitleaks.toml`로 커스텀 가능 (별도 PR)

### 5. Spectral Lint
- CLI: `@stoplight/spectral-cli`
- `apis/` 디렉터리에 OpenAPI 스펙 파일(.yaml/.yml/.json)이 있을 때만 실행
- 디렉터리 가드: `apis/` 없으면 skip (메모리 24 6번 PR `docs(api): add initial api-v1 openapi contract` 이전 단계 false positive 방지)
- `.spectral.yaml` 있으면 사용, 없으면 기본 OpenAPI ruleset
- `--fail-severity=warn` (warn 이상이면 실패)

### 6. Docker Compose Config
- 명령: `docker compose config --quiet`
- 파일 가드: `docker-compose.yml` / `compose.yaml` / `compose.yml` 중 하나 있을 때만 실행
- v1 Docker Compose 부트스트랩 이전 단계 false positive 방지

## 검증 계획

- PR 머지 전: GitHub Actions에서 3 새 게이트가 모두 통과하는지 확인
- 현재 작업 저장소에 `apis/`, `docker-compose.yml` 미생성 → spectral-lint와 docker-compose-config는 "skip" 메시지로 통과 예상
- gitleaks는 실제 검사 실행됨 — 기존 코드에서 secret 누출이 없어야 통과
- Claude PR 자동 리뷰는 APPROVE 예상 (인프라 변경, v3.1 정합)
- **이 PR이 자동 머지 정책의 첫 실전 테스트** — APPROVE + 6 CI success → squash merge → dev 자동

## 예상 리스크

1. **gitleaks가 기존 코드에서 false positive 검출 가능** — `.env.example`이나 더미 secret이 있으면 실패. 발견 시 `.gitleaksignore` 추가 또는 더미 값 교체
2. **spectral-lint가 디렉터리 가드로 skip되면 게이트 작동 실증 못 함** — 6번 PR(OpenAPI 추가) 이후에 실제 검증 가능
3. **docker-compose-config도 동일하게 skip** — v1 Docker Compose 부트스트랩 PR 이후 실증 가능
4. **자동 머지 정책 첫 테스트 — 예상치 못한 동작 가능성** — Claude APPROVE/REQUEST_CHANGES 판정 정확도, CI 대기 시간, 머지 권한 문제 등
5. **Spectral CLI 의존성 npm install -g 비용** — 빌드 시간 약 30초 추가

---

## 작업 후 추록 (2026-05-18 첫 CI 실행 결과 반영)

### 첫 CI 결과 (commit fbca1e6) — 7/7 중 3 success / 4 failure

- success 3개 (이번 PR로 추가한 새 게이트): gitleaks / spectral-lint / docker-compose-config
- failure 4개:
  - spring-build: defaults.run.working-directory: qtai-server — 미존재 폴더 cd 실패
  - flutter-test: defaults.run.working-directory: flutter-app — 미존재 폴더 cd 실패
  - requirements-guard: 자기 report.md의 12번 규칙 인용 라인이 BLOCK 매칭 (self-inflicted bug)
  - claude-review: 추정 CLAUDE_CODE_OAUTH_TOKEN secret 미설정 — Lead 확인 필요

### 두 번째 commit 변경 사항 (PR-B 범위 확장, ci.yml만)

1. spring-build / flutter-test: defaults.run.working-directory 제거 + 각 step에 [ -d <dir> ] && cd <dir> 가드. Set up JDK / Set up Flutter도 hashFiles() 조건부 실행
2. requirements-guard 12번: --exclude-dir=workspaces 추가 (README "workspaces 빌드·런타임·테스트·CI에 영향 0" 근거). self-inflicted bug 해결

### 미해결 — Lead 결정 사항

- claude-review failure: GitHub repo Settings → Secrets and variables → Actions에서 CLAUDE_CODE_OAUTH_TOKEN 설정 여부 확인
- 없으면 추가 후 PR 재실행 (push 또는 빈 commit으로 트리거)