# 리포트 — 기본 성능/부하 점검 (k6 baseline)

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `chore/perf-baseline-k6` → PR 대상 `dev`
- 기준: 3단계 "통합·E2E·성능" 마지막

## 1. 한 줄 요약

핵심 읽기 경로에 대한 재사용 가능한 k6 부하 스크립트와 측정 기준(런북)을 추가하고, 로컬 스택(앱+MySQL)에 1회 실행해 baseline을 기록했다. 20 동시 사용자에서 **에러 0%, p95 13.4ms**로 임계값을 여유롭게 통과.

## 2. 산출물

- `perf/k6-smoke.js` — k6 부하 스크립트(20 VU 램프·유지·정리, threshold p95<500ms·실패<1%).
- `perf/README.md` — 지표 의미·실행법(docker compose + k6 docker)·해석·한계.

## 3. 대상

dev 프로파일(dev-bypass, `X-Dev-User-Id` 헤더로 memberId 주입) 핵심 읽기 GET:
`/api/v1/qt/passages/1`, `/api/v1/qt/today`, `/api/v1/qt/1/simulator`, `/api/v1/qt/1/study-content`.

## 4. baseline 측정 결과 (로컬 1대, MySQL 8.0 Docker)

| 지표 | 값 | 임계값 | 판정 |
|------|----|--------|------|
| 총 요청 | 4,312 | — | — |
| 처리량(RPS) | 61.1 req/s | — | — |
| 실패율 | **0.00%** (0/4312) | < 1% | ✅ |
| checks(2xx) | **100%** (4312/4312) | — | ✅ |
| 응답 지연 avg | 8.74ms | — | — |
| 응답 지연 **p95** | **13.42ms** | < 500ms | ✅ |
| 응답 지연 max | 41.06ms | — | — |

→ 기본 동시 부하에서 핵심 읽기 경로는 빠르고 안정적. (로컬 1대 수치는 운영 보장이 아니며, 같은 환경 반복 추세로 본다.)

## 5. 점검 중 발견한 버그 (별도 공유 필요)

- `GET /api/v1/bible/books` → **500**: `No enum constant com.qtai.domain.bible.internal.BibleBook.Testament.OT`.
- 원인: `bible_books` 시드 데이터의 testament 값('OT')과 `BibleBook.Testament` enum 상수가 불일치.
- bible 도메인(이지윤) 영역 → **담당자 공유 필요**. (성경 책 목록 화면이 깨지는 demo-blocking 이슈)

## 6. 후속

- 인증 포함·쓰기 경로(신고·노트) 부하는 토큰 발급을 붙여 확장.
- 임계값/시나리오는 시연 환경에 맞게 조정.
- 위 bible/books 500 버그는 별도 처리.
