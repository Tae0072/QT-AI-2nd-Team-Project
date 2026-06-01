# 워크플로우 — Docker Compose 시연 스택(앱+MySQL+Redis)

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project
- 기준 문서: `CLAUDE.md` §8(평문 비밀 금지), 2단계. dev MySQL 전환(#177) 후속
- PR: #178 (머지됨)

## 1. 배경

시연을 위해 앱+MySQL+Redis를 한 번에 띄우는 통합 스택이 필요하다. 기존 루트 compose는 인프라(mysql+redis)만 있었고 앱 서비스가 없었다.

## 2. 작업 범위

- `docker-compose.yml`에 `qtai-server` 서비스 추가(healthcheck, depends_on, dev 프로파일).
- 비밀값(JWT 키)은 `.env`로 분리(레포 평문 금지). `.env.example` 제공.
- 실제 통합 기동으로 앱이 MySQL에 붙어 뜨는지 검증.

## 3. 절차

1. `feature/infra-compose-demo` 브랜치.
2. compose: mysql/redis healthcheck + `qtai-server`(build, `depends_on: service_healthy`, env로 dev DB/redis 주입) + `env_file: .env(required:false)`.
3. `.env.example`(JWT 키 안내) 작성, `.env`는 gitignore 확인.
4. 통합 기동 검증 중 버그 2건 발견·수정:
   - Dockerfile CRLF gradlew(`exit 127`) → `sed`로 정규화 + `.gitattributes eol=lf`.
   - `@Lob` ↔ MySQL 타입 불일치(Hibernate validate 실패) → ai/audit 4개 필드 `@Lob` → `@Column(columnDefinition="LONGTEXT")`.
5. 앱 기동 성공(`Started QtAiApplication`) + H2 회귀 통과 후 PR.

## 4. 정책 준수

- 비밀값 평문 커밋 금지(§8) — `.env` 주입.
- 수정한 ai·audit 엔티티는 PR 공유.

## 5. 검증 명령

```powershell
docker compose up --build      # 통합 기동 (Started QtAiApplication 확인)
cd qtai-server; .\gradlew.bat test --no-daemon
```
