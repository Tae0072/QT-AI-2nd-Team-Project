# ai-event-driven-transition-design Report

## 1. 작업 개요

`docs/ai-event-driven-transition-design` 브랜치에서 AI MSA 분리 이후 event-driven 전환 설계 문서를 작성했다.

이번 작업은 문서 전용이다. Kafka 의존성 추가, topic 생성, consumer/producer 구현, provider live 호출, gateway route enable, 운영 DB migration 적용, monolith AI 삭제는 수행하지 않았다.

## 2. 생성 파일

| 경로 | 내용 |
| --- | --- |
| `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-driven-transition-design.md` | 작업 목표, 범위, 제외 범위, 검증 기준 |
| `doc/workspaces/DevC_강상민/2026-06-09_ai-event-driven-transition-design.md` | HTTP 유지 대상과 event-driven 전환 후보 설계 |
| `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-driven-transition-design_report.md` | 실행 결과와 검증 기록 |

## 3. 설계 반영 내용

- HTTP 유지 대상과 event-driven 전환 후보를 분리했다.
- AI generation job, validation, approved asset publish, audit/notification 후처리를 event 후보로 정리했다.
- provider가 AI 입력 이벤트를 발행하는 구조는 합의 필요 항목으로 분리했다.
- 이벤트 payload 허용 필드와 금지 필드를 명시했다.
- 멱등성, 순서 보장, 실패 처리, 관측성 기준을 정리했다.
- 다음 작업 순서를 `ai-event-contract-fixtures`, `ai-event-outbox-decision-record`, `provider-ai-input-event-contract`, `ai-generation-worker-design`로 제안했다.

## 4. 제외한 작업

- Kafka 구현
- topic 생성
- consumer/producer 작성
- provider Controller 구현
- provider live endpoint 호출
- gateway route 실제 enable
- 운영 DB migration 적용
- monolith AI 코드 삭제
- 기존 HTTP client/mock 제거

## 5. 검증 결과

문서 전용 변경이라 Gradle 테스트는 실행하지 않았다.

| 검증 | 결과 |
| --- | --- |
| `git diff --check` | 통과 |
| 기준 문서 존재 확인 | 통과 |
| placeholder 검색 | 통과 |

## 6. 후속 작업

1. `ai-event-contract-fixtures`
2. `ai-event-outbox-decision-record`
3. `provider-ai-input-event-contract`
4. `ai-generation-worker-design`
