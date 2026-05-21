# AI_AGENT — AI 에이전트 작업 규칙

> Claude Code, Codex 등 AI 에이전트가 이 레포에서 작업할 때 따라야 할 규칙입니다.
> 이 파일은 `CLAUDE.md`와 함께 읽어야 합니다. 충돌 시 `CLAUDE.md`가 우선합니다.

---

## PR 생성 규칙

1. **base 브랜치**: 항상 `dev` (긴급 핫픽스만 `master`)
2. **브랜치명**: `{type}/{kebab-case-summary}` 형식 준수
3. **커밋**: Conventional Commits, 한 PR에 한 가지 관심사
4. **PR 제목**: `{type}({scope}): {대문자/한글로 시작하는 요약}`
5. **PR 본문**: `.github/pull_request_template.md` 모든 필드 채움
6. **변경 줄 수**: 500줄 이하 권장. 초과 시 PR 본문에 사유 명시
7. **F-ID**: 기능 관련 PR은 반드시 관련 F-ID 명시

## 작업 단위 분할 원칙

- 1 PR = 1 논리적 변경
- 리팩토링과 기능 추가를 섞지 않는다
- 테스트는 같은 PR에 포함한다
- 관련 없는 파일 포맷팅, 파일 이동은 하지 않는다

## 절대 금지

- `master`, `dev`에 직접 push
- 보호 파일 단독 변경: `build.gradle.kts`, `Dockerfile`, `.github/**`
  (변경 필요 시 PR + Lead 리뷰 필수)
- 강제 푸시 (`git push --force`)
- 머지 커밋 생성 (squash merge만 허용)
- 아래 `CLAUDE.md §8`의 금지 기능·금지 기술 구현

## 도메인 경계 준수

- 다른 도메인의 `Entity`, `Service`, `Repository`를 직접 import하지 않는다
- 다른 도메인 호출은 반드시 `api/UseCase` 인터페이스를 통한다
- Controller는 Repository를 직접 호출하지 않는다

## 코드 검증

작업 완료 후 아래 명령으로 검증:

```bash
cd qtai-server
./gradlew build
./gradlew test jacocoTestReport
```
