# ai-service Cutover Readiness Checklist

## 1. 목적

이 문서는 monolith의 AI endpoint 트래픽을 `ai-service`로 넘기기 전에 팀이 확인해야 하는 준비 조건을 고정한다.

전환 대상 endpoint는 다음 3개 축이다.

- `/api/v1/system/ai/**`
- `/api/v1/system/validation-reference-jobs/**`
- `/api/v1/admin/ai/**`

이번 checklist는 readiness 판단용이다. 실제 gateway route 활성화, provider live 호출, 운영 DB migration 적용, service-token/JWKS 구현, monolith AI 코드 삭제는 이 문서 작업 범위에 포함하지 않는다.

## 2. 현재 완료 기준

| 항목 | 확인 기준 |
| --- | --- |
| ai-service module skeleton | `:ai-service:compileJava`, `:ai-service:test` 통과 |
| inbound API skeleton | system/admin AI route inventory가 ai-service에 존재 |
| outbound client adapter/mock | 기본 mock mode, opt-in HTTP mode 전환 검증 완료 |
| runtime toggle | `qtai.ai.client.mode`에 따른 mock/http bean 전환 검증 완료 |
| DB ownership skeleton | AI 소유 entity/repository 7종이 ai-service에 존재 |
| DB migration skeleton | AI 소유 table DDL과 Flyway validate 경로 준비 |
| usecase persistence skeleton | inbound API가 opt-in persistence usecase로 동작 가능 |
| runtime smoke readiness | embedded ai-service, H2, mock client, health, mapping smoke 검증 완료 |

## 3. Cutover 대상 API

| 구분 | Method/Path | 전환 확인 |
| --- | --- | --- |
| System | `POST /api/v1/system/ai/generation-jobs` | generation job 생성 |
| System | `POST /api/v1/system/ai/assets` | generated asset 등록 |
| System | `POST /api/v1/system/ai/validation-logs` | validation log 등록 |
| System | `POST /api/v1/system/validation-reference-jobs` | validation reference job 생성 |
| System | `GET /api/v1/system/validation-reference-jobs/{jobId}` | validation reference job 조회 |
| System | `POST /api/v1/system/validation-reference-jobs/{jobId}/expire` | validation reference job 만료 |
| Admin | `GET /api/v1/admin/ai/assets` | asset 목록 |
| Admin | `GET /api/v1/admin/ai/assets/{assetId}` | asset 상세 |
| Admin | `POST /api/v1/admin/ai/assets/{assetId}/approve` | asset 승인 |
| Admin | `POST /api/v1/admin/ai/assets/{assetId}/reject` | asset 반려 |
| Admin | `POST /api/v1/admin/ai/assets/{assetId}/hide` | asset 숨김 |
| Admin | `POST /api/v1/admin/ai/assets/{assetId}/regenerate` | asset 재생성 |
| Admin | `GET /api/v1/admin/ai/monitoring` | monitoring 조회 |
| Admin | `GET /api/v1/admin/ai/batch-run-logs` | batch log 조회 |
| Admin | `GET /api/v1/admin/ai/validation-checklists` | checklist 목록 |
| Admin | `POST /api/v1/admin/ai/validation-checklists` | checklist 생성 |
| Admin | `POST /api/v1/admin/ai/validation-checklists/{id}/activate` | checklist 활성화 |
| Admin | `POST /api/v1/admin/ai/validation-checklists/{id}/retire` | checklist retire |

## 4. 환경 변수 확인

