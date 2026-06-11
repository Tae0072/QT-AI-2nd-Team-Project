# ai-service Operational Handoff

## 1. 목적

이 문서는 ai-service MSA 분리 작업을 다음 담당자가 이어받기 위한 운영 인수인계 문서다. 현재 상태, 실행 명령, 환경 변수, DB 소유권, route flag, 남은 차단 조건, 담당자별 다음 액션을 한 곳에 정리한다.

이 문서는 실행 안내 문서이며, 실제 provider 호출이나 gateway route enable을 수행하지 않는다.

## 2. 현재 상태 요약

| 영역 | 상태 | 기준 산출물 |
| --- | --- | --- |
| ai-service module skeleton | 완료 | `feat(ai): ai-service 분리 skeleton 추가` |
| outbound HTTP client/mock | 완료 | HTTP adapter, mock, runtime toggle 검증 |
| inbound API skeleton | 완료 | system/admin AI inbound API skeleton |
| DB ownership skeleton | 완료 | AI 소유 entity/repository 7종 |
| DB migration skeleton | 완료 | AI 소유 table DDL과 Flyway validate 경로 |
| usecase persistence skeleton | 완료 | inbound API와 ai-service 소유 repository 연결 |
| runtime smoke readiness | 완료 | `qtai-server/ai-service/scripts/runtime-smoke-readiness.ps1` |
| cutover readiness checklist | 완료 | `2026-06-09_ai-service-cutover-readiness-checklist.md` |
| gateway route skeleton | 완료 | `ai-service-cutover` disabled route skeleton |
| cutover runbook | 완료 | `2026-06-09_ai-service-cutover-runbook.md` |
| provider live smoke readiness | 완료 | `qtai-server/scripts/provider-live-smoke-readiness.ps1` |

## 3. 주요 참조 문서

| 문서 | 용도 |
| --- | --- |
| `doc/workspaces/DevC_강상민/2026-06-09_ai-service-cutover-readiness-checklist.md` | 전환 가능 조건 확인 |
| `doc/workspaces/DevC_강상민/2026-06-09_ai-service-cutover-runbook.md` | dry-run, live cutover, rollback 실행 순서 |
| `doc/workspaces/DevC_강상민/2026-06-09_provider-live-smoke-readiness.md` | provider open 후 live smoke 준비 |
| `doc/workspaces/DevC_강상민/2026-06-08_ai-provider-endpoint-readiness-checklist.md` | provider `/api/v1/system/**` 계약 확인 |
| `doc/workspaces/DevC_강상민/workflows/2026-06-09_gateway-ai-route-transition-skeleton.md` | gateway route skeleton 기준 |

## 4. 실행/검증 명령

| 목적 | 명령 |
| --- | --- |
| ai-service compile | `cd qtai-server; .\gradlew.bat :ai-service:compileJava` |
| ai-service test | `cd qtai-server; .\gradlew.bat :ai-service:test` |
| ai-service runtime smoke | `cd qtai-server; powershell -ExecutionPolicy Bypass -File .\ai-service\scripts\runtime-smoke-readiness.ps1` |
| gateway test | `cd qtai-server; .\gradlew.bat :service-gateway:test` |
| provider smoke guard | `cd qtai-server; powershell -ExecutionPolicy Bypass -File .\scripts\provider-live-smoke-readiness.ps1 -AllowSkip` |
| provider live smoke | `cd qtai-server; powershell -ExecutionPolicy Bypass -File .\scripts\provider-live-smoke-readiness.ps1` |

provider live smoke는 provider endpoint와 모든 환경 변수가 준비된 뒤에만 실행한다.

## 5. 환경 변수 목록

값은 문서에 기록하지 않고 배포 환경이나 secret store에서만 주입한다.

### ai-service app/persistence

| 구분 | 변수 |
| --- | --- |
| App | `AI_SERVICE_PORT` |
| Inbound | `QTAI_AI_INBOUND_ENABLED` |
| Persistence | `QTAI_AI_PERSISTENCE_ENABLED` |
| DB | `QTAI_AI_DB_URL`, `QTAI_AI_DB_USERNAME`, `QTAI_AI_DB_PASSWORD`, `QTAI_AI_DB_DRIVER_CLASS_NAME` |
| DB policy | `QTAI_AI_DB_DDL_AUTO`, `QTAI_AI_DB_DIALECT` |
| Migration | `QTAI_AI_DB_FLYWAY_ENABLED`, `QTAI_AI_DB_FLYWAY_LOCATIONS` |

### outbound client/provider

| 구분 | 변수 |
| --- | --- |
| Client mode | `QTAI_AI_CLIENT_MODE` |
| Service auth | `QTAI_AI_CLIENT_SERVICE_TOKEN` |
| Timeout | `QTAI_AI_CLIENT_TIMEOUT_MS` |
| QT | `QTAI_AI_CLIENT_QT_BASE_URL` |
| Bible | `QTAI_AI_CLIENT_BIBLE_BASE_URL` |
| Study | `QTAI_AI_CLIENT_STUDY_BASE_URL` |
| Audit | `QTAI_AI_CLIENT_AUDIT_BASE_URL` |
| Admin/Auth | `QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL` |

