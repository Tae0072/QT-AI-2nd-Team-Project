# Workflow - 2026-06-09 ai-event-outbox-decision-record

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-event-outbox-decision-record` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `ai-event-contract-fixtures`가 dev에 반영되어, event-driven 구현 전 outbox/processed event 소유권과 멱등 책임을 ADR로 고정해야 한다. |
| 기준 문서 | `2026-06-09_ai-event-driven-transition-design.md`, `2026-06-09_ai-event-contract-fixtures.md`, `2026-06-09_ai-service-operational-handoff.md` |
| 해당 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

Kafka 구현 전 단계로 ai-service의 outbox/processed event 저장소 위치, 중복 처리 기준, retry 책임, 외부 write 연계 기준을 결정 기록으로 남긴다. 이번 작업은 문서 전용이며 DB migration, JPA entity, relay worker, consumer/producer 구현은 포함하지 않는다.

## 범위

- outbox/processed event 저장소를 ai-service 소유 DB에 둔다고 결정한다.
- 후보 테이블명을 `ai_event_outbox`, `ai_processed_events`로 고정한다.
- broker-agnostic outbox pattern을 채택하고 Kafka 구현은 승인 이후로 남긴다.
- producer transaction, handler idempotency, retry, 실패 로그, payload 금지 범위를 문서화한다.
- `AiAssetApproved`와 Study publish 성공 상태를 분리한다.
- workflow, decision record, report 문서를 작성한다.

## 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- outbox/processed event DB migration
- JPA entity/repository 구현
- relay worker 구현
- provider live endpoint 호출
- gateway route enable
- 운영 DB 적용
- monolith AI 코드 삭제

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-outbox-decision-record.md` | 작업 범위와 검증 계획 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-event-outbox-decision-record.md` | ADR 형식의 outbox 결정 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-outbox-decision-record_report.md` | 수행 결과와 검증 결과 |

## 구현 순서

1. `dev`를 최신화한다.
2. `ai-event-contract-fixtures` 산출물이 dev에 있는지 확인한다.
3. `docs/ai-event-outbox-decision-record` 브랜치를 생성한다.
4. workflow 문서를 작성한다.
5. ADR 본문을 작성한다.
6. report 문서를 작성한다.
7. 문서 placeholder, 금지 데이터, trailing whitespace를 검증한다.
8. 문서 3개만 stage하고 커밋한다.

## 수용 기준

- [ ] 문서 3개가 생성된다.
- [ ] outbox/processed event 저장소 소유권이 ai-service DB로 결정되어 있다.
- [ ] 후보 테이블명이 `ai_event_outbox`, `ai_processed_events`로 기록되어 있다.
- [ ] event 발행 방식이 broker-agnostic outbox pattern으로 기록되어 있다.
- [ ] `eventId + handlerName` 중복 차단 기준이 명시되어 있다.
- [ ] `AiAssetApproved`와 Study publish 성공 상태 분리 기준이 명시되어 있다.
- [ ] Kafka, topic, consumer, producer, migration, entity, relay 구현이 제외되어 있다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 문서 3개가 같은 아키텍처 결정을 공유한다.
- 병렬 작성보다 한 흐름에서 결정 문구와 report를 맞추는 편이 안전하다.
- 구현 제외 범위가 넓어 일관된 경계 통제가 필요하다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, ADR, report를 직접 작성하고 검증한다.

## 검증 계획

- 문서 전용 변경이므로 Gradle 테스트는 실행하지 않는다.
- `git diff --check`
- fixture 산출물 존재 확인:
  - `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json`
  - `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java`
- placeholder 검색으로 미완성 표현이 남아 있지 않은지 확인한다.
- 금지 데이터 검색으로 허용되지 않은 번역본명, 외부 본문 출처명, 민감 값 예시가 문서에 없는지 확인한다.

## 후속 작업으로 남길 항목

- `provider-ai-input-event-contract`
- `ai-generation-worker-design`
- 설계 승인 후 `ai-event-outbox-skeleton`
