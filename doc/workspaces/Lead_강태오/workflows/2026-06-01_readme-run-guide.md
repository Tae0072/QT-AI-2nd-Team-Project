# 워크플로우 — 팀원용 백엔드 로컬 실행 가이드(README)

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project
- 관련: Docker Compose 시연 스택(#178)
- 반영: 본 문서 backfill PR

## 1. 배경

#178로 앱+MySQL+Redis 통합 compose가 들어갔지만, 팀원이 pull 후 바로 실행하려면 `.env` 생성·포트·빌드 시간 같은 전제 조건 안내가 필요하다. README에 실행 가이드가 없었다.

## 2. 작업 범위

- 루트 `README.md`에 "백엔드 로컬 실행 (Docker Compose)" 섹션 추가.
- 사전 준비(Docker, 포트 3306/6379/8080), `.env` 생성, `docker compose up --build`, 프로파일별 DB 표.

## 3. 절차 / 비고

1. README에 실행 가이드 섹션 추가.
2. 주의: 최초 작성분이 #178 머지 시점 이후 커밋되어 dev에 누락 → 본 문서 backfill PR에 재포함.

## 4. 검증

- 가이드대로 `cp .env.example .env`(테스트 키 입력) → `docker compose up`이 동작함을 #178 통합 기동으로 확인.