### provider smoke

| 구분 | 변수 |
| --- | --- |
| Toggle | `QTAI_PROVIDER_SMOKE_ENABLED` |
| Timeout | `QTAI_PROVIDER_SMOKE_TIMEOUT_MS` |
| QT 입력 | `QTAI_PROVIDER_SMOKE_QT_PASSAGE_ID`, `QTAI_PROVIDER_SMOKE_QT_DATE` |
| Bible 입력 | `QTAI_PROVIDER_SMOKE_BIBLE_VERSE_ID`, `QTAI_PROVIDER_SMOKE_BIBLE_BATCH_VERSE_IDS`, `QTAI_PROVIDER_SMOKE_BIBLE_BOOK`, `QTAI_PROVIDER_SMOKE_BIBLE_CHAPTER`, `QTAI_PROVIDER_SMOKE_BIBLE_START_VERSE`, `QTAI_PROVIDER_SMOKE_BIBLE_END_VERSE` |
| Admin/Auth 입력 | `QTAI_PROVIDER_SMOKE_ADMIN_MEMBER_ID`, `QTAI_PROVIDER_SMOKE_ADMIN_ROLE`, `QTAI_PROVIDER_SMOKE_ADMIN_ROLES` |

### gateway route

| 구분 | 변수 |
| --- | --- |
| Route toggle | `QTAI_GATEWAY_AI_ROUTE_ENABLED` |
| Target | `GATEWAY_AI_SERVICE_URI` |
| Health | `GATEWAY_AI_SERVICE_HEALTH_PATH` |
| Order | `QTAI_GATEWAY_AI_ROUTE_ORDER` |

## 6. DB 소유권

### ai-service 소유

- `ai_generation_jobs`
- `ai_generated_assets`
- `ai_validation_logs`
- `ai_prompt_versions`
- `ai_validation_checklist_versions`
- `validation_reference_jobs`
- `ai_batch_run_logs`

### ai-service 비소유

- `verse_explanations`
- `qt_passages`
- `bible_verses`
- `audit_logs`
- `admin_users`
- `members`

비소유 테이블은 provider API나 client 계약을 통해 접근한다. ai-service에서 직접 DB 조회하거나 Entity/Repository를 공유하지 않는다.

## 7. 담당자별 다음 액션

| 담당 | 다음 액션 |
| --- | --- |
| AI 담당 | provider endpoint open 전까지 mock/client mode 유지, provider live smoke 준비 상태 유지 |
| Provider 담당 | QT, Bible, Study, Audit, Admin/Auth `/api/v1/system/**` endpoint readiness 완료 |
| Gateway 담당 | `ai-service-cutover` route enable 전 health, order, rollback 경로 확인 |
| DB 담당 | ai-service 운영 DB migration validate와 데이터 이관 절차 승인 |
| QA 담당 | system/admin 대표 요청과 관리자 화면 회귀 케이스 준비 |
| Lead 승인자 | provider live smoke, route enable, rollback 기준 승인 |

## 8. 아직 하면 안 되는 작업

- `QTAI_GATEWAY_AI_ROUTE_ENABLED=true` 운영 적용
- provider live smoke 무조건 실행
- 운영 DB migration 실제 적용
- Study/Audit write smoke 실행
- Kafka, topic, event dependency 추가
- monolith AI controller/usecase/entity 삭제
- provider HTTP client 제거

## 9. 남은 차단 조건

| 차단 조건 | 해소 기준 |
| --- | --- |
| provider endpoint 미개설 | provider readiness checklist 통과 |
| live smoke 미실행 | provider live smoke 성공 |
| 운영 DB 이관 미확정 | migration validate와 데이터 이관/rollback 절차 승인 |
| gateway route 미활성 | route enable PR과 rollback 경로 승인 |
| write smoke 정책 미확정 | Study/Audit 테스트 데이터와 idempotency 기준 승인 |

## 10. 다음 아키텍처 전환 계획

다음 설계 작업은 `ai-event-driven-transition-design`이다.

설계 범위는 Kafka 구현이 아니라 HTTP 유지 대상과 event 전환 후보를 분리하는 것이다.

| HTTP 유지 후보 | Event 전환 후보 |
| --- | --- |
| QT context 조회 | AI generation job 시작/완료 |
| Bible verse 조회 | validation job 완료 |
| Admin/Auth 권한 검증 | approved asset publish 후처리 |
| 관리자 화면 즉시 응답 | audit/notification 비동기 후처리 |
| provider 상태 조회 | provider가 AI 입력 이벤트를 발행하는 구조 |

Kafka, topic, consumer 구현은 설계 승인 이후에만 검토한다.

## 11. handoff 완료 기준

- 모든 담당자가 참조할 문서 경로를 알고 있다.
- provider open 전에는 mock/client mode와 guard script만 사용한다.
- gateway route enable은 별도 승인 PR로만 진행한다.
- 운영 DB migration은 DB 담당 승인 전까지 적용하지 않는다.
- event-driven 전환은 설계 PR부터 시작한다.