| 구분 | 변수 | 전환 전 기준 |
| --- | --- | --- |
| App | `AI_SERVICE_PORT` | gateway target과 일치 |
| Inbound | `QTAI_AI_INBOUND_ENABLED` | cutover smoke에서 `true` |
| Persistence | `QTAI_AI_PERSISTENCE_ENABLED` | cutover smoke에서 `true` |
| Persistence | `QTAI_AI_DB_URL` | ai-service 전용 DB 접속 문자열 |
| Persistence | `QTAI_AI_DB_USERNAME` | ai-service 전용 DB 계정 |
| Persistence | `QTAI_AI_DB_PASSWORD` | 값은 배포 secret으로만 주입 |
| Persistence | `QTAI_AI_DB_DRIVER_CLASS_NAME` | 운영 DB driver와 일치 |
| Persistence | `QTAI_AI_DB_DDL_AUTO` | 운영 전환에서는 자동 DDL 생성 금지 |
| Persistence | `QTAI_AI_DB_DIALECT` | 운영 DB dialect와 일치 |
| Persistence | `QTAI_AI_DB_FLYWAY_ENABLED` | migration 검증 단계에서 `true` |
| Persistence | `QTAI_AI_DB_FLYWAY_LOCATIONS` | ai-service migration 위치 |
| Client | `QTAI_AI_CLIENT_MODE` | provider 준비 전 `mock`, live 연결 시 `http` |
| Client | `QTAI_AI_CLIENT_SERVICE_TOKEN` | `http` mode에서만 필요, 값은 배포 secret으로만 주입 |
| Client | `QTAI_AI_CLIENT_TIMEOUT_MS` | provider SLA 기준으로 설정 |
| Client | `QTAI_AI_CLIENT_QT_BASE_URL` | QT provider base URL |
| Client | `QTAI_AI_CLIENT_BIBLE_BASE_URL` | Bible provider base URL |
| Client | `QTAI_AI_CLIENT_STUDY_BASE_URL` | Study provider base URL |
| Client | `QTAI_AI_CLIENT_AUDIT_BASE_URL` | Audit provider base URL |
| Client | `QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL` | Admin/Auth provider base URL |

## 5. Smoke 실행 순서

| 순서 | 명령/확인 | 성공 기준 |
| --- | --- | --- |
| 1 | `cd qtai-server` | 서버 모듈 경로 진입 |
| 2 | `.\gradlew.bat :ai-service:compileJava` | compile 성공 |
| 3 | `.\gradlew.bat :ai-service:test` | ai-service 테스트 통과 |
| 4 | `powershell -ExecutionPolicy Bypass -File .\ai-service\scripts\runtime-smoke-readiness.ps1` | runtime smoke 통과 |
| 5 | `/actuator/health` | `UP` 응답 |
| 6 | route inventory 확인 | 18개 cutover 대상 route 등록 |
| 7 | provider 미연결 상태 확인 | 기본 mock client mode 유지 |

## 6. DB 전환 전 확인

| 항목 | 기준 |
| --- | --- |
| 소유 테이블 | `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`, `ai_prompt_versions`, `ai_validation_checklist_versions`, `validation_reference_jobs`, `ai_batch_run_logs` |
| 비소유 테이블 | `verse_explanations`, `qt_passages`, `bible_verses`, `audit_logs`, `admin_users`, `members`는 ai-service가 직접 소유하지 않음 |
| migration 검증 | ai-service 전용 Flyway validate 성공 |
| 데이터 이관 | 기존 AI 데이터 이관 방식과 write freeze 시점이 합의됨 |
| 권한 경계 | `validation_reference_jobs`가 사용자 노출 API로 열리지 않음 |
| 직접 조회 금지 | ai-service가 QT, Bible, Study, Audit, Admin/Auth DB를 직접 조회하지 않음 |

## 7. Provider readiness

| Provider | Endpoint | 전환 전 기준 |
| --- | --- | --- |
| QT | `GET /api/v1/system/qt/passages/{passageId}/context` | `QtContextResult`, `cacheStatus` 제외 |
| QT | `GET /api/v1/system/qt/passages/today/status` | `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY` 지원 |
| Bible | `GET /api/v1/system/bible/verses/{verseId}` | 단건 verse 응답 |
| Bible | `POST /api/v1/system/bible/verses:batch` | batch body `{ "verseIds": [...] }` 지원 |
| Bible | `GET /api/v1/system/bible/verses` | book/chapter/startVerse/endVerse query 지원 |
| Study | `POST /api/v1/system/study/verse-explanations:publish` | `Idempotency-Key` 필수 |
| Study | `POST /api/v1/system/study/verse-explanations:hide` | `Idempotency-Key` 필수 |
| Audit | `POST /api/v1/system/audit/logs` | `actorType=SYSTEM_BATCH`, `Idempotency-Key` 필수 |
| Admin/Auth | `GET /api/v1/system/admin/auth/active` | active admin 확인 |
| Admin/Auth | `GET /api/v1/system/admin/auth/verify` | 단일 role 확인 |
| Admin/Auth | `GET /api/v1/system/admin/auth/verify-any` | 복수 role 중 하나 충족 확인 |

