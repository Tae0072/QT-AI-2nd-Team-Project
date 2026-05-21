# CONTRIBUTING — QT-AI 기여 가이드

> 이 파일 한 장으로 팀원 누구든, AI 에이전트든 동일한 규칙으로 작업할 수 있어야 합니다.
> 상세 Git 규칙은 `doc/09_Git_규칙.md`를 참고하세요.

---

## 브랜치 전략

```
master  ←  최종 배포 (Lead 수동 머지)
  └── dev  ←  통합 브랜치 (모든 PR의 target)
        ├── feature/...
        ├── bugfix/...
        ├── hotfix/...
        ├── chore/...
        └── docs/...
```

## 브랜치명 규칙

```
{type}/{kebab-case-summary}
```

허용 타입: `feature` `bugfix` `hotfix` `chore` `release` `docs`

```bash
# 올바른 예시
feature/member-kakao-oauth
bugfix/qt-cache-expiry
chore/add-dependabot
docs/requirements-v34-sync

# 잘못된 예시
feat/memberOAuth       # camelCase 금지
fix_login              # 언더스코어 금지
my-feature             # type 접두사 없음
```

브랜치명 정규식 (CI에서 검증):
```
^(feature|bugfix|hotfix|chore|release|docs)/[a-z0-9]+([a-z0-9-]*[a-z0-9])?$
```

---

## 커밋 메시지 (Conventional Commits)

```
{type}({scope}): {대문자 또는 한글로 시작하는 요약}
```

허용 타입: `feat` `fix` `refactor` `chore` `docs` `test` `perf` `build` `ci` `revert`

```bash
# 올바른 예시
feat(member): Kakao OAuth 로그인 API 추가 (F-01)
fix(qt): 오늘 QT 캐시 00:00~04:00 만료 오류 수정
chore(ci): ci-all 집계 잡 추가
test(study): 시뮬레이터 상태 READY 조건 단위 테스트 추가

# 잘못된 예시
feat: add oauth         # 소문자 시작
fix: 버그 수정          # 너무 모호
WIP: 작업 중           # WIP 커밋 금지 (squash 후 PR)
```

---

## PR 규칙

- **base 브랜치는 항상 `dev`** (hotfix는 예외로 `master`)
- PR 크기: 가능하면 10 files 이하, 500 changed lines 이하
- 1 PR = 1 논리적 변경 (리팩토링과 기능 추가 혼용 금지)
- 테스트는 같은 PR에 포함
- 관련 F-ID는 PR 본문에 명시

## 절대 금지

- `master`, `dev`에 직접 push
- 강제 푸시 (`git push --force`)
- `.env`, `application-secret.yml` 등 시크릿 커밋
- `javax.*` import (Jakarta 사용)

---

## 로컬 환경 설정

```bash
# 레포 클론
git clone https://github.com/Tae0072/QT-AI-2nd-Team-Project.git
cd QT-AI-2nd-Team-Project
git checkout dev

# 백엔드 빌드 확인
cd qtai-server
./gradlew build -x test

# Flutter 의존성 설치
cd ../flutter-app
flutter pub get
flutter analyze
```

자세한 실행 방법은 각 워크스페이스 `실행가이드.md`를 참고하세요.
