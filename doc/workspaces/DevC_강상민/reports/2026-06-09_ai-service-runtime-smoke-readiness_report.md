# Report - 2026-06-09 ai-service-runtime-smoke-readiness

## 작업 요약

`ai-service`가 provider live endpoint 없이 독립 앱처럼 기동 가능한지 확인하는 runtime smoke readiness를 추가했다. smoke는 embedded web server, H2 MySQL mode, inbound/persistence opt-in, mock client mode로 실행된다.

이번 작업은 gateway/provider 연결 전 준비 상태 검증이며, 실제 gateway route 전환이나 provider live 호출은 수행하지 않았다.

## 변경 내용

- `AiServiceRuntimeSmokeReadinessTest`를 추가했다.
  - `SpringBootTest.WebEnvironment.RANDOM_PORT`로 embedded web server를 실제 기동한다.
  - `qtai.ai.inbound.enabled=true`
  - `qtai.ai.persistence.enabled=true`
  - `qtai.ai.client.mode=mock`
  - H2 MySQL mode datasource를 사용한다.
  - Flyway는 smoke 목적상 비활성화하고 Hibernate `create-drop`으로 기동성을 검증한다.
- `/actuator/health`가 2xx와 `UP` 상태를 반환하는지 검증했다.
- `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**` 대표 route mapping이 등록되는지 검증했다.
- outbound client 5종이 mock bean으로 등록되는지 검증했다.
- 로컬 실행 wrapper `qtai-server/ai-service/scripts/runtime-smoke-readiness.ps1`를 추가했다.

## 실행 방법

PowerShell에서 아래 명령으로 smoke만 실행할 수 있다.

```powershell
powershell -ExecutionPolicy Bypass -File .\qtai-server\ai-service\scripts\runtime-smoke-readiness.ps1
```

Gradle 직접 실행도 가능하다.

```powershell
cd qtai-server
.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiServiceRuntimeSmokeReadinessTest
```

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiServiceRuntimeSmokeReadinessTest` | 통과 |
| `powershell -ExecutionPolicy Bypass -File .\qtai-server\ai-service\scripts\runtime-smoke-readiness.ps1` | 통과 |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test` | 통과 |
| `git diff --check` | 통과 |
| placeholder 문구 검사 | 통과 |
| 금지 데이터/민감 예시 검사 | 통과 |

## 확인한 route

- `POST /api/v1/system/ai/generation-jobs`
- `POST /api/v1/system/ai/assets`
- `POST /api/v1/system/ai/validation-logs`
- `POST /api/v1/system/validation-reference-jobs`
- `GET /api/v1/system/validation-reference-jobs/{jobId}`
- `POST /api/v1/system/validation-reference-jobs/{jobId}/expire`
- `GET /api/v1/admin/ai/assets`
- `GET /api/v1/admin/ai/assets/{assetId}`
- `POST /api/v1/admin/ai/assets/{assetId}/approve`
- `POST /api/v1/admin/ai/assets/{assetId}/reject`
- `POST /api/v1/admin/ai/assets/{assetId}/hide`
- `POST /api/v1/admin/ai/assets/{assetId}/regenerate`
- `GET /api/v1/admin/ai/monitoring`
- `GET /api/v1/admin/ai/batch-run-logs`
- `GET /api/v1/admin/ai/validation-checklists`
- `POST /api/v1/admin/ai/validation-checklists`
- `POST /api/v1/admin/ai/validation-checklists/{id}/activate`
- `POST /api/v1/admin/ai/validation-checklists/{id}/retire`

## 제외 확인

- provider `/api/v1/system/**` live endpoint를 호출하지 않았다.
- gateway route를 변경하지 않았다.
- 운영 DB 연결을 추가하지 않았다.
- service-token/JWKS, Docker/K8s, monolith AI 삭제는 변경하지 않았다.

## 다음 작업

- `ai-service-cutover-readiness-checklist`
- `gateway-ai-route-transition-skeleton`
- provider endpoint open 후 live smoke 실행
