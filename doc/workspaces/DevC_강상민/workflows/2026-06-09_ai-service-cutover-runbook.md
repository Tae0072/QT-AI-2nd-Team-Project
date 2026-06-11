# Workflow - 2026-06-09 ai-service-cutover-runbook

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-cutover-runbook` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `docs/ai-service-cutover-runbook` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | ai-service readiness checklist와 gateway route skeleton이 머지되어 실제 전환 실행 순서를 팀 문서로 고정해야 한다. |
| 기준 문서 | `2026-06-09_ai-service-cutover-readiness-checklist.md`, `2026-06-09_gateway-ai-route-transition-skeleton.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service` 전환 실행자가 그대로 따라갈 수 있는 runbook을 작성한다. readiness checklist가 전환 조건을 확인하는 문서라면, 이번 runbook은 dry-run, live cutover, rollback, post-cutover 확인 절차를 시간 순서대로 고정하는 문서다.

이번 작업은 문서 전용이다. 실제 gateway route 활성화, provider live 호출, 운영 DB migration 적용, 비밀 값/JWKS 구현, monolith AI 코드 삭제는 하지 않는다.

## 범위

- `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**` 전환 runbook을 작성한다.
- dry-run mode와 live cutover mode를 분리한다.
- 담당 역할, pre-flight gate, cutover 순서, rollback 순서, failure matrix, post-cutover 확인 항목을 문서화한다.
- 기존 cutover readiness checklist에 runbook 참조를 추가한다.

## 제외 범위

- gateway route enable
- provider endpoint live smoke 실행
- 운영 DB migration 적용
- 운영 비밀 값/JWKS 구현
- Docker/K8s 배포 설정
- monolith AI controller/usecase/entity 삭제
- 코드, 테스트, OpenAPI 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-service-cutover-runbook.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-service-cutover-runbook.md` | 팀 실행용 runbook |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-cutover-runbook_report.md` | 검증 결과 |
| Modify | `doc/workspaces/DevC_강상민/2026-06-09_ai-service-cutover-readiness-checklist.md` | runbook 참조 |

## 구현 순서

1. `dev` 최신화 후 `docs/ai-service-cutover-runbook` 브랜치를 생성한다.
2. workflow 문서를 작성한다.
3. cutover runbook 본문을 작성한다.
4. 기존 readiness checklist에 runbook 참조를 추가한다.
5. report 문서를 작성한다.
6. 문서 placeholder, trailing whitespace, 기준 산출물 존재 여부를 검증한다.
7. 지정 파일만 stage한다.
8. `docs(ai): ai-service cutover runbook 작성` 커밋을 생성한다.

## 수용 기준

- [ ] runbook이 dry-run과 live cutover를 분리한다.
- [ ] runbook이 pre-flight, cutover, rollback, post-cutover 순서를 포함한다.
- [ ] runbook이 route id/property와 대상 endpoint를 명시한다.
- [ ] runbook이 failure matrix와 rollback 기준을 포함한다.
- [ ] 실제 gateway/provider/DB/code 변경이 없다.
- [ ] placeholder 문구와 trailing whitespace가 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 문서 4개가 같은 cutover 절차를 공유한다.
- 작업 범위가 문서 전용으로 좁다.
- 직접 작성이 readiness checklist와 gateway skeleton 용어 일관성을 유지하기 쉽다.

## 검증 계획

```powershell
git diff --check
Test-Path "doc\workspaces\DevC_강상민\2026-06-09_ai-service-cutover-readiness-checklist.md"
Test-Path "doc\workspaces\DevC_강상민\workflows\2026-06-09_gateway-ai-route-transition-skeleton.md"
Test-Path "qtai-server\service-gateway\src\main\java\com\qtai\gateway\config\AiServiceRouteConfiguration.java"
$placeholderPattern = ("TB" + "D") + "|" + ("TO" + "DO") + "|" + ("추후 " + "정리") + "|" + ("나중에 " + "정리") + "|" + ("미" + "정")
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-cutover-runbook.md" `
  "doc\workspaces\DevC_강상민\2026-06-09_ai-service-cutover-runbook.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-cutover-runbook_report.md"
```

## 다음 작업으로 넘길 항목

- provider endpoint open 후 live smoke 연결
- 운영 DB 이관 절차 실행
- gateway AI route enable PR
- route 전환 후 monolith AI 제거 계획
