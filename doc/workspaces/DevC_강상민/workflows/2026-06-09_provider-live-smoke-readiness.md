# Workflow - 2026-06-09 provider-live-smoke-readiness

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `provider-live-smoke-readiness` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `test/provider-live-smoke-readiness` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | provider `/api/v1/system/**` endpoint가 열렸을 때 AI HTTP adapter live smoke를 즉시 실행할 수 있는 준비 문서와 wrapper가 필요하다. |
| 기준 문서 | `2026-06-08_ai-provider-endpoint-readiness-checklist.md`, `2026-06-08_ai-provider-smoke-test-skeleton.md`, `2026-06-09_ai-service-cutover-runbook.md` |
| 대상 경로 | `qtai-server/scripts/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

기존 `AiProviderSmokeTest` skeleton을 실제 provider 오픈 시 안전하게 실행할 수 있도록 준비한다. 이번 작업은 live 연결 자체가 아니라 실행 조건, 입력값, 중단 기준, read/write smoke 분리 원칙, opt-in wrapper를 고정하는 작업이다.

## 범위

- provider live smoke readiness 문서를 작성한다.
- `qtai-server/scripts/provider-live-smoke-readiness.ps1` wrapper를 추가한다.
- wrapper는 `QTAI_PROVIDER_SMOKE_ENABLED=true`가 없으면 provider를 호출하지 않는다.
- read smoke 대상은 QT, Bible, Admin/Auth로 제한한다.
- Study publish/hide와 Audit write smoke는 데이터 변경 위험 때문에 제외한다.

## 제외 범위

- provider Controller 구현
- provider endpoint 실제 호출
- gateway route enable
- 운영 DB migration 적용
- Kafka, topic, event dependency 추가
- monolith AI 코드 삭제

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/scripts/provider-live-smoke-readiness.ps1` | opt-in live smoke wrapper |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_provider-live-smoke-readiness.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_provider-live-smoke-readiness.md` | 실행 준비 문서 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_provider-live-smoke-readiness_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 `test/provider-live-smoke-readiness` 브랜치를 생성한다.
2. workflow 문서를 작성한다.
3. opt-in wrapper script를 추가한다.
4. provider live smoke readiness 문서를 작성한다.
5. report 문서를 작성한다.
6. compile/test/script guard를 검증한다.
7. 지정 파일만 stage하고 커밋한다.

## 수용 기준

- [ ] `AiProviderSmokeTest`는 env 미설정 기본 상태에서 skip/pass 된다.
- [ ] wrapper는 `-AllowSkip`에서 provider 호출 없이 통과한다.
- [ ] wrapper는 enabled 상태에서 필수 env 이름을 검증한다.
- [ ] readiness 문서가 read smoke 대상과 write smoke 제외 사유를 명시한다.
- [ ] 실제 provider 호출, gateway enable, 운영 DB 변경이 없다.

## Subagent Decision

Subagent 사용은 권장하지 않는다. wrapper와 문서가 같은 env 계약을 공유하므로 직접 실행이 안전하다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat compileTestJava
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiProviderSmokeTest
powershell -ExecutionPolicy Bypass -File .\scripts\provider-live-smoke-readiness.ps1 -AllowSkip
cd ..
git diff --check
$placeholderPattern = ("TB" + "D") + "|" + ("TO" + "DO") + "|" + ("추후 " + "정리") + "|" + ("나중에 " + "정리") + "|" + ("미" + "정")
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-09_provider-live-smoke-readiness.md" `
  "doc\workspaces\DevC_강상민\2026-06-09_provider-live-smoke-readiness.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-09_provider-live-smoke-readiness_report.md"
```

## 다음 작업으로 넘길 항목

- `ai-service-operational-handoff`
- provider endpoint open 후 `ai-provider-live-smoke`
- write smoke 정책과 테스트 데이터 승인
