# Workflow - 2026-06-09 gateway-ai-route-transition-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `gateway-ai-route-transition-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `chore/gateway-ai-route-transition-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | `ai-service` cutover checklist 이후 gateway가 AI inbound route를 나중에 켤 수 있는 disabled skeleton이 필요하다. |
| 기준 문서 | `2026-06-09_ai-service-cutover-readiness-checklist.md`, `service-gateway` route 설정 |
| 대상 경로 | `qtai-server/service-gateway/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`service-gateway`에 AI inbound API를 `ai-service`로 보낼 수 있는 route skeleton을 추가한다. 기본값에서는 route가 등록되지 않아 기존 `/api/v1/**` monolith Strangler route가 그대로 동작한다.

이번 작업은 전환 준비 skeleton이다. 실제 gateway route 활성화, provider live 연결, 운영 service-token/JWKS 정책 변경, monolith AI 코드 삭제는 하지 않는다.

## 범위

- `qtai.gateway.ai-service.*` property를 추가한다.
- `QTAI_GATEWAY_AI_ROUTE_ENABLED=false` 기본값에서 AI route가 등록되지 않게 한다.
- `route-enabled=true`와 `GATEWAY_AI_SERVICE_URI` 설정 시 `ai-service-cutover` route를 등록한다.
- route path는 `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**` 3개로 고정한다.
- 기존 monolith route보다 먼저 매칭되도록 order 기본값을 `-10`으로 둔다.
- 기존 fallback `/__fallback` CircuitBreaker를 재사용한다.

## 제외 범위

- 실제 route enable
- provider endpoint live smoke
- ai-service 운영 DB 연결
- service-token/JWKS 정책 변경
- Docker/K8s 배포 설정
- monolith AI controller/usecase/entity 삭제

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `service-gateway/.../config/AiServiceRouteConfiguration.java` | opt-in AI route definition |
| Create | `service-gateway/.../config/AiServiceRouteProperties.java` | route property binding |
| Modify | `service-gateway/src/main/resources/application.yml` | disabled-by-default env placeholder |
| Test | `service-gateway/src/test/java/com/qtai/gateway/*Route*Test.java` | default disabled, opt-in enabled, URI fail-fast 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_gateway-ai-route-transition-skeleton.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_gateway-ai-route-transition-skeleton_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 `chore/gateway-ai-route-transition-skeleton` 브랜치를 생성한다.
2. workflow 문서를 작성한다.
3. `AiServiceRouteProperties`와 `AiServiceRouteConfiguration`을 추가한다.
4. `application.yml`에 `qtai.gateway.ai-service` 기본 비활성 property를 추가한다.
5. gateway route 테스트를 추가/보강한다.
6. cutover checklist의 gateway section에 route id와 property를 반영한다.
7. report 문서를 작성한다.
8. 검증 명령을 실행한다.
9. 지정 파일만 stage하고 커밋한다.

## 수용 기준

- [ ] 기본값에서 `ai-service-cutover` route가 등록되지 않는다.
- [ ] opt-in 설정에서 `ai-service-cutover` route가 3개 AI path만 가진다.
- [ ] opt-in route order가 monolith route보다 앞선다.
- [ ] opt-in route가 `CircuitBreaker` filter와 `forward:/__fallback`을 가진다.
- [ ] route enabled 상태에서 target URI가 없으면 context가 fail-fast 된다.
- [ ] 기존 monolith route는 유지된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- gateway route 설정, 테스트, 문서가 같은 route id/property 계약을 공유한다.
- 변경 범위가 `service-gateway`와 DevC 문서에 집중되어 있다.
- 직접 실행이 route precedence와 기본 비활성 조건을 일관되게 검증하기 쉽다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :service-gateway:compileJava
.\gradlew.bat :service-gateway:test
cd ..
git diff --check
$placeholderPattern = ("TB" + "D") + "|" + ("TO" + "DO") + "|" + ("추후 " + "정리") + "|" + ("나중에 " + "정리") + "|" + ("미" + "정")
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-09_gateway-ai-route-transition-skeleton.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-09_gateway-ai-route-transition-skeleton_report.md"
```

## 다음 작업으로 넘길 항목

- provider endpoint open 후 live smoke
- gateway AI route enable PR
- route 전환 후 monolith AI 제거 계획
