# ai-generation-worker-design Report

## 1. 작업 개요

- 작업 브랜치: `docs/ai-generation-worker-design`
- 작업 유형: 문서 전용
- 목적: 긴 AI generation workflow를 worker 경계로 분리하기 위한 설계 기준 고정
- 선행 조건:
  - provider AI input event 계약 문서가 `dev`에 반영되어 있음
  - AI event-driven 전환 설계 문서가 `dev`에 반영되어 있음
  - AI event outbox 결정 기록 문서가 `dev`에 반영되어 있음
  - AI event contract fixture가 `dev`에 반영되어 있음

## 2. 생성 문서

- `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-worker-design.md`
- `doc/workspaces/DevC_강상민/2026-06-09_ai-generation-worker-design.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-worker-design_report.md`

## 3. 반영 내용

- job 생성 HTTP 응답은 유지하고 실제 LLM generation은 worker 후보로 분리한다고 명시했다.
- worker input 후보를 `AiGenerationJobRequested`로 고정했다.
- worker output 후보를 다음 event로 고정했다.
  - `AiGenerationJobStarted`
  - `AiGenerationJobCompleted`
  - `AiGenerationJobFailed`
- generation worker 후보 책임을 정리했다.
  - `ai_generation_jobs` 상태 전이
  - QT/Bible 참조 조회
  - LLM generation 실행
  - 성공 시 `ai_generated_assets` 저장
  - 성공/실패 결과 event 후보 기록
- generation worker 비책임을 정리했다.
  - Study publish/hide 직접 호출
  - Audit write 직접 수행
  - AdminAuth 검증
  - provider event ingestion
  - validation 최종 판단
- `ProviderAiInputPrepared`는 승인 전까지 HTTP 조회를 대체하지 않는다고 명시했다.
- validation, approval, publish는 별도 worker/coordinator 후보로 분리했다.

## 4. 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- outbox table migration
- JPA entity/repository 구현
- relay worker 구현
- generation worker 구현
- DeepSeek flow 이관
- HTTP client 제거
- provider live 연결
- gateway route 활성화

## 5. 검증 결과

- `git diff --check`: 통과
- 선행 산출물 존재 확인:
  - `doc/workspaces/DevC_강상민/2026-06-09_provider-ai-input-event-contract.md`: 존재
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-event-outbox-decision-record.md`: 존재
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-event-driven-transition-design.md`: 존재
  - `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json`: 존재
- placeholder 문구 검색: 매칭 없음
- 금지 데이터/민감 예시 문구 검색: 매칭 없음
- Gradle 테스트: 문서 전용 변경이라 실행하지 않음

## 6. 후속 작업

- 설계 승인 후 `ai-generation-worker-skeleton`을 검토한다.
- outbox 저장소 구현이 먼저 필요하면 `ai-event-outbox-skeleton`을 선행한다.
- worker skeleton에서 필요한 event fixture 보강은 별도 test PR에서 진행한다.
