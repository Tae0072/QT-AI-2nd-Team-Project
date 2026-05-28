# GitHub Actions — CI/CD 자동화

> **왜 배워야 하나:** QT-AI 프로젝트는 PR을 올리면 자동으로 빌드, 테스트, 코드 리뷰가 실행된다. 이걸 GitHub Actions가 해준다. 노션에서 Git 기초(merge, rebase, reset)를 배웠지만, GitHub Actions(CI/CD 파이프라인)는 다루지 않았다.

---

## 1. CI/CD가 뭔가?

- **CI (Continuous Integration):** 코드를 push하면 자동으로 빌드하고 테스트하는 것
- **CD (Continuous Deployment):** 테스트를 통과하면 자동으로 배포하는 것

QT-AI에서는 주로 CI를 활용한다: PR 올리면 → 자동 빌드 → 자동 테스트 → 자동 리뷰

## 2. GitHub Actions 기본 구조

워크플로우 파일은 `.github/workflows/` 폴더에 YAML로 작성한다.

```yaml
# .github/workflows/qt-ai-ci.yml
name: QT-AI CI                    # 워크플로우 이름

on:                                 # 언제 실행할까?
  pull_request:
    branches: [dev]                 # dev 브랜치로 PR이 올라오면

jobs:                               # 어떤 작업을 할까?
  build:
    runs-on: ubuntu-latest          # Ubuntu 가상 머신에서 실행
    steps:
      - uses: actions/checkout@v4   # 1. 소스 코드 가져오기

      - uses: actions/setup-java@v4 # 2. Java 21 설치
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build                 # 3. 빌드
        run: ./gradlew -p qtai-server build

      - name: Test                  # 4. 테스트
        run: ./gradlew -p qtai-server test
```

## 3. 핵심 개념

### 3.1 워크플로우 (Workflow)
하나의 자동화 파이프라인. `.yml` 파일 하나가 워크플로우 하나다.

### 3.2 이벤트 (Event)
워크플로우를 실행시키는 트리거:

```yaml
on:
  push:                    # push할 때
    branches: [dev]
  pull_request:            # PR 올릴 때
    branches: [dev]
  schedule:                # 정해진 시간에 (cron)
    - cron: '0 4 * * *'   # 매일 04:00 UTC
```

### 3.3 잡 (Job)
하나의 가상 머신에서 실행되는 작업 묶음. 여러 잡은 병렬로 실행된다.

### 3.4 스텝 (Step)
잡 안에서 순서대로 실행되는 개별 명령.

## 4. QT-AI에서 실제 사용하는 워크플로우

QT-AI 프로젝트의 `.github/workflows/`에는 두 가지 워크플로우가 있다:

### 4.1 qt-ai-ci.yml — CI 파이프라인
- PR이 올라오면 자동 실행
- Gradle 빌드 + 테스트
- 금지 기술 패턴 검사 (Kafka, K8s, SSE 등)

### 4.2 claude-pr-review.yml — 자동 코드 리뷰
- PR이 올라오면 Claude AI가 자동으로 코드를 리뷰
- 아키텍처 규칙 위반, 금지 패턴 사용을 감지

## 5. Secrets — 민감 정보 관리

GitHub Actions에서 API 키 같은 민감 정보는 Settings → Secrets에 저장한다:

```yaml
# 워크플로우에서 시크릿 사용
env:
  DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

코드에 직접 비밀번호를 넣으면 안 되는 이유와 같은 원리다.

## 6. 참고 자료

- GitHub Actions 공식 문서: https://docs.github.com/en/actions
- GitHub Actions 빠른 시작: https://docs.github.com/en/actions/quickstart
