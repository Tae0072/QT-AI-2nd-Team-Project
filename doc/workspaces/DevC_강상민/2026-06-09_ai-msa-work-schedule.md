# AI MSA 남은 작업 스케줄

> 작성자: DevC 강상민
> 기준일: 2026-06-09
> 목적: AI MSA 분리 이후 남은 작업, 선행 조건, 다음 작업 흐름을 팀원이 바로 확인할 수 있게 정리한다.

## 1. 현재 상태 요약

AI MSA 분리는 `ai-service` 물리 분리, inbound/outbound 계약, DB 소유권, runtime smoke, gateway cutover 준비, event-driven 설계, outbox/worker 기반까지 완료된 상태다. 현재 바로 이어갈 작업은 **real executor가 사용할 prompt/context 계약 고정**이며, Kafka 구현은 outbox relay skeleton 이후에 진행한다.

| 영역 | 현재 상태 | 설명 |
| --- | --- | --- |
| AI boundary / contract | ✅ 완료 | AI가 외부 서비스에 요구하는 API, provider endpoint, fixture, smoke 준비가 고정됐다. |
| outbound client | ✅ 완료 | mock/http adapter와 runtime toggle 검증이 준비됐다. |
| ai-service 물리 분리 | ✅ 완료 | 독립 모듈, inbound API, DB ownership, usecase persistence, runtime smoke, gateway skeleton과 runbook이 준비됐다. |
| event-driven 전환 설계 | ✅ 완료 | event 전환 방향, fixture, outbox 결정, provider input event, worker 설계가 정리됐다. |
| outbox / generation worker 기반 | ✅ 완료 | outbox persistence, requested event append, worker/scheduler/executor contract와 real executor skeleton/toggle이 준비됐다. |
| Kafka 도입 전 작업 | ▶️ 진행 대상 | prompt/context 계약부터 real executor 구현, outbox relay skeleton까지 진행한다. |
| Kafka 이벤트 구현 | ⏳ 대기 | outbox relay skeleton 완료 후 Kafka 의존성, topic, adapter, consumer 순서로 진행한다. |
| Provider / MSA cutover | 🔒 조건부 | provider endpoint, provider input event, gateway live cutover 준비가 끝난 뒤 진행한다. |
| 최종 정리 | 🔒 조건부 | live cutover 안정화 후 monolith write freeze, route deprecation, dashboard, final handoff를 진행한다. |

## 2. 다음 작업 흐름

1. `ai-generation-prompt-context-contract`
2. `ai-generation-deepseek-client-adapter`
3. `ai-generation-real-executor-implementation`
4. `ai-generation-real-executor-runtime-smoke`
5. `ai-event-outbox-relay-design`
6. `ai-event-outbox-relay-skeleton`
7. Kafka foundation 이후 이벤트 구현
8. provider live smoke / gateway cutover
9. monolith write freeze / final handoff

## 3. 전체 작업 스케줄 표

