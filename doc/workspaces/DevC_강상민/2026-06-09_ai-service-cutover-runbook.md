# ai-service Cutover Runbook

## 1. 목적

이 문서는 monolith의 AI inbound API 트래픽을 `ai-service`로 넘길 때 실행자가 따라야 하는 순서를 고정한다. readiness checklist는 전환 가능 여부를 판단하는 문서이고, 이 runbook은 dry-run, live cutover, rollback, post-cutover 확인을 시간 순서로 수행하는 문서다.

전환 대상은 다음 route 3개 축이다.

- `/api/v1/system/ai/**`
- `/api/v1/system/validation-reference-jobs/**`
- `/api/v1/admin/ai/**`

이번 runbook은 운영 절차 문서다. 이 문서 PR에서 gateway route를 켜거나, provider를 실제 호출하거나, 운영 DB migration을 적용하거나, monolith AI 코드를 삭제하지 않는다.

## 2. 실행 모드

| 모드 | 목적 | gateway | ai-service client | DB | 실행 기준 |
| --- | --- | --- | --- | --- | --- |
| Dry-run | 전환 전 기동과 route 준비 검증 | `QTAI_GATEWAY_AI_ROUTE_ENABLED=false` | `QTAI_AI_CLIENT_MODE=mock` | H2 또는 검증용 DB | provider 미개설 상태에서도 실행 가능 |
| Live cutover | 실제 트래픽 전환 | `QTAI_GATEWAY_AI_ROUTE_ENABLED=true` | `QTAI_AI_CLIENT_MODE=http` | ai-service 운영 DB | provider, DB, gateway, rollback 승인 완료 후 실행 |

Live cutover는 아래 조건이 모두 충족될 때만 진행한다.

- ai-service runtime smoke 통과
- gateway route skeleton 테스트 통과
- provider `/api/v1/system/**` endpoint 준비 완료
- ai-service 운영 DB migration validate 통과
- rollback 담당자와 실행 권한 확정
- 관리자 화면과 system endpoint 대표 요청 smoke 케이스 확정

## 3. 역할 분담

| 역할 | 책임 |
| --- | --- |
| AI 담당 | ai-service 기동, runtime smoke, AI endpoint 대표 요청 확인 |
| Gateway 담당 | `ai-service-cutover` route enable, route order, monolith rollback route 확인 |
| Provider 담당 | QT, Bible, Study, Audit, Admin/Auth system endpoint readiness 확인 |
| DB 담당 | ai-service 운영 DB migration validate, 데이터 이관 상태, write freeze 시점 확인 |
| QA 담당 | system/admin 대표 API와 관리자 화면 회귀 확인 |
| Lead 승인자 | pre-flight gate 승인, live cutover 시작/중단/rollback 승인 |

## 4. Pre-flight Gate

| Gate | 명령/확인 | 통과 기준 |
| --- | --- | --- |
| Git 기준 | `dev` 최신 상태 확인 | cutover 관련 PR이 모두 merge됨 |
| ai-service compile | `.\gradlew.bat :ai-service:compileJava` | 성공 |
| ai-service test | `.\gradlew.bat :ai-service:test` | 성공 |
| ai-service runtime smoke | `powershell -ExecutionPolicy Bypass -File .\ai-service\scripts\runtime-smoke-readiness.ps1` | 성공 |
| gateway compile | `.\gradlew.bat :service-gateway:compileJava` | 성공 |
| gateway test | `.\gradlew.bat :service-gateway:test` | 성공 |
| DB validate | ai-service 전용 Flyway validate | 성공 |
| Provider readiness | 5개 provider 담당 확인 | 모두 승인 |
| Rollback | route disable과 monolith 복귀 절차 확인 | 담당자 승인 |

Pre-flight 중 하나라도 실패하면 live cutover를 시작하지 않는다.

## 5. Dry-run 절차

Dry-run은 provider endpoint가 늦게 열리는 상황에서도 수행할 수 있다.

