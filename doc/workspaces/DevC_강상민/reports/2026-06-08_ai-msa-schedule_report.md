# AI MSA 일정표 작성 리포트

## 1. 작업 개요

- 팀원이 DevC의 AI MSA 분리 일정과 상대 의존성을 한눈에 확인할 수 있도록 일정표 문서를 작성했다.
- workflow 문서 파일명 규칙에 맞춰 `2026-06-08_ai-msa-schedule.md`로 정리했다.
- 문서 전용 PR이므로 서버 코드, 테스트 코드, OpenAPI, DB migration은 변경하지 않았다.

## 2. 변경 파일

| 파일 | 내용 |
| --- | --- |
| `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-msa-schedule.md` | AI MSA 분리 현재 상태, 작업 일정, provider 의존성, 분리 완료 기준 정리 |
| `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-msa-schedule_report.md` | 문서 작업 결과와 검증 범위 기록 |

## 3. 반영 내용

- 완료된 AI boundary, system endpoint contract, HTTP adapter foundation, runtime toggle 검증 흐름을 정리했다.
- provider endpoint 오픈 전 준비할 readiness checklist, contract fixture, smoke test skeleton 작업을 명시했다.
- provider endpoint 오픈 후 실제 연결 smoke test와 `ai-service` 물리 분리 순서를 정리했다.
- 후속 checklist/fixture에 오늘 QT `STALE_FALLBACK` 응답과 F-15 차단 응답의 `blocked_reason` 케이스를 포함하도록 명시했다.

## 4. 제외 범위

- 실제 HTTP client 구현 변경
- provider endpoint Controller 구현
- DB/migration 변경
- service-token/JWKS 발급 구현
- gateway, Docker, Kubernetes, Helm 운영 설정

## 5. 검증

- 문서 경로와 파일명 규칙을 확인했다.
- 문서 내 미완료 표시 문구가 없음을 확인한다.
- `git diff --check`로 trailing whitespace가 없음을 확인한다.
