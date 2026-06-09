# Workflow - 2026-06-09 ai-event-driven-transition-design

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-event-driven-transition-design` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | ai-service MSA 분리 준비가 cutover/runbook/handoff까지 완료되어, Kafka 구현 전 event-driven 전환 후보와 HTTP 유지 대상을 설계로 고정해야 한다. |
| 기준 문서 | `2026-06-09_ai-service-operational-handoff.md`, `2026-06-09_ai-service-cutover-runbook.md`, `2026-06-09_provider-live-smoke-readiness.md`, `2026-06-08_ai-provider-endpoint-readiness-checklist.md` |
| 해당 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

ai-service MSA 분리 이후 긴 AI workflow 중 event-driven으로 전환할 후보와 HTTP로 유지할 경계를 문서로 확정한다. 이번 작업은 Kafka, topic, consumer, producer 구현이 아니라 다음 아키텍처 전환의 판단 기준과 단계적 실행 순서를 정리하는 문서 작업이다.

## 범위

- AI workflow를 HTTP 유지 대상과 event-driven 전환 후보로 분류한다.
- generation, validation, publish 후처리, audit/notification 후처리의 이벤트 후보를 정리한다.
- provider가 AI 입력 이벤트를 발행하는 구조를 검토하되, 현재 provider HTTP 계약을 제거하지 않는다.
- 이벤트 설계 원칙으로 idempotency, ordering, retry, deduplication, failure handling, observability를 정리한다.
- 단계별 후속 PR 순서를 제안한다.
- workflow 문서, 설계 문서, report 문서를 작성한다.

## 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- provider Controller 구현
- provider live endpoint 호출
- gateway route 실제 enable
- 운영 DB migration 적용
- monolith AI 코드 삭제
- 기존 HTTP client/mock 구조 제거

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-driven-transition-design.md` | 작업 범위, 실행 순서, 검증 기준 기록 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-event-driven-transition-design.md` | event-driven 전환 설계 본문 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-driven-transition-design_report.md` | 수행 결과와 검증 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `docs/ai-event-driven-transition-design` 브랜치를 생성한다.
2. 기존 operational handoff, cutover runbook, provider live smoke readiness 문서를 확인한다.
3. workflow 문서를 작성한다.
4. workflow 기준으로 설계 문서를 작성한다.
5. report 문서를 작성한다.
6. 문서 placeholder, trailing whitespace, 범위 위반 여부를 검증한다.
7. 문서 3개만 stage하고 커밋한다.

## 수용 기준

- [ ] 문서 3개가 생성된다.
- [ ] 설계 문서가 HTTP 유지 대상과 event-driven 전환 후보를 분리한다.
- [ ] Kafka 구현, topic 생성, consumer/producer 구현을 이번 범위에서 제외한다고 명시한다.
- [ ] provider HTTP client/mock 구조를 유지한다고 명시한다.
- [ ] gateway enable, provider live 호출, 운영 DB 적용, monolith AI 삭제를 하지 않는다.
- [ ] 다음 PR 순서가 제안된다.
- [ ] placeholder 문구와 trailing whitespace가 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 문서 3개에 집중되어 병렬화 이점이 작다.
- HTTP 유지와 event 전환 경계 판단이 한 문서 안에서 일관되어야 한다.
- Kafka 구현 금지 범위를 문서 전체에 동일하게 적용해야 한다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 설계 문서, report를 직접 작성하고 최종 검증까지 수행한다.

## 검증 계획

- 문서 전용 변경이므로 Gradle 테스트는 실행하지 않는다.
- `git diff --check`
- 참조 문서 존재 확인:
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-service-operational-handoff.md`
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-service-cutover-runbook.md`
  - `doc/workspaces/DevC_강상민/2026-06-09_provider-live-smoke-readiness.md`
- placeholder 검색:
  - 미완성 표시, 임시 작업 표시, 모호한 후속 정리 표현이 남아 있지 않은지 확인한다.

## 후속 작업으로 남길 항목

- event-driven 전환 ADR 작성 여부 결정
- provider 입력 이벤트 계약 초안 작성
- outbox/processed event 저장소 설계
- Kafka 또는 대체 메시징 기술 선택 검토
- 설계 승인 이후 consumer/producer skeleton 작성
