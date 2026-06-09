# Workflow - 2026-06-09 ai-service-operational-handoff

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-operational-handoff` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `docs/ai-service-operational-handoff` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | ai-service MSA 분리 준비가 provider live smoke readiness까지 완료되어, 다음 담당자가 운영 전환 준비 상태를 한 문서로 이어받을 필요가 있다. |
| 기준 문서 | `2026-06-09_ai-service-cutover-runbook.md`, `2026-06-09_provider-live-smoke-readiness.md`, `2026-06-09_ai-service-cutover-readiness-checklist.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

AI, provider, gateway, DB, QA/Lead 담당자가 현재 ai-service MSA 분리 상태를 바로 이어받을 수 있도록 operational handoff 문서를 작성한다. 이 문서는 실행 명령, 환경 변수, route flag, DB 소유권, 남은 차단 조건, 담당자별 다음 액션을 한 곳에 모은다.

이번 작업은 문서 전용이다. provider live 호출, gateway route enable, 운영 DB migration 적용, Kafka 구현, monolith AI 삭제는 하지 않는다.

## 범위

- 지금까지 완료된 ai-service MSA 분리 산출물을 상태표로 정리한다.
- 주요 참조 문서와 실행 명령을 정리한다.
- ai-service, provider smoke, gateway route, DB 관련 env var 이름을 모은다.
- AI 소유 DB와 비소유 DB를 분리해서 기록한다.
- 담당자별 다음 액션과 아직 하면 안 되는 작업을 명확히 기록한다.
- 다음 설계 작업으로 `ai-event-driven-transition-design`을 지정한다.

## 제외 범위

- 코드 변경
- 테스트 변경
- provider endpoint live smoke
- gateway route enable
- 운영 DB migration 적용
- Kafka, topic, event dependency 추가
- monolith AI controller/usecase/entity 삭제

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-service-operational-handoff.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-service-operational-handoff.md` | 운영 인수인계 문서 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-operational-handoff_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 `docs/ai-service-operational-handoff` 브랜치를 생성한다.
2. workflow 문서를 작성한다.
3. operational handoff 본문을 작성한다.
4. report 문서를 작성한다.
5. 기준 산출물 경로, placeholder, trailing whitespace를 검증한다.
6. 지정 파일만 stage한다.
7. `docs(ai): ai-service operational handoff 작성` 커밋을 생성한다.

## 수용 기준

- [ ] 완료 상태와 남은 차단 조건이 한 문서에 정리된다.
- [ ] 실행/검증 명령과 env var 이름이 실제 산출물과 일치한다.
- [ ] DB 소유/비소유 테이블이 분리되어 있다.
- [ ] 담당자별 다음 액션이 명확하다.
- [ ] gateway enable, provider live 호출, 운영 DB 적용, Kafka 구현, monolith AI 삭제를 하지 않는다.

## Subagent Decision

Subagent 사용은 권장하지 않는다. 문서 3개가 같은 상태표와 인수인계 기준을 공유하므로 직접 실행이 안전하다.

## 검증 계획

```powershell
git diff --check
Test-Path "doc\workspaces\DevC_강상민\2026-06-09_ai-service-cutover-runbook.md"
Test-Path "doc\workspaces\DevC_강상민\2026-06-09_provider-live-smoke-readiness.md"
Test-Path "doc\workspaces\DevC_강상민\2026-06-09_ai-service-cutover-readiness-checklist.md"
Test-Path "qtai-server\scripts\provider-live-smoke-readiness.ps1"
Test-Path "qtai-server\ai-service\scripts\runtime-smoke-readiness.ps1"
$placeholderPattern = ("TB" + "D") + "|" + ("TO" + "DO") + "|" + ("추후 " + "정리") + "|" + ("나중에 " + "정리") + "|" + ("미" + "정")
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-operational-handoff.md" `
  "doc\workspaces\DevC_강상민\2026-06-09_ai-service-operational-handoff.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-operational-handoff_report.md"
```

## 다음 작업으로 넘길 항목

- `ai-event-driven-transition-design`
- provider endpoint open 후 `ai-provider-live-smoke`
- gateway route enable 실행 PR
