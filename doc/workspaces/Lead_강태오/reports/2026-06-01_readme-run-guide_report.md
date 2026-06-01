# 리포트 — 팀원용 백엔드 로컬 실행 가이드(README)

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 반영: 문서 backfill PR

## 1. 한 줄 요약

팀원이 pull 후 바로 백엔드를 띄울 수 있도록, 루트 README에 "백엔드 로컬 실행 (Docker Compose)" 섹션을 추가했다.

## 2. 내용

- 사전 준비: Docker 설치, 포트 3306/6379/8080.
- `.env` 생성(`cp .env.example .env` + JWT 키를 `qtai-server/src/test/resources/application.yml`에서 복사).
- `docker compose up --build`(첫 빌드 수 분, 접속 `http://localhost:8080`).
- 프로파일별 DB 표(test/local=H2, dev/prod=MySQL).

## 3. 주의(이력)

- 최초 작성분이 #178 머지 시점 이후 커밋되어 dev에 반영되지 않았다 → 본 backfill PR에 README 가이드를 재포함해 dev에 반영.

## 4. 검증

- 가이드 절차(`.env` → `docker compose up`)는 #178 통합 기동(`Started QtAiApplication`)으로 동작 확인됨.