1. ai-service를 mock client mode로 기동한다.
   - `QTAI_AI_INBOUND_ENABLED=true`
   - `QTAI_AI_PERSISTENCE_ENABLED=true`
   - `QTAI_AI_CLIENT_MODE=mock`
2. ai-service health를 확인한다.
   - `GET /actuator/health`
   - 응답 상태가 `UP`인지 확인한다.
3. runtime smoke를 실행한다.
   - `powershell -ExecutionPolicy Bypass -File .\ai-service\scripts\runtime-smoke-readiness.ps1`
4. gateway는 AI route disabled 상태를 유지한다.
   - `QTAI_GATEWAY_AI_ROUTE_ENABLED=false`
5. monolith route가 계속 `/api/v1/**`를 처리하는지 확인한다.
6. dry-run 결과를 cutover 작업 로그에 기록한다.

## 6. Live Cutover 준비

Live cutover 직전에는 실제 값을 문서에 남기지 않고 배포 환경 변수로만 주입한다.

| 구분 | 변수 |
| --- | --- |
| ai-service app | `AI_SERVICE_PORT` |
| ai-service inbound | `QTAI_AI_INBOUND_ENABLED=true` |
| ai-service persistence | `QTAI_AI_PERSISTENCE_ENABLED=true` |
| ai-service DB | `QTAI_AI_DB_URL`, `QTAI_AI_DB_USERNAME`, `QTAI_AI_DB_PASSWORD`, `QTAI_AI_DB_DRIVER_CLASS_NAME` |
| ai-service migration | `QTAI_AI_DB_FLYWAY_ENABLED=true`, `QTAI_AI_DB_FLYWAY_LOCATIONS` |
| ai-service client | `QTAI_AI_CLIENT_MODE=http`, `QTAI_AI_CLIENT_SERVICE_TOKEN` |
| provider base URL | `QTAI_AI_CLIENT_QT_BASE_URL`, `QTAI_AI_CLIENT_BIBLE_BASE_URL`, `QTAI_AI_CLIENT_STUDY_BASE_URL`, `QTAI_AI_CLIENT_AUDIT_BASE_URL`, `QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL` |
| gateway route | `QTAI_GATEWAY_AI_ROUTE_ENABLED=true`, `GATEWAY_AI_SERVICE_URI`, `GATEWAY_AI_SERVICE_HEALTH_PATH=/actuator/health`, `QTAI_GATEWAY_AI_ROUTE_ORDER=-10` |

## 7. Live Cutover 순서

1. DB 담당자가 ai-service 운영 DB migration validate를 완료한다.
2. Provider 담당자가 QT, Bible, Study, Audit, Admin/Auth endpoint readiness를 승인한다.
3. AI 담당자가 ai-service를 live 설정으로 기동한다.
4. AI 담당자가 ai-service 직접 health를 확인한다.
   - `GET /actuator/health`
5. AI 담당자가 ai-service 직접 smoke를 수행한다.
   - system AI 대표 요청
   - validation reference job 대표 요청
   - admin AI 대표 조회 요청
6. Gateway 담당자가 `ai-service-cutover` route를 enable한다.
   - `QTAI_GATEWAY_AI_ROUTE_ENABLED=true`
   - `GATEWAY_AI_SERVICE_URI`가 ai-service base URL을 가리키는지 확인
7. Gateway 담당자가 gateway를 재배포하거나 재시작한다.
8. QA 담당자가 gateway 경유 smoke를 수행한다.
   - `/api/v1/system/ai/**`
   - `/api/v1/system/validation-reference-jobs/**`
   - `/api/v1/admin/ai/**`
9. Lead 승인자가 15분 관찰 구간을 시작한다.
10. 관찰 구간 동안 오류 기준에 걸리지 않으면 cutover 완료로 기록한다.

## 8. Gateway Route 확인

