# Report - 2026-06-09 gateway-ai-route-transition-skeleton

## 작업 요약

`service-gateway`에 `ai-service` cutover용 route skeleton을 추가했다. 기본값은 비활성이며, `QTAI_GATEWAY_AI_ROUTE_ENABLED=true`와 `GATEWAY_AI_SERVICE_URI`가 설정될 때만 `ai-service-cutover` route가 등록된다.

이번 변경은 route 전환 준비다. 실제 route enable, provider live 연결, 운영 DB 연결, service-token/JWKS 정책 변경, monolith AI 삭제는 수행하지 않았다.

## 변경 내용

- `AiServiceRouteProperties`를 추가해 `qtai.gateway.ai-service.*` 설정을 바인딩했다.
- `AiServiceRouteConfiguration`을 추가해 opt-in `RouteDefinitionLocator`를 등록했다.
- `application.yml`에 다음 기본값을 추가했다.
  - `route-enabled: ${QTAI_GATEWAY_AI_ROUTE_ENABLED:false}`
  - `uri: ${GATEWAY_AI_SERVICE_URI:}`
  - `health-path: ${GATEWAY_AI_SERVICE_HEALTH_PATH:/actuator/health}`
  - `route-order: ${QTAI_GATEWAY_AI_ROUTE_ORDER:-10}`
- route path는 다음 3개로 고정했다.
  - `/api/v1/system/ai/**`
  - `/api/v1/system/validation-reference-jobs/**`
  - `/api/v1/admin/ai/**`
- `GatewayRouteTest`, `GatewayAiRouteEnabledTest`, `GatewayAiRouteFailFastTest`로 route 등록 조건을 검증했다.
- cutover readiness checklist의 gateway section에 route id/property를 반영했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :service-gateway:compileJava` | 통과 |
| `.\gradlew.bat :service-gateway:test` | 통과 |
| `git diff --check` | 통과 |
| placeholder 문구 검색 | 통과 |

## 제외 확인

- gateway AI route를 실제로 활성화하지 않았다.
- provider endpoint를 호출하지 않았다.
- ai-service 운영 DB 연결을 추가하지 않았다.
- service-token/JWKS 정책을 변경하지 않았다.
- monolith AI 코드를 삭제하지 않았다.
- Docker/K8s 배포 설정을 변경하지 않았다.

## 다음 작업

- provider endpoint open 후 live smoke
- gateway AI route enable PR
- route 전환 후 monolith AI 제거 계획