공통 기준:

- `/api/v1/system/**` base prefix 유지
- `Authorization: Bearer {service-token}` + `SYSTEM_BATCH` 권한
- `ApiResponse<T>` envelope 유지
- `traceparent` 전파와 응답 `traceId` 반영
- provider error envelope는 AI 쪽 `AiClientException`으로 변환

## 8. Gateway route 전환 전 확인

| 항목 | 기준 |
| --- | --- |
| route id | `ai-service-cutover` |
| route enable flag | `QTAI_GATEWAY_AI_ROUTE_ENABLED=false` 기본값 유지 |
| route target | `GATEWAY_AI_SERVICE_URI`가 ai-service base URL을 가리킴 |
| route health | `GATEWAY_AI_SERVICE_HEALTH_PATH=/actuator/health` |
| route order | `QTAI_GATEWAY_AI_ROUTE_ORDER=-10`으로 monolith catch-all보다 먼저 매칭 |
| route 대상 | `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**`만 ai-service로 보냄 |
| route 충돌 | monolith route와 우선순위 충돌 없음 |
| target URL | ai-service base URL과 health path가 배포 환경에 등록됨 |
| health check | `/actuator/health` 기반으로 route enable 판단 |
| auth 전달 | system/admin 인증 header가 ai-service까지 전달됨 |
| rollback route | 동일 path를 monolith로 되돌리는 절차가 준비됨 |
| 관측성 | trace header, status code, latency, 4xx/5xx 지표 확인 가능 |

## 9. Cutover 실행 전 최종 Gate

| Gate | 통과 기준 |
| --- | --- |
| Runtime | ai-service runtime smoke 통과 |
| DB | ai-service 전용 DB migration validate 통과 |
| Provider | QT/Bible/Study/Audit/Admin/Auth provider readiness 통과 |
| Gateway | disabled route skeleton 또는 동등한 route 설정 검토 완료 |
| Security | SYSTEM_BATCH, ROLE_ADMIN, AdminAuthClient role 검증 경로 확인 |
| Data | 금지 번역본/금지 본문/민감 값이 fixture, seed, log에 없음 |
| Rollback | monolith route 복귀 절차와 담당자가 정해짐 |

## 10. Rollback 기준

다음 중 하나라도 발생하면 AI route를 monolith로 되돌린다.

- `/actuator/health`가 `UP`이 아님
- cutover 대상 route가 누락됨
- system endpoint가 예상과 다른 401/403/5xx를 반환함
- admin endpoint에서 role 검증이 누락되거나 과하게 허용됨
- ai-service DB migration validate 실패
- provider timeout, 5xx, rate limit이 연속 발생함
- Study publish/hide 또는 Audit write의 멱등성 보장이 깨짐
- 관리자 asset review, monitoring, checklist 화면의 핵심 응답이 회귀함

## 11. 전환 후 확인

| 항목 | 기준 |
| --- | --- |
| System API | generation job, asset, validation log, validation reference job 대표 요청 성공 |
| Admin API | asset list/detail/review, monitoring, batch log, checklist 대표 요청 성공 |
| Audit | SYSTEM_BATCH 주체 감사 로그 기록 |
| Study | 승인/숨김 반영이 provider read model과 일치 |
| DB | AI 소유 7개 테이블 write/read 정상 |
| Metric | ai-service 4xx/5xx, latency, provider failure 지표 확인 |
| Log | 인증 값, DB 접속 값, 서명 키 값이 로그에 노출되지 않음 |

## 12. 이번 문서 PR에서 하지 않는 일

- gateway route 활성화
- provider live smoke 실행
- 운영 DB migration 적용
- 운영 service-token/JWKS 구현
- Docker/K8s 배포 설정
- monolith AI 코드 삭제
- AI business flow를 강제로 새 서비스로 전환