| 상태 | Phase | 작업명 | 한줄 설명 | 선행 조건 | 완료 기준 | 예상 브랜치 |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ 완료 | AI boundary / contract | `ai-service-boundary-contract` | AI 소유 DB와 외부 의존 경계를 표로 고정했다. | 없음 | boundary 문서와 client interface 초안 완료 | `chore/ai-service-boundary-contract` |
| ✅ 완료 | AI boundary / contract | `ai-client-contract-followup` | client 메서드명과 mock 부정 입력 계약을 정리했다. | boundary contract | 계약 테스트 통과 | `chore/ai-client-contract-followup` |
| ✅ 완료 | AI boundary / contract | `ai-system-endpoint-contract-sync` | provider `/api/v1/system/**` endpoint 계약을 AI 문서/OpenAPI 확장에 동기화했다. | endpoint 합의 | outbound system endpoint 6종 문서화 | `chore/ai-system-endpoint-contract-sync` |
| ✅ 완료 | AI boundary / contract | `provider-endpoint-readiness-checklist` | provider 구현자가 확인할 endpoint readiness checklist를 만들었다. | endpoint sync | 공통 규약과 endpoint별 확인표 완료 | `docs/ai-provider-endpoint-readiness` |
| ✅ 완료 | AI boundary / contract | `http-client-contract-fixtures` | HTTP client 계약 fixture를 테스트 리소스로 고정했다. | readiness checklist | fixture 기반 MockWebServer 테스트 통과 | `test/ai-http-client-contract-fixtures` |
| ✅ 완료 | AI boundary / contract | `provider-smoke-test-skeleton` | provider endpoint가 열릴 때 사용할 opt-in smoke skeleton을 추가했다. | fixture 정리 | 기본 CI에서 skip/pass | `test/ai-provider-smoke-skeleton` |
| ✅ 완료 | AI boundary / contract | `provider-live-smoke-readiness` | provider live smoke 실행 env와 wrapper guard를 준비했다. | provider smoke skeleton | `-AllowSkip` 실행 통과 | `test/provider-live-smoke-readiness` |
| ✅ 완료 | outbound client | `ai-http-client-adapter-foundation` | `mode=http`일 때만 HTTP adapter가 켜지는 기반을 만들었다. | endpoint contract | MockWebServer 계약 테스트 통과 | `chore/ai-http-client-adapter-foundation` |
| ✅ 완료 | outbound client | `ai-http-client-runtime-toggle-verification` | Spring context에서 mock/http client 전환 조건을 검증했다. | HTTP adapter foundation | runtime toggle 테스트 통과 | `test/ai-http-client-runtime-toggle` |
| ✅ 완료 | outbound client | `ai-provider-smoke-skeleton` | 실제 provider 호출 전 opt-in smoke test skeleton을 준비했다. | runtime toggle | env 미설정 skip/pass | `test/ai-provider-smoke-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-extraction-skeleton` | `:ai-service` 독립 모듈 skeleton과 outbound client 복사본을 만들었다. | client 기반 | `:ai-service:compileJava/test` 통과 | `feature/ai-service-extraction-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-inbound-api-skeleton` | system/admin AI inbound API 경계를 opt-in controller로 복제했다. | extraction skeleton | inbound disabled/enabled context 테스트 통과 | `feature/ai-service-inbound-api-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-db-ownership-skeleton` | AI 소유 DB entity/repository skeleton을 `ai-service`에 추가했다. | inbound skeleton | H2 repository/entity 테스트 통과 | `feature/ai-db-ownership-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-db-migration-skeleton` | AI 소유 table DDL과 Flyway validate skeleton을 준비했다. | DB ownership | migration validate 테스트 통과 | `feature/ai-service-db-migration-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-usecase-persistence-skeleton` | inbound controller가 ai-service 소유 repository 기반 usecase를 사용할 수 있게 연결했다. | DB migration | usecase persistence 테스트 통과 | `feature/ai-service-usecase-persistence-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-runtime-smoke-readiness` | 독립 ai-service 실행 smoke와 wrapper를 준비했다. | usecase persistence | runtime smoke readiness 통과 | `test/ai-service-runtime-smoke-readiness` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-cutover-readiness-checklist` | monolith에서 ai-service로 넘기기 전 확인표를 만들었다. | runtime smoke | checklist 문서 완료 | `docs/ai-service-cutover-readiness` |
| ✅ 완료 | ai-service 물리 분리 | `gateway-ai-route-transition-skeleton` | gateway AI route를 disabled 기본값으로 준비했다. | cutover checklist | gateway route 테스트 통과 | `chore/gateway-ai-route-transition-skeleton` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-cutover-runbook` | dry-run, live cutover, rollback 절차를 runbook으로 정리했다. | gateway skeleton | runbook 문서 완료 | `docs/ai-service-cutover-runbook` |
| ✅ 완료 | ai-service 물리 분리 | `ai-service-operational-handoff` | AI/provider/gateway/DB/QA 담당자별 인수인계 문서를 만들었다. | cutover runbook | handoff 문서 완료 | `docs/ai-service-operational-handoff` |
| ✅ 완료 | event-driven 설계 | `ai-event-driven-transition-design` | HTTP 유지 대상과 event 전환 후보를 분리했다. | operational handoff | event 전환 설계 문서 완료 | `docs/ai-event-driven-transition-design` |
| ✅ 완료 | event-driven 설계 | `ai-event-contract-fixtures` | AI event fixture와 계약 테스트를 추가했다. | event design | fixture 테스트 통과 | `test/ai-event-contract-fixtures` |
| ✅ 완료 | event-driven 설계 | `ai-event-outbox-decision-record` | outbox/processed event 소유권과 멱등 기준을 ADR로 고정했다. | event fixture | decision record 완료 | `docs/ai-event-outbox-decision-record` |
| ✅ 완료 | event-driven 설계 | `provider-ai-input-event-contract` | provider input event payload와 금지 저장 범위를 문서화했다. | outbox decision | provider 합의용 계약 문서 완료 | `docs/provider-ai-input-event-contract` |
| ✅ 완료 | event-driven 설계 | `ai-generation-worker-design` | 긴 generation workflow를 worker 후보로 분리하는 설계를 정리했다. | provider input contract | worker design 문서 완료 | `docs/ai-generation-worker-design` |
| ✅ 완료 | outbox / worker 기반 | `ai-event-outbox-skeleton` | `ai_event_outbox`, `ai_processed_events` persistence skeleton을 추가했다. | outbox decision | outbox repository 테스트 통과 | `feature/ai-event-outbox-skeleton` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-job-requested-outbox-append` | job 생성 transaction에 `AiGenerationJobRequested` outbox append를 연결했다. | outbox skeleton | job 생성/outbox 테스트 통과 | `feature/ai-generation-job-requested-outbox-append` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-worker-skeleton` | DB polling 기반 worker skeleton과 상태 전이를 추가했다. | worker design | worker success/failure 테스트 통과 | `feature/ai-generation-worker-skeleton` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-worker-scheduler-skeleton` | disabled 기본값의 worker scheduler skeleton을 추가했다. | worker skeleton | scheduler context 테스트 통과 | `feature/ai-generation-worker-scheduler-skeleton` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-worker-executor-contract` | worker executor job/result 계약을 코드와 테스트로 고정했다. | worker skeleton | executor contract 테스트 통과 | `feature/ai-generation-worker-executor-contract` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-worker-runtime-smoke-readiness` | fake executor + H2로 worker runtime smoke를 준비했다. | executor contract | smoke wrapper/test 통과 | `test/ai-generation-worker-runtime-smoke-readiness` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-real-executor-skeleton` | real executor를 꽂을 deepseek mode skeleton을 추가했다. | runtime smoke | skeleton config 테스트 통과 | `feature/ai-generation-real-executor-skeleton` |
| ✅ 완료 | outbox / worker 기반 | `ai-generation-real-executor-runtime-toggle` | fake executor와 real skeleton executor 전환을 검증했다. | real executor skeleton | runtime toggle 테스트 통과 | `test/ai-generation-real-executor-runtime-toggle` |
| ▶️ 다음 | Kafka 도입 전 | `ai-generation-prompt-context-contract` | real executor가 사용할 prompt 입력, provider context 조회, 저장 금지 필드 범위를 계약으로 고정한다. | real executor toggle | prompt/context 계약 문서와 테스트 fixture 기준 완료 | `docs/ai-generation-prompt-context-contract` |
| ⏳ 대기 | Kafka 도입 전 | `ai-generation-deepseek-client-adapter` | DeepSeek 호환 HTTP client adapter를 skeleton executor와 분리된 호출 계층으로 추가한다. | prompt/context contract | adapter 계약 테스트 통과 | `feature/ai-generation-deepseek-client-adapter` |
| ⏳ 대기 | Kafka 도입 전 | `ai-generation-real-executor-implementation` | worker executor가 prompt/context를 조립하고 DeepSeek adapter를 호출해 asset payload를 생성하도록 연결한다. | DeepSeek adapter | worker real executor 테스트 통과 | `feature/ai-generation-real-executor-implementation` |
| ⏳ 대기 | Kafka 도입 전 | `ai-generation-real-executor-runtime-smoke` | H2 또는 opt-in 환경에서 real executor 실행 경로를 안전하게 smoke 검증한다. | real executor implementation | opt-in smoke 통과 | `test/ai-generation-real-executor-runtime-smoke` |
| ⏳ 대기 | Kafka 도입 전 | `ai-event-outbox-relay-design` | outbox row를 외부 broker로 발행하는 relay 책임, retry, 실패 기록 정책을 설계한다. | real executor smoke | relay design 문서 완료 | `docs/ai-event-outbox-relay-design` |
| ⏳ 대기 | Kafka 도입 전 | `ai-event-outbox-relay-skeleton` | Kafka 없이 DB polling relay skeleton과 상태 전이 테스트를 추가한다. | relay design | relay skeleton 테스트 통과 | `feature/ai-event-outbox-relay-skeleton` |
| ⏳ 대기 | Kafka 이벤트 구현 | `ai-kafka-dependency-foundation` | Kafka 의존성과 설정을 disabled 기본값으로 추가한다. | relay skeleton | 기본 실행에서 Kafka 미활성 검증 | `chore/ai-kafka-dependency-foundation` |
| ⏳ 대기 | Kafka 이벤트 구현 | `ai-kafka-topic-contract` | topic 이름, key, schemaVersion, payload 금지 필드를 문서/fixture로 고정한다. | Kafka foundation | topic contract 문서와 fixture 테스트 완료 | `docs/ai-kafka-topic-contract` |
| ⏳ 대기 | Kafka 이벤트 구현 | `ai-event-outbox-relay-kafka-adapter` | outbox relay가 Kafka producer adapter를 통해 이벤트를 발행하게 한다. | topic contract | adapter 테스트 통과 | `feature/ai-event-outbox-relay-kafka-adapter` |
| ⏳ 대기 | Kafka 이벤트 구현 | `ai-event-consumer-skeleton` | ai-service event consumer skeleton과 handler 등록 경계를 만든다. | Kafka foundation | consumer disabled/enabled context 테스트 통과 | `feature/ai-event-consumer-skeleton` |
| ⏳ 대기 | Kafka 이벤트 구현 | `ai-processed-event-idempotency-verification` | `eventId + handlerName` 기준 중복 처리 차단을 검증한다. | consumer skeleton | 중복 처리 차단 테스트 통과 | `test/ai-processed-event-idempotency-verification` |
| ⏳ 대기 | Kafka 이벤트 구현 | `ai-generation-job-requested-kafka-flow-test` | job requested event가 outbox에서 Kafka flow까지 이어지는 통합 흐름을 검증한다. | relay Kafka adapter | opt-in Kafka flow 테스트 통과 | `test/ai-generation-job-requested-kafka-flow-test` |
| 🔒 조건부 | Provider / MSA cutover | `provider-system-endpoint-live-smoke` | provider `/api/v1/system/**` 실제 endpoint와 AI HTTP client 연결을 opt-in smoke로 검증한다. | provider endpoint open | live smoke read 계열 통과 | `test/provider-system-endpoint-live-smoke` |
| 🔒 조건부 | Provider / MSA cutover | `provider-ai-input-event-fixture-sync` | provider input event fixture를 provider 합의 payload와 동기화한다. | provider event 합의 | fixture 계약 테스트 통과 | `test/provider-ai-input-event-fixture-sync` |
| 🔒 조건부 | Provider / MSA cutover | `provider-ai-input-event-producer-contract-test` | provider가 `ProviderAiInputPrepared` 계약대로 event를 만들 수 있는지 계약 테스트를 준비한다. | fixture sync | producer contract 테스트 통과 | `test/provider-ai-input-event-producer-contract-test` |
| 🔒 조건부 | Provider / MSA cutover | `gateway-ai-route-live-cutover` | gateway AI route를 실제 ai-service로 넘기는 운영 전환 PR을 수행한다. | provider smoke, readiness 승인 | gateway smoke와 rollback 경로 확인 | `feature/gateway-ai-route-live-cutover` |
| 🔒 조건부 | Provider / MSA cutover | `ai-service-production-readiness-check` | 운영 환경 변수, DB, health, smoke, rollback 조건을 최종 확인한다. | gateway cutover 준비 | readiness checklist 전부 통과 | `docs/ai-service-production-readiness-check` |
| 🔒 조건부 | 최종 정리 | `monolith-ai-write-path-freeze` | monolith AI write path를 중단하거나 read-only로 고정하는 계획을 세운다. | cutover 안정화 | freeze 기준 문서와 guard 테스트 완료 | `docs/monolith-ai-write-path-freeze` |
| 🔒 조건부 | 최종 정리 | `monolith-ai-route-deprecation-plan` | monolith AI route 제거 순서와 호환 기간을 정리한다. | write path freeze | deprecation plan 완료 | `docs/monolith-ai-route-deprecation-plan` |
| 🔒 조건부 | 최종 정리 | `ai-service-observability-dashboard` | ai-service health, latency, 4xx/5xx, provider failure, worker/outbox 지표 대시보드 기준을 정한다. | production readiness | dashboard 기준 문서 완료 | `docs/ai-service-observability-dashboard` |
| 🔒 조건부 | 최종 정리 | `ai-service-kafka-runbook` | Kafka 장애, replay, DLQ/재처리, idempotency 점검 runbook을 작성한다. | Kafka adapter/consumer 준비 | runbook 완료 | `docs/ai-service-kafka-runbook` |
| 🔒 조건부 | 최종 정리 | `ai-msa-final-handoff-report` | 전체 MSA 분리 완료 상태와 남은 운영 책임을 인수인계 문서로 고정한다. | cutover 안정화, runbook 완료 | final handoff report 완료 | `docs/ai-msa-final-handoff-report` |

