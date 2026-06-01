# Gitleaks — 시크릿 유출 탐지

> **왜 배워야 하나:** QT-AI의 PR 전 검증 명령 중 하나가 Gitleaks 실행이다. 코드에 비밀번호, API 키, 토큰 같은 민감 정보가 실수로 커밋되는 것을 방지한다. 노션에서 다루지 않은 보안 도구다.

---

## 1. Gitleaks가 뭔가?

Gitleaks는 Git 저장소에서 **비밀 정보(secrets)**가 유출되었는지 자동으로 스캔하는 도구다.

개발하다 보면 실수로 이런 것들을 커밋할 수 있다:

```
❌ 비밀번호: password = "admin123"
❌ API 키: DEEPSEEK_API_KEY = "sk-abc123..."
❌ 토큰: jwt.secret = "mysupersecretkey"
❌ DB 접속 정보: spring.datasource.password = "root"
```

이런 정보가 GitHub에 올라가면 누구나 볼 수 있다. Gitleaks는 이걸 **커밋 전에 잡아준다.**

## 2. 사용법

### 2.1 설치

```bash
# Mac
brew install gitleaks

# Windows (Chocolatey)
choco install gitleaks

# 또는 GitHub에서 바이너리 다운로드
# https://github.com/gitleaks/gitleaks/releases
```

### 2.2 검사 실행

```bash
# QT-AI PR 전 검증 명령 (CLAUDE.md §11)
gitleaks detect --source . --redact --exit-code 1
```

옵션 설명:
- `--source .`: 현재 디렉토리 검사
- `--redact`: 발견된 시크릿을 마스킹해서 표시 (전체 노출 방지)
- `--exit-code 1`: 시크릿이 발견되면 에러 코드 반환 (CI에서 빌드 실패시킴)

### 2.3 결과 예시

```
Finding:     password = "REDACTED"
Secret:      REDACTED
RuleID:      generic-password
Entropy:     3.5
File:        src/main/resources/application.yml
Line:        15
Commit:      abc123...

12 leaks detected. Exit code: 1
```

## 3. 올바른 비밀 정보 관리법

```yaml
# ❌ 잘못된 방법 — 직접 값을 넣음
spring:
  datasource:
    password: admin123

# ✅ 올바른 방법 — 환경 변수 참조
spring:
  datasource:
    password: ${DB_PASSWORD}
```

환경 변수는 서버 실행 시 주입하거나, `.env` 파일(`.gitignore`에 추가)로 관리한다.

## 4. .gitleaks.toml — 예외 설정

테스트 코드의 가짜 토큰 같은 건 허용해야 할 때:

```toml
# .gitleaks.toml
[allowlist]
paths = [
    "src/test/",        # 테스트 코드 제외
    ".env.example"       # 예시 파일 제외
]
```

## 5. QT-AI에서의 적용

CLAUDE.md에 명시된 규칙:
- 로그에 password, token, private key, 민감 개인정보를 남기지 않는다
- AI Provider API key는 외부 연동 영역에서만 사용하고 로그에 남기지 않는다
- PR 전에 `gitleaks detect` 실행으로 유출 여부를 확인한다

## 6. 참고 자료

- Gitleaks 공식 GitHub: https://github.com/gitleaks/gitleaks
- Gitleaks 설정 가이드: https://github.com/gitleaks/gitleaks#configuration
