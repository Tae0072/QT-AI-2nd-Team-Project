# 워크플로우 — 기본 성능/부하 점검 (k6)

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project
- 기준: 3단계 "통합·E2E·성능" 마지막 / 시연(W4~W5) 준비
- PR 대상: `dev`

## 1. 배경

시연 전 "핵심 읽기 경로가 모듈한 동시 부하에서 빠르고 안정적인가"를 확인할 재사용 도구가 없었다. 일회성 측정이 아니라 누구나 반복 실행할 수 있는 부하 스크립트 + 측정 기준이 필요하다.

## 2. 작업 범위

- k6 부하 스크립트(`perf/k6-smoke.js`) + 런북(`perf/README.md`).
- dev-bypass로 인증 없이 칠 수 있는 핵심 읽기 엔드포인트 대상.
- 가능하면 로컬 스택에 1회 돌려 baseline 기록(절대값보다 회귀 탐지용).

## 3. 절차

1. `chore/perf-baseline-k6` 브랜치.
2. 대상 엔드포인트 선정(전부 GET, dev 프로파일 dev-bypass + `X-Dev-User-Id` 헤더로 memberId 주입): `/api/v1/qt/passages/1`, `/api/v1/qt/today`, `/api/v1/qt/1/simulator`, `/api/v1/qt/1/study-content`.
   - 초안에서 후보였던 `/api/v1/bible/books`는 점검 중 500(`BibleBook.Testament.OT` enum 불일치)으로 확인되어 대상에서 제외(bible 도메인 후속 이슈). 스크립트(`perf/k6-smoke.js`)·리포트와 이 목록을 동일하게 유지한다.
3. k6 스크립트: stages(20 VU 램프업·유지·정리) + thresholds(p95<500ms, 실패<1%).
4. 런북: 지표 의미·실행법(docker compose + k6 docker)·해석·한계.
5. (선택) 로컬에서 앱 기동 후 k6 1회 실행해 baseline 캡처.

## 4. 정책/주의

- 인증 포함·쓰기 경로 부하는 후속(토큰 발급 필요).
- 로컬 1대 수치는 운영 보장이 아니므로 "같은 환경 반복 추세"로 해석.

## 5. 검증/실행 명령

```bash
docker compose up -d
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < perf/k6-smoke.js
```
