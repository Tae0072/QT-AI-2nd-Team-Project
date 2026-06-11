# Report - 2026-06-09 ai-service-cutover-runbook

## 작업 요약

`ai-service` 전환 실행을 위한 runbook을 작성했다. readiness checklist와 gateway route skeleton이 준비된 상태를 기준으로 dry-run, live cutover, rollback, post-cutover 확인 절차를 문서화했다.

이번 변경은 문서 전용이다. gateway route 활성화, provider live 호출, 운영 DB migration 적용, 운영 비밀 값/JWKS 구현, monolith AI 삭제는 수행하지 않았다.

## 변경 내용

- workflow 문서 `2026-06-09_ai-service-cutover-runbook.md`를 추가했다.
- 팀 실행용 runbook `2026-06-09_ai-service-cutover-runbook.md`를 추가했다.
- report 문서 `2026-06-09_ai-service-cutover-runbook_report.md`를 추가했다.
- 기존 cutover readiness checklist에 runbook 참조를 추가했다.

## runbook 반영 항목

- 대상 route 3개 축
- dry-run과 live cutover 실행 모드
- AI, gateway, provider, DB, QA, Lead 역할 분담
- pre-flight gate
- dry-run 절차
- live cutover 순서
- gateway route 확인 항목
- failure matrix
- rollback 순서
- post-cutover 확인 항목
- 완료 기록 항목

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 통과 |
| readiness checklist 경로 확인 | 통과 |
| gateway route skeleton workflow 경로 확인 | 통과 |
| `AiServiceRouteConfiguration.java` 경로 확인 | 통과 |
| placeholder 문구 검색 | 통과 |

## 제외 확인

- gateway AI route를 활성화하지 않았다.
- provider endpoint를 호출하지 않았다.
- 운영 DB migration을 적용하지 않았다.
- 운영 비밀 값/JWKS 구현을 추가하지 않았다.
- monolith AI 코드를 삭제하지 않았다.
- Docker/K8s 설정을 변경하지 않았다.

## 다음 작업

- provider endpoint open 후 live smoke 연결
- 운영 DB 이관 절차 실행
- gateway AI route enable PR
- route 전환 후 monolith AI 제거 계획
