# Workflow - 2026-06-09 ai-generation-worker-design

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-generation-worker-design` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | event-driven 전환 설계, outbox 결정 기록, provider AI input event 계약이 준비되어 긴 AI generation workflow를 worker 경계로 분리할 기준을 문서화해야 한다. |
| 기준 문서 | `2026-06-09_ai-event-driven-transition-design.md`, `2026-06-09_ai-event-outbox-decision-record.md`, `2026-06-09_provider-ai-input-event-contract.md` |
| 해당 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

AI generation 작업을 HTTP request thread에서 분리할 때 어떤 부분을 worker 후보로 둘지 설계 문서로 고정한다. 이번 작업은 설계 전용이며 Kafka, topic, consumer, producer, outbox table, worker 구현, DeepSeek flow 이관은 수행하지 않는다.

## 범위

- job 생성 HTTP 응답은 유지하고 실제 긴 LLM generation을 worker 후보로 분리한다.
- worker input 후보를 `AiGenerationJobRequested`로 고정한다.
- worker output 후보를 `AiGenerationJobStarted`, `AiGenerationJobCompleted`, `AiGenerationJobFailed`로 고정한다.
- worker 책임과 비책임을 분리한다.
- QT/Bible 참조 조회는 provider event 합의 전까지 기존 HTTP client 계약을 유지한다고 명시한다.
- validation, approval, publish는 별도 worker/coordinator 후보로 분리한다.
- workflow 문서, 설계 문서, report 문서를 작성한다.

## 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- outbox table migration
- outbox JPA entity/repository 구현
- relay worker 구현
- generation worker 구현
- DeepSeek 실행 flow 이관
- 기존 HTTP client 제거
- provider live 호출

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-worker-design.md` | 작업 범위와 검증 계획 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-generation-worker-design.md` | AI generation worker 전환 설계 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-worker-design_report.md` | 수행 결과와 검증 결과 |

## 구현 순서

1. `dev`를 최신화한다.
2. provider AI input event 계약, event-driven 전환 설계, outbox 결정 기록, event fixture가 dev에 있는지 확인한다.
3. `docs/ai-generation-worker-design` 브랜치를 생성한다.
4. workflow 문서를 작성한다.
5. AI generation worker 설계 문서를 작성한다.
6. report 문서를 작성한다.
7. placeholder, 금지 데이터/민감 예시 문구, trailing whitespace를 검증한다.
8. 문서 3개만 stage하고 커밋한다.

## 수용 기준

- [ ] 문서 3개가 생성된다.
- [ ] `AiGenerationJobRequested`가 worker input 후보로 명시되어 있다.
- [ ] `AiGenerationJobStarted`, `AiGenerationJobCompleted`, `AiGenerationJobFailed`가 worker output 후보로 명시되어 있다.
- [ ] `ai_generation_jobs` 상태 전이 기준이 기록되어 있다.
- [ ] 성공 시 `ai_generated_assets` 저장 책임이 worker 후보 책임으로 기록되어 있다.
- [ ] QT/Bible 참조 조회는 provider event 합의 전까지 HTTP client 계약을 유지한다고 기록되어 있다.
- [ ] Study publish/hide, Audit write, AdminAuth 검증은 generation worker 책임이 아니라고 기록되어 있다.
- [ ] event payload/log에 provider raw response, prompt, 본문 원문, 인증 값, DB 접속 값을 저장하지 않는다고 기록되어 있다.
- [ ] Kafka, topic, consumer, producer, actual worker 구현은 제외되어 있다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 설계 문서, outbox 결정, event fixture, provider input event 계약이 같은 경계 판단을 공유한다.
- 변경 범위가 문서 3개에 집중되어 병렬화 이점이 작다.
- worker 책임과 제외 범위를 한 문서 맥락에서 일관되게 다뤄야 한다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow, 설계 문서, report를 직접 작성하고 검증한다.

## 검증 계획

- 문서 전용 변경이므로 Gradle 테스트는 실행하지 않는다.
- `git diff --check`
- 선행 산출물 존재 확인:
  - `doc/workspaces/DevC_강상민/2026-06-09_provider-ai-input-event-contract.md`
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-event-outbox-decision-record.md`
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-event-driven-transition-design.md`
  - `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json`
- placeholder 검색으로 미완성 표현이 남아 있지 않은지 확인한다.
- 금지 데이터/민감 예시 문구 검색으로 문서에 허용되지 않는 내용이 없는지 확인한다.

## 후속 작업으로 남길 항목

- `ai-generation-worker-skeleton`
- 선행 구현이 필요할 경우 `ai-event-outbox-skeleton`
- 설계 승인 후 event fixture 보강
