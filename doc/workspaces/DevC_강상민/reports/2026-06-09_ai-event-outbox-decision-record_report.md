# ai-event-outbox-decision-record Report

## 1. 작업 개요

`docs/ai-event-outbox-decision-record` 브랜치에서 AI event outbox 결정 기록 문서를 작성했다.

이번 작업은 문서 전용이다. Kafka 의존성 추가, topic 생성, consumer/producer 구현, outbox table migration, JPA entity, relay worker, provider live 호출, gateway route enable, 운영 DB 적용, monolith AI 삭제는 수행하지 않았다.

## 2. 전제 확인

`dev` 최신화 후 `ai-event-contract-fixtures` 산출물이 존재하는지 확인했다.

| 확인 대상 | 결과 |
| --- | --- |
| `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json` | 존재 |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | 존재 |

## 3. 생성 파일

| 경로 | 내용 |
| --- | --- |
| `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-outbox-decision-record.md` | 작업 범위와 검증 계획 |
| `doc/workspaces/DevC_강상민/2026-06-09_ai-event-outbox-decision-record.md` | ADR 형식의 outbox 결정 기록 |
| `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-outbox-decision-record_report.md` | 수행 결과와 검증 결과 |

## 4. 결정 내용

- outbox/processed event 저장소는 ai-service 소유 DB에 둔다.
- 후보 테이블명은 `ai_event_outbox`, `ai_processed_events`로 고정한다.
- event 발행은 broker-agnostic outbox pattern으로 설계한다.
- producer는 domain state 변경과 outbox append를 같은 DB transaction 안에서 처리한다.
- handler는 `eventId + handlerName` 기준으로 중복 처리를 차단한다.
- 외부 write 연계는 기존 `Idempotency-Key` 계약을 유지한다.
- `AiAssetApproved`와 Study publish 성공 상태는 분리한다.
- retry 책임은 outbox relay와 handler retry로 나눈다.
- event payload와 log에는 식별자, 상태, trace, idempotency 값만 허용한다.

## 5. 제외한 작업

- Kafka 구현
- topic 생성
- consumer/producer 작성
- outbox/processed event migration
- JPA entity/repository 구현
- relay worker 구현
- provider live endpoint 호출
- gateway route enable
- 운영 DB 적용
- monolith AI 코드 삭제

## 6. 검증 결과

문서 전용 변경이라 Gradle 테스트는 실행하지 않았다.

| 검증 | 결과 |
| --- | --- |
| `git diff --check` | 통과 |
| fixture 산출물 존재 확인 | 통과 |
| placeholder 검색 | 통과 |
| 금지 데이터/민감 값 검색 | 통과 |

## 7. 후속 작업

1. `provider-ai-input-event-contract`
2. `ai-generation-worker-design`
3. 설계 승인 후 `ai-event-outbox-skeleton`
