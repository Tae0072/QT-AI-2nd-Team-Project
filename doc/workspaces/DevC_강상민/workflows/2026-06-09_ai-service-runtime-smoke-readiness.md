# Workflow - 2026-06-09 ai-service-runtime-smoke-readiness

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-runtime-smoke-readiness` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `test/ai-service-runtime-smoke-readiness` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | `ai-service` module, inbound API, outbound client, DB ownership, migration, usecase persistence skeleton이 머지되어 독립 앱 실행 준비 상태를 확인해야 한다. |
| 기준 문서 | `2026-06-09_ai-service-usecase-persistence-skeleton.md`, `qtai-server/apis/ai-service/openapi.yaml` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service`가 코드상 분리만 된 상태가 아니라 embedded web server 기준으로 독립 앱처럼 기동 가능한지 검증한다. smoke는 provider live endpoint 없이 mock client mode와 H2 persistence로 실행한다.

이번 작업은 후속 gateway/provider 연결 작업자가 참고할 수 있는 실행 방법과 성공 기준을 문서화하는 것이 목적이다. 실제 gateway route 전환, provider live 연결, 운영 DB 연결은 하지 않는다.

## 범위

- `qtai.ai.inbound.enabled=true`, `qtai.ai.persistence.enabled=true`, `qtai.ai.client.mode=mock` 조건으로 ai-service를 기동하는 runtime smoke 테스트를 추가한다.
- H2 MySQL mode를 사용해 외부 DB 없이 persistence context를 띄운다.
- `/actuator/health` HTTP 응답을 확인한다.
- system/admin AI 대표 endpoint mapping이 등록되는지 확인한다.
- smoke 실행 방법을 report에 기록한다.

## 제외 범위

- gateway route 실제 전환
- provider `/api/v1/system/**` live 연결
- 운영 MySQL 연결
- service-token/JWKS 구현
- monolith AI 코드 삭제
- Docker/K8s 배포 설정
- write endpoint 실제 데이터 변경을 수반하는 외부 호출

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiServiceRuntimeSmokeReadinessTest.java` | RANDOM_PORT embedded runtime smoke |
| Create | `qtai-server/ai-service/scripts/runtime-smoke-readiness.ps1` | 로컬 smoke 실행 명령 래퍼 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-runtime-smoke-readiness_report.md` | 실행 방법과 검증 결과 기록 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-service-runtime-smoke-readiness.md` | 작업 범위와 수용 기준 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-service-runtime-smoke-readiness` 브랜치를 생성한다.
2. workflow 문서를 저장한다.
3. `AiServiceRuntimeSmokeReadinessTest`를 추가한다.
4. 테스트는 `SpringBootTest.WebEnvironment.RANDOM_PORT`로 실제 embedded server를 띄운다.
5. 테스트 property는 inbound/persistence enabled, mock client mode, H2 MySQL mode, Flyway disabled로 고정한다.
6. `TestRestTemplate`로 `/actuator/health`가 2xx 응답을 주는지 검증한다.
7. `RequestMappingHandlerMapping`으로 system/admin AI 대표 endpoint가 등록됐는지 검증한다.
8. 로컬 실행용 PowerShell script를 추가한다.
9. 검증 명령 실행 후 report에 결과를 기록한다.
10. 지정 파일만 stage하고 `test(ai): ai-service runtime smoke readiness 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceRuntimeSmokeReadinessTest` | embedded server 기동, actuator health, inbound mapping inventory, mock client mode, H2 persistence |

## 수용 기준

- [ ] `:ai-service:compileJava`가 통과한다.
- [ ] `:ai-service:test --tests com.qtai.ai.AiServiceRuntimeSmokeReadinessTest`가 통과한다.
- [ ] `:ai-service:test`가 통과한다.
- [ ] smoke는 provider live endpoint를 호출하지 않는다.
- [ ] smoke는 운영 DB를 요구하지 않는다.
- [ ] smoke 실행 방법이 report와 script에 남는다.
- [ ] gateway/provider/monolith 삭제 변경이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 runtime smoke 축과 문서에 집중되어 있다.
- 테스트 property, route inventory, 실행 script가 같은 성공 기준을 공유한다.
- 병렬화보다 직접 실행이 범위 초과를 줄인다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 테스트 추가, script 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiServiceRuntimeSmokeReadinessTest
.\gradlew.bat :ai-service:test
cd ..
git diff --check
$placeholderPattern = "TB" + "D|TO" + "DO|추후 " + "정리|나중에 " + "정리|미" + "정"
rg -n $placeholderPattern "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-runtime-smoke-readiness.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-runtime-smoke-readiness_report.md"
$restrictedPattern = "개역" + "개정|ES" + "V|NI" + "V|성서" + "유니온|두란" + "노|plain " + "secret|private " + "key"
rg -n $restrictedPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-runtime-smoke-readiness.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-runtime-smoke-readiness_report.md"
```

## 후속 작업으로 남길 항목

- `ai-service-cutover-readiness-checklist`
- `gateway-ai-route-transition-skeleton`
- provider endpoint open 후 live smoke 실행