## 4. 지금 바로 시작할 작업

| 순서 | 작업명 | 목적 | 완료 후 다음 |
| --- | --- | --- | --- |
| 1 | `ai-generation-prompt-context-contract` | real executor가 prompt/context를 어떤 형태로 받을지 계약화한다. | DeepSeek client adapter 구현 |
| 2 | `ai-generation-deepseek-client-adapter` | 실제 LLM 호출 계층을 executor와 분리해 테스트 가능하게 만든다. | real executor implementation |
| 3 | `ai-generation-real-executor-implementation` | worker가 실제 adapter를 호출하고 asset payload를 저장하게 연결한다. | real executor runtime smoke |
| 4 | `ai-generation-real-executor-runtime-smoke` | 실제 실행 경로를 opt-in smoke로 검증한다. | outbox relay 설계 |
| 5 | `ai-event-outbox-relay-design` | Kafka 전에 relay 책임과 실패 정책을 확정한다. | relay skeleton |
| 6 | `ai-event-outbox-relay-skeleton` | Kafka 없이 DB polling relay 기반을 만든다. | Kafka foundation |

## 5. Provider/Kafka 전 대기 조건

| 구분 | 대기 조건 | 확인 방법 |
| --- | --- | --- |
| Provider endpoint | QT/Bible/Study/Audit/Admin/Auth `/api/v1/system/**` endpoint open | `provider-system-endpoint-live-smoke` |
| Provider input event | `ProviderAiInputPrepared` payload 합의 | `provider-ai-input-event-fixture-sync` |
| Kafka 도입 | relay skeleton과 idempotency 기준 완료 | `ai-event-outbox-relay-skeleton` |
| Gateway cutover | provider smoke, ai-service readiness, rollback 승인 | `gateway-ai-route-live-cutover` |
| Monolith freeze | gateway cutover 안정화와 ai-service write path 확인 | `monolith-ai-write-path-freeze` |

## 6. 표기 규칙

| 표기 | 의미 |
| --- | --- |
| ✅ 완료 | PR이 완료되어 다음 단계의 선행 조건으로 사용할 수 있는 상태 |
| ▶️ 다음 | 지금 바로 시작할 작업 |
| ⏳ 대기 | 선행 작업 완료 후 진행할 작업 |
| 🔒 조건부 | provider, gateway, 운영 준비 등 외부 조건 충족 후 진행할 작업 |

## 7. 운영 원칙

- Kafka 구현은 `ai-event-outbox-relay-skeleton` 완료 전 시작하지 않는다.
- provider live 호출은 endpoint와 service auth 준비 전 실행하지 않는다.
- gateway route는 readiness, smoke, rollback 기준이 준비된 뒤 enable한다.
- monolith AI write path 제거는 ai-service cutover 안정화 후 진행한다.
- 문서에서 실제 환경 값이나 민감 값은 기록하지 않는다.