| 항목 | 기준 |
| --- | --- |
| route id | `ai-service-cutover` |
| route enabled | `QTAI_GATEWAY_AI_ROUTE_ENABLED=true` |
| target | `GATEWAY_AI_SERVICE_URI` |
| health | `GATEWAY_AI_SERVICE_HEALTH_PATH=/actuator/health` |
| order | `QTAI_GATEWAY_AI_ROUTE_ORDER=-10` |
| circuit breaker | `aiServiceCb` |
| fallback | `forward:/__fallback` |
| path rewrite | 없음 |

## 9. Failure Matrix

| 실패 상황 | 즉시 조치 | 최종 판단 |
| --- | --- | --- |
| ai-service health 실패 | gateway route enable 금지 | 원인 확인 후 dry-run부터 재시작 |
| gateway route 누락 | route property와 배포 로그 확인 | route 등록 전까지 live cutover 중단 |
| system endpoint 401/403 | SYSTEM_BATCH 권한과 인증 header 확인 | 권한 정책 확인 전 rollback |
| admin endpoint 권한 과허용 | `ROLE_ADMIN`과 AdminAuthClient 검증 확인 | 즉시 rollback |
| DB migration validate 실패 | ai-service write 시작 금지 | DB 담당 승인 전 중단 |
| provider timeout/5xx 증가 | provider 담당 확인, circuit breaker 지표 확인 | 연속 발생 시 rollback |
| Study publish/hide 불일치 | idempotency와 read model 확인 | 데이터 영향 판단 후 rollback |
| Audit log 누락 | `SYSTEM_BATCH` actor 기록 확인 | 감사 로그 복구 전 cutover 완료 금지 |
| 관리자 화면 회귀 | QA 재현 경로 기록 | 핵심 업무 차단이면 rollback |

## 10. Rollback 순서

Rollback은 Lead 승인자 또는 장애 기준에 의해 즉시 시작할 수 있다.

1. Gateway 담당자가 AI route를 disable한다.
   - `QTAI_GATEWAY_AI_ROUTE_ENABLED=false`
2. Gateway 담당자가 gateway를 재배포하거나 재시작한다.
3. Gateway 담당자가 `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**`가 monolith로 복귀했는지 확인한다.
4. AI 담당자가 ai-service 신규 write 발생 여부를 확인한다.
5. DB 담당자가 rollback 시점 이후 데이터 reconciliation 필요 여부를 판단한다.
6. Provider 담당자가 provider side effect 발생 여부를 확인한다.
7. QA 담당자가 monolith 기준 대표 요청과 관리자 화면을 재확인한다.
8. Lead 승인자가 rollback 완료를 기록한다.

## 11. Post-cutover 확인

| 항목 | 기준 |
| --- | --- |
| System API | generation job, asset, validation log, validation reference job 대표 요청 성공 |
| Admin API | asset list/detail/review, monitoring, batch log, checklist 대표 요청 성공 |
| Audit | `actorType=SYSTEM_BATCH` 감사 로그 기록 |
| Study | publish/hide 결과가 provider read model과 일치 |
| Provider | QT/Bible/Study/Audit/Admin/Auth 호출 실패율 허용 범위 |
| Gateway | ai-service route 4xx/5xx, latency, fallback 지표 확인 |
| DB | AI 소유 7개 테이블 write/read 정상 |
| Log | 인증 값, DB 접속 값, 서명 키 값이 로그에 노출되지 않음 |

## 12. Cutover 완료 기록

완료 기록에는 다음 항목을 남긴다.

- cutover 시작/종료 시각
- 실행자와 승인자
- 적용한 route mode
- smoke 결과
- rollback 필요 여부
- provider 장애 여부
- 데이터 reconciliation 필요 여부
- 후속 PR 또는 운영 조치 항목

## 13. 이 runbook에서 하지 않는 일

- gateway route를 실제로 enable하지 않는다.
- provider endpoint를 실제로 호출하지 않는다.
- 운영 DB migration을 적용하지 않는다.
- 운영 비밀 값/JWKS 구현을 추가하지 않는다.
- Docker/K8s 설정을 변경하지 않는다.
- monolith AI 코드를 삭제하지 않는다.
