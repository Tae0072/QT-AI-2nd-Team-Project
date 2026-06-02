# 성능/부하 점검 (perf)

QT-AI 백엔드의 **기본 성능·부하**를 시연 전에 점검하기 위한 도구와 기준.

## 무엇을 보나 (지표)

| 지표 | 의미 | MVP 기준(권장) |
|------|------|----------------|
| `http_req_duration` p95 | 95% 요청이 끝나는 시간(응답 지연) | < 500ms |
| `http_req_failed` rate | 실패(5xx/네트워크) 비율 | < 1% |
| `http_reqs` (RPS) | 초당 처리 요청 수(처리량) | 측정·기록 |
| VUs | 동시 가상 사용자 수 | 기본 20 |

> "기본 점검"이므로 대규모 부하가 아니라 **동시 20명 수준에서 핵심 읽기 경로가 안정적인지**를 본다.

## 대상 엔드포인트

`dev` 프로파일은 `dev-bypass`로 인증이 비활성이라(+`X-Dev-User-Id` 헤더로 memberId 주입) 토큰 없이 읽기 경로를 칠 수 있다(`k6-smoke.js`):

- `GET /api/v1/qt/passages/1` — QT 본문 단건
- `GET /api/v1/qt/today` — 오늘의 QT
- `GET /api/v1/qt/1/simulator` — 시뮬레이터 상태
- `GET /api/v1/qt/1/study-content` — 학습 콘텐츠 진입

> 후보였던 `GET /api/v1/bible/books`는 점검 중 500(`BibleBook.Testament.OT` enum 불일치)으로 확인되어 제외했다(bible 도메인 후속 이슈).

## ⚠️ dev-bypass / BASE_URL 안전 주의

- 무인증 읽기(dev-bypass, `X-Dev-User-Id`)는 **`dev` 프로파일에서만** 동작한다. `prod`에서는 강제 비활성이다: `application-prod.yml`이 `qtai.security.dev-bypass=false`로 고정하고, `DevSecurityConfig`가 `@Profile("dev")` + 토글 + "active profiles에 prod 포함 시 기동 실패"의 3중 가드를 가진다.
- 따라서 이 부하 스크립트는 **로컬 전용**으로만 쓴다. `BASE_URL`은 `http://localhost:8080` 또는 `http://host.docker.internal:8080`만 가리키고, **운영(prod) URL을 절대 대상으로 삼지 않는다.**

## 실행 방법

### 1) 앱 스택 기동 (dev 프로파일)

```bash
# 루트에서 (.env 준비 후 — README 참고)
docker compose up -d
# Flyway가 스키마 생성 + 샘플 시드(성경 66권, QT 본문 3건) 적용
```

### 2) k6로 부하 주기

k6를 설치했다면:
```bash
BASE_URL=http://localhost:8080 k6 run perf/k6-smoke.js
```

설치 없이 Docker로:
```bash
# Linux
docker run --rm -i --network host -e BASE_URL=http://localhost:8080 \
  grafana/k6 run - < perf/k6-smoke.js
# Windows/Mac (Docker Desktop)
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run - < perf/k6-smoke.js
```

### 3) 결과 해석

- k6 요약의 `http_req_duration`(avg/p95/max), `http_req_failed`, `http_reqs`를 확인.
- 임계값(p95<500ms, 실패<1%)을 못 넘기면 k6가 **실패 종료코드**를 반환한다 → CI 게이트나 시연 전 체크로 사용 가능.
- 결과 수치는 머신/환경에 따라 다르므로, **같은 환경에서 반복 측정한 추세**로 본다(절대값보다 회귀 탐지).

## 한계 / 후속

- 로컬 1대 측정은 운영 성능을 보장하지 않는다(네트워크·DB 규모·인스턴스 차이).
- 쓰기 경로(신고·노트 등)와 인증 포함 경로 부하는 토큰 발급을 붙여 후속 확장.
- 필요 시 stages(VU·시간)·thresholds를 환경에 맞게 조정한다.
