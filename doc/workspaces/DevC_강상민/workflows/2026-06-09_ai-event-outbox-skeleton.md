# Workflow - 2026-06-09 ai-event-outbox-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-event-outbox-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `ai-event-outbox-decision-record`와 `ai-generation-worker-design`에서 outbox/processed event 저장소 책임이 ai-service 소유 DB로 고정되어 skeleton 구현이 필요하다. |
| 기준 문서 | `2026-06-09_ai-event-outbox-decision-record.md`, `2026-06-09_ai-generation-worker-design.md`, `ai-event-contract-fixtures.json` |
| 해당 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

ai-service 소유 DB에 event outbox와 processed event skeleton을 추가한다. 이번 작업은 persistence 기반과 H2/Flyway 검증을 만드는 단계이며 Kafka, topic, producer, consumer, relay worker, 실제 event publish는 구현하지 않는다.

## 범위

- `ai_event_outbox` entity/repository/status skeleton을 추가한다.
- `ai_processed_events` entity/repository/status skeleton을 추가한다.
- 기존 ai-service Flyway V1 DDL에 두 테이블과 핵심 index/unique constraint를 추가한다.
- persistence opt-in context에서 두 repository와 entity가 등록되는지 검증한다.
- repository 테스트로 pending outbox 조회, published/failed 상태 전이, processed event 중복 차단 기준을 검증한다.
- migration validation 테스트가 9개 AI 소유 테이블을 확인하도록 갱신한다.
- workflow 문서와 report 문서를 작성한다.

## 제외 범위

- Kafka 의존성 추가
- topic 생성
- producer/consumer 구현
- relay worker 구현
- event handler 구현
- 실제 broker publish
- existing usecase에 outbox append 연결
- 운영 데이터 이관
- 기존 HTTP client 제거

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `AiEventOutbox.java`, `AiEventOutboxStatus.java`, `AiEventOutboxRepository.java` | outbox 저장 모델과 repository |
| Create | `AiProcessedEvent.java`, `AiProcessedEventStatus.java`, `AiProcessedEventRepository.java` | handler별 처리 이력 모델과 repository |
| Modify | `V1__create_ai_owned_tables.sql` | `ai_event_outbox`, `ai_processed_events` DDL 추가 |
| Modify | persistence context/migration/repository tests | entity/repository/DDL 정합성 검증 |
| Create | `AiServiceEventOutboxPersistenceTest.java` | outbox/processed event repository 동작 검증 |
| Create | workflow/report 문서 | 작업 계획과 수행 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-event-outbox-skeleton` 브랜치를 준비한다.
2. workflow 문서를 작성한다.
3. outbox/processed event status enum과 entity를 추가한다.
4. repository를 추가한다.
5. Flyway V1 DDL에 두 테이블, index, unique constraint를 추가한다.
6. persistence enabled context test와 migration validation test를 9개 entity/table 기준으로 보강한다.
7. repository 테스트를 추가한다.
8. report 문서를 작성한다.
9. compile/test, diff, 금지어 검증 후 문서와 코드 변경만 stage한다.

## 수용 기준

- [ ] `qtai.ai.persistence.enabled=false` 기본 실행 경로는 변경하지 않는다.
- [ ] `qtai.ai.persistence.enabled=true`에서 outbox/processed event repository가 등록된다.
- [ ] JPA metamodel에 `AiEventOutbox`, `AiProcessedEvent`가 포함된다.
- [ ] Flyway migration validation에서 `ai_event_outbox`, `ai_processed_events` 테이블이 확인된다.
- [ ] outbox pending event 조회가 `createdAt`, `id` 오름차순으로 동작한다.
- [ ] outbox published/failed 상태 전이가 저장된다.
- [ ] processed event는 `eventId + handlerName` unique 기준으로 중복 처리를 차단한다.
- [ ] event payload/log에 prompt, provider raw response, 본문 원문, 인증 값, DB 접속 값 저장을 허용하지 않는다.
- [ ] Kafka, topic, relay worker, producer/consumer 구현은 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- entity, repository, DDL, migration validation 테스트가 같은 schema 경계를 공유한다.
- DDL과 JPA mapping이 불일치하면 즉시 검증 실패가 나므로 한 맥락에서 수정하는 편이 안전하다.
- 변경 범위가 ai-service persistence에 집중되어 병렬화 이점이 작다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow, persistence skeleton, 테스트, report를 직접 작성하고 검증한다.

## 검증 계획

- `cd qtai-server`
- `.\gradlew.bat :ai-service:compileJava`
- `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiServiceEventOutboxPersistenceTest --tests com.qtai.domain.ai.internal.AiServicePersistenceEnabledContextTest --tests com.qtai.domain.ai.internal.AiServiceMigrationValidationTest`
- `cd ..`
- `git diff --check`
- placeholder 검색으로 workflow/report 미완성 표현이 없는지 확인한다.
- 금지 데이터/민감 예시 문구 검색으로 ai-service와 문서에 허용되지 않는 내용이 없는지 확인한다.

## 후속 작업으로 남길 항목

- outbox append를 실제 UseCase transaction에 연결
- outbox relay worker skeleton
- processed event cleanup 정책
- broker live smoke readiness
