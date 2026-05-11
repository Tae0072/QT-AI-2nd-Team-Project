# BRANCHING.md — QT-AI 브랜치 전략 및 커밋 규칙

> 이 문서는 QT-AI 개발자 6명이 충돌 없이 협업하기 위한 세부 귀칙을 정의합니다.

---

## 브랜치 전략

QT-AI는 간소화된 **GitHub Flow** 모델을 사용합니다.

### 영구 브랜치

| 브랜치 | 용도 | 보호 |
|--------|------|------|
| `master` | 프로덕션 배포 가능 상태 | ✅ Direct push 금지 (PR만) |
| `develop` | (선택적) W2 이후 도입 검토 | |

### 작업 브랜치 (Short-lived)

| 패턴 | 용도 | 예시 |
|------|------|------|
| `feature/{service}-{TICKET}-{kebab-case}` | 새 기능 | `feature/auth-AUTH001-jwt-filter` |
| `fix/{service}-{TICKET}-{kebab-case}` | 버그 수정 | `fix/bible-BIBLE015-redis-ttl` |
| `chore/{kebab-case}` | 설정·인프라·문서 | `chore/update-spring-boot` |
| `refactor/{service}-{kebab-case}` | 리팩토링 | `refactor/journal-event-store` |
| `docs/{kebab-case}` | 문서만 수정 | `docs/update-decisions` |

- 작업 브랜치는 `master`에서 분기하고, PR로 머지하고, 즉시 삭제합니다.
- TICKET은 GitHub Issue 번호 또는 서비스 접두어 + 일련번호 사용.

---

## 커밋 메시지 — Conventional Commits

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Type 종류

| Type | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서만 수정 |
| `style` | 코드 포맷팅 (기능 변화 없음) |
| `refactor` | 리팩토링 (기능 변화 없음) |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드·CI·의존성 등 |
| `perf` | 성능 개선 |
| `revert` | 이전 커밋 되돌림 |

### Scope

서비스 또는 영역명: `auth`, `bible`, `ai`, `journal`, `gateway`, `bff`, `mobile`, `infra`, `docs`

### 예시

```
feat(auth): JWT RS256 필터 구현

- Spring Security FilterChain에 JwtAuthenticationFilter 추가
- 토큰 검증 실패 시 RFC 7807 ProblemDetail 응답
- AUTH001 이슈 참조

Closes #12
```

```
fix(bible): Redis 캐시 TTL 누락 수정

기존 코드에서 TTL 미설정으로 캐시가 영구 저장되던 문제 수정.
DECISIONS.md 기준 24h TTL 적용.
```

```
chore(infra): docker-compose Kafka 3.8 업그레이드
```

```
docs(decisions): Refresh Token TTL 7일로 단축 검토 삭제
```

---

## PR 프로세스

```
1. 작업 시작 전
   ├─ master에서 최신 pull
   ├─ feature/... 브랜치 생성
   └─ workspaces/{본인명}/workflows/ 작성

2. 작업
   ├─ 코드 작성 + 테스트
   ├─ DECISIONS.md 기준 준수 확인
   └─ Conventional Commits 메시지 작성

3. PR 생성
   ├─ base: master, head: feature/...
   ├─ .github/pull_request_template.md 체크리스트 모두 확인
   ├─ CODEOWNERS 자동 지정 리뷰어 부여
   └─ CI 통과 대기

4. 리뷰 · 상의
   ├─ 최소 1명 승인
   └─ 수정 요청 있으면 commit 추가 (PR은 자동 업데이트)

5. 머지
   ├─ ✅ 기본: Squash and merge (커밋 히스토리 단순화)
   ├─ 자동 브랜치 삭제
   └─ workspaces/{본인명}/reports/ 작성
```

### 머지 전략

- 🟢 **Squash and merge** 기본
- 🟡 예외: 큰 기능 통합이 필요한 경우 Lead 협의 후 `Create a merge commit`
- 🔴 **Rebase merge 금지** — 히스토리 추적 복잡해짐

---

## 브랜치 보호 규칙 (Lead가 GitHub Settings에서 설정)

`master` 브랜치 보호:

- [x] Require pull request before merging
- [x] Require approvals: **1명 이상**
- [x] Dismiss stale pull request approvals when new commits are pushed
- [x] Require review from Code Owners
- [x] Require status checks to pass before merging
  - `Spring Boot Build & Test`
  - `AI Service Lint & Test`
  - `Flutter Test`
  - `Decisions Guard`
- [x] Require branches to be up to date before merging
- [x] Require conversation resolution before merging
- [x] Do not allow bypassing the above settings
- [ ] Restrict who can push to matching branches: Lead만 허용 (선택)

---

## 충돌 발생 시 해결

### master가 앞서 가서 내 PR이 뷰지가 되었을 때

```bash
git checkout master
git pull origin master
git checkout feature/your-branch
git rebase master
# 충돌 발생 시 해결 → git add → git rebase --continue
git push --force-with-lease origin feature/your-branch
```

> ⚠️ `--force-with-lease` 사용 — 절대 `--force`만 쓰지 말 것 (다른 사람 commit 날아갈 수 있음)

### 잘못 커밋한 경우

```bash
# 마지막 커밋 수정
git commit --amend

# 아직 push 안 했으면
git reset HEAD~1   # 소프트 리셋 (파일 유지)
git reset --hard HEAD~1   # 하드 리셋 (파일 유실 주의)
```

---

## 이슈 랜닝은 모두 행복하게

겪도는 속도가 다르더라도 서로를 존중하고 배려하면 6주가 마락 될 겁니다. 파이팅!
