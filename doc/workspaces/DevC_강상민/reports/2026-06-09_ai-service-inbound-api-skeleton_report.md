# Report - 2026-06-09 ai-service-inbound-api-skeleton

## 개요

- 작업명: `ai-service-inbound-api-skeleton`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-service-inbound-api-skeleton.md`
- 작업 브랜치: `feature/ai-service-inbound-api-skeleton`
- PR 대상: `dev`
- 실행 방식: 직접 실행
- 관련 F-ID: F-02, F-14, F-15
- 목적: `ai-service` 모듈에 monolith AI system/admin inbound API 경계 skeleton 추가

## 변경 요약

- `ai-service`에 AI admin/generation/validation Java API interface/DTO 경계를 복제했다.
- `ai-service`에 AI system/admin web controller, request/response DTO, exception mapping helper를 복제했다.
- `AiServiceInboundConfiguration`을 추가해 `qtai.ai.inbound.enabled=true`일 때만 inbound controller를 명시 import하게 했다.
- 기본값은 `qtai.ai.inbound.enabled=false`로 두어 기존 기본 실행에서는 inbound controller가 등록되지 않게 했다.
- admin 인증 helper는 admin-service Java usecase 직접 import 없이 `AdminAuthClient.verifyAnyRole(...)` 기반으로 조정했다.
- `qtai-server/apis/ai-service/openapi.yaml`에 `x-ai-inbound-api-skeleton` extension으로 endpoint inventory와 opt-in 정책을 기록했다.

## 제외 범위 준수

- 기존 monolith AI controller/api/internal 삭제 또는 이동 없음
- AI DB table migration 없음
- 실제 usecase/internal/business logic 구현 없음
- production fake usecase bean 추가 없음
- gateway route 전환 없음
- service-token 발급, JWT/JWKS 구현 없음
- full OpenAPI schema parity 없음
- 사용자 프론트가 `ai-service`를 직접 호출하는 전환 없음

## 테스트 보강

- `AiServiceInboundDisabledContextTest`
  - 기본 property에서 inbound controller bean이 등록되지 않음을 검증했다.
- `AiServiceInboundEnabledContextTest`
  - opt-in property와 mocked usecase 조건에서 inbound controller bean과 request mapping inventory가 등록됨을 검증했다.
- `AiServiceSystemInboundControllerTest`
  - system generation job, asset, validation log, validation-reference 대표 endpoint와 system authority 실패를 검증했다.
- `AiServiceAdminInboundControllerTest`
  - admin asset, monitoring, batch run log, checklist 대표 endpoint와 `AdminAuthClient` 기반 role 검증 흐름을 검증했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | PASS |
| `.\gradlew.bat :ai-service:test` | PASS |
| `.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest` | PASS |
| `git diff --check` | PASS |
| ai-service 금지 import 검색 | PASS: 매치 없음 |
| ai-service 및 신규 문서 금지 키워드 검색 | PASS: 매치 없음 |
| workflow placeholder 검색 | PASS: 매치 없음 |

## 보정 사항

- 최초 admin controller 테스트에 짧은 JSON text block 문법 오류가 있어 일반 JSON 문자열로 수정했다.
- enabled context 테스트에서 actuator mapping bean과 MVC mapping bean이 함께 잡혀 `requestMappingHandlerMapping` qualifier를 지정했다.
- 문서 전체 경로를 대상으로 한 금지 키워드 검색은 과거 문서의 검증 명령 예시까지 매칭하므로, 이번 변경 파일과 `ai-service` 범위로 스코프를 고정했다.

## 후속 작업

- full OpenAPI schema parity는 별도 PR에서 진행한다.
- 실제 AI usecase/internal/DB 이관은 DB 소유권 분리 PR에서 진행한다.
- gateway route 전환은 provider readiness와 service-token/JWKS 준비 후 진행한다.
- 기존 monolith AI controller 제거는 ai-service live smoke와 route 전환 후 별도 PR에서 진행한다.
