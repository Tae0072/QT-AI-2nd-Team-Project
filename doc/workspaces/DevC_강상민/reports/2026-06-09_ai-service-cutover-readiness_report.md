# Report - 2026-06-09 ai-service-cutover-readiness

## 작업 요약

`ai-service` cutover 전에 팀이 확인해야 하는 readiness checklist를 문서화했다. 대상은 `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**` 전환 준비 조건이다.

이번 변경은 문서 전용이다. gateway route 활성화, provider live 호출, 운영 DB migration 적용, service-token/JWKS 구현, monolith AI 삭제는 수행하지 않았다.

## 변경 내용

- workflow 문서 `2026-06-09_ai-service-cutover-readiness.md`를 추가했다.
- 팀 공유용 checklist 문서 `2026-06-09_ai-service-cutover-readiness-checklist.md`를 추가했다.
- report 문서 `2026-06-09_ai-service-cutover-readiness_report.md`를 추가했다.
- checklist에는 다음 항목을 고정했다.
  - cutover 대상 endpoint inventory
  - 전환 전 완료 기준
  - env var 목록
  - runtime smoke 실행 순서
  - AI 소유 DB 7개 테이블과 비소유 테이블
  - provider `/api/v1/system/**` readiness
  - gateway route 전환 전 확인 항목
  - cutover gate, rollback 기준, 전환 후 확인 항목

## 확인한 기준 상태

- `dev`에 `ai-service runtime smoke readiness` 머지 커밋이 반영되어 있었다.
- runtime smoke script 경로가 존재한다.
- runtime smoke test 경로가 존재한다.
- 문서 작업 외 코드, 테스트, OpenAPI 변경은 하지 않았다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git status --short --branch` | 통과 |
| `git diff --check` | 통과 |
| `Test-Path "qtai-server\ai-service\scripts\runtime-smoke-readiness.ps1"` | 통과 |
| `Test-Path "qtai-server\ai-service\src\test\java\com\qtai\ai\AiServiceRuntimeSmokeReadinessTest.java"` | 통과 |
| placeholder 문구 검색 | 통과 |

## 제외 확인

- gateway route를 변경하지 않았다.
- provider endpoint를 호출하지 않았다.
- 운영 DB 연결이나 migration 적용을 하지 않았다.
- service-token/JWKS 구현을 추가하지 않았다.
- monolith AI 코드를 삭제하지 않았다.
- Docker/K8s 설정을 변경하지 않았다.

## 다음 작업

- `gateway-ai-route-transition-skeleton`
- provider endpoint open 후 live smoke 연결
- 운영 DB 이관 절차 확정
