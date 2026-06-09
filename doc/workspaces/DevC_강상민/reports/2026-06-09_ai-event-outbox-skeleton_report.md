# ai-event-outbox-skeleton Report

## 1. 작업 개요

- 작업 브랜치: `feature/ai-event-outbox-skeleton`
- 작업 유형: ai-service persistence skeleton
- 목적: ai-service 소유 DB에 event outbox와 processed event 저장 기반을 추가
- 기준 문서:
  - `2026-06-09_ai-event-outbox-decision-record.md`
  - `2026-06-09_ai-generation-worker-design.md`
  - `ai-event-contract-fixtures.json`

## 2. 반영 내용

- `ai_event_outbox` skeleton을 추가했다.
  - event id, event name, aggregate, schema version, payload, status, retry, trace, created/published time을 저장한다.
  - `PENDING`, `PUBLISHED`, `FAILED` 상태를 가진다.
  - pending event 조회는 `createdAt`, `id` 오름차순으로 수행한다.
- `ai_processed_events` skeleton을 추가했다.
  - `eventId + handlerName` 기준으로 handler별 처리 이력을 저장한다.
  - 같은 event라도 handler가 다르면 별도 처리 이력으로 저장할 수 있다.
  - 같은 event와 같은 handler 조합은 unique constraint로 중복 저장을 차단한다.
- 기존 ai-service Flyway V1 DDL에 두 테이블과 index/unique constraint를 추가했다.
- 기존 persistence enabled context test와 migration validation test를 9개 AI 소유 entity/table 기준으로 보강했다.
- `AiJsonStorageGuard`를 outbox payload에도 재사용하고, event payload 금지 필드 목록과 정렬되도록 차단 필드를 확장했다.

## 3. 생성/수정 파일

- `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-outbox-skeleton.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-outbox-skeleton_report.md`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiEventOutbox.java`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiEventOutboxStatus.java`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiEventOutboxRepository.java`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiProcessedEvent.java`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiProcessedEventStatus.java`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiProcessedEventRepository.java`
- `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiJsonStorageGuard.java`
- `qtai-server/ai-service/src/main/resources/db/migration/V1__create_ai_owned_tables.sql`
- `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiServiceEventOutboxPersistenceTest.java`
- `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiServicePersistenceEnabledContextTest.java`
- `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiServiceMigrationValidationTest.java`

## 4. 제외 범위

- Kafka 의존성 추가
- topic 생성
- producer/consumer 구현
- relay worker 구현
- event handler 구현
- broker publish
- 기존 UseCase transaction에 outbox append 연결
- 운영 데이터 이관
- 기존 HTTP client 제거

## 5. 검증 결과

- `.\gradlew.bat :ai-service:compileJava`: 통과
- `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiServiceEventOutboxPersistenceTest --tests com.qtai.domain.ai.internal.AiServicePersistenceEnabledContextTest --tests com.qtai.domain.ai.internal.AiServiceMigrationValidationTest`: 통과
- `git diff --check`: 통과
- placeholder 문구 검색: 매칭 없음
- 금지 데이터/민감 예시 문구 검색: 기존 event contract fixture의 금지 필드명 카탈로그와 payload guard 정책 문자열은 정책 검증용으로 존재한다. 신규 저장 데이터나 응답 본문은 추가하지 않았다.

## 6. 후속 작업

- 실제 UseCase transaction에 outbox append 연결
- outbox relay worker skeleton
- processed event cleanup 정책
- broker live smoke readiness
