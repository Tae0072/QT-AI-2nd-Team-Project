# Workflow - 2026-06-09 ai-service-inbound-api-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-inbound-api-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-service-inbound-api-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | `ai-service` 모듈이 outbound client만 보유한 상태라 MSA 전환 시 호출받을 AI system/admin API 경계 skeleton이 필요함 |
| 기준 문서 | `2026-06-08_ai-service-boundary-contract.md`, `2026-06-08_ai-service-extraction-skeleton.md`, `qtai-server/apis/ai-service/openapi.yaml` |
| 대상 경로 | `qtai-server/ai-service/**`, `qtai-server/apis/ai-service/openapi.yaml`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service` 모듈에 monolith의 AI inbound API 경계를 skeleton으로 복제한다. 이번 단계에서는 system/admin controller와 Java API DTO/interface 경계만 새 모듈에 올리고, 실제 DB/usecase/business logic 이관은 하지 않는다.

Controller는 `qtai.ai.inbound.enabled=true`일 때만 활성화한다. 기본값은 `false`로 유지해 현재 기본 실행, CI, 운영 기본 동작에 새 endpoint가 노출되지 않게 한다.

## 범위

- `ai-service`에 `com.qtai.domain.ai.api.admin/**`, `generation/**`, `validation/**`를 복제한다.
- `ai-service`에 monolith 전체 AI system/admin controller 경계를 복제한다.
- admin 인증 helper는 `AdminAuthClient`를 사용하게 해 `com.qtai.domain.admin.*` 직접 import를 금지한다.
- `AiServiceInboundConfiguration`으로 inbound controller/helper를 opt-in import한다.
- `qtai-server/apis/ai-service/openapi.yaml`에는 `x-ai-inbound-api-skeleton` extension으로 endpoint inventory와 opt-in 상태만 기록한다.
- context/mapping/controller skeleton 테스트를 추가한다.

## 제외 범위

- 기존 monolith AI controller/api/internal 삭제 또는 이동
- AI DB table migration
- 실제 usecase/business logic 구현
- production fake usecase bean 추가
- gateway route 전환
- service-token 발급, JWT/JWKS 구현
- full OpenAPI schema parity
- 사용자 프론트가 `ai-service`를 직접 호출하는 `/api/v1/ai/**` 전환

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/ai-service/build.gradle.kts` | validation/security-core 의존성 추가 |
| Modify | `qtai-server/ai-service/src/main/resources/application.yml` | inbound opt-in property 기본값 추가 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceInboundConfiguration.java` | inbound controller/helper 명시 import 및 opt-in 활성화 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/api/**` | AI inbound Java API interface/DTO 경계 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/web/**` | AI system/admin inbound controller skeleton |
| Modify | `qtai-server/apis/ai-service/openapi.yaml` | inbound skeleton endpoint inventory extension |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/**` | inbound disabled/enabled context 테스트 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/web/**` | system/admin controller skeleton 테스트 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-inbound-api-skeleton_report.md` | 작업 결과와 검증 결과 기록 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-service-inbound-api-skeleton.md` | 작업 범위와 실행 기준 기록 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-service-inbound-api-skeleton` 브랜치로 이동한다.
2. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
3. `ai-service` build/config에 inbound controller 실행에 필요한 최소 의존성과 opt-in property를 추가한다.
4. monolith의 AI admin/generation/validation API interface/DTO를 `ai-service`로 복제한다.
5. monolith의 AI system/admin web request/response/controller를 `ai-service`로 복제한다.
6. `AdminAiAuthentication`은 `AdminAuthClient` 기반으로 조정해 admin-service 직접 import를 제거한다.
7. `AiServiceInboundConfiguration`을 추가하고 application entrypoint에서 import한다.
8. OpenAPI extension에 skeleton endpoint inventory를 기록한다.
9. disabled/enabled context 테스트와 system/admin 대표 controller 테스트를 추가한다.
10. 검증 명령을 실행하고 report 문서에 결과를 기록한다.
11. 지정 파일만 stage하고 `feat(ai): ai-service inbound API skeleton 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceInboundDisabledContextTest` | 기본 property에서 inbound controller bean이 등록되지 않음 |
| `AiServiceInboundEnabledContextTest` | `qtai.ai.inbound.enabled=true`와 mocked usecase/admin client 조건에서 controller bean과 request mapping 등록 |
| `AiServiceSystemInboundControllerTest` | generation job, asset, validation log, validation-reference 대표 success와 unauthorized/forbidden |
| `AiServiceAdminInboundControllerTest` | asset, monitoring, batch run log, checklist 대표 mapping과 `AdminAuthClient` role 검증 흐름 |

## 수용 기준

- [ ] `:ai-service:compileJava`가 통과한다.
- [ ] `:ai-service:test`가 통과한다.
- [ ] 기존 `qtai-server` AI runtime toggle 테스트가 통과한다.
- [ ] 기본 property에서 inbound controller가 등록되지 않는다.
- [ ] opt-in property에서 system/admin inbound controller mapping이 등록된다.
- [ ] `ai-service`가 `com.qtai.domain.admin.*`, Repository, Entity를 import하지 않는다.
- [ ] 실제 인증 값, 민감 키 값은 추가하지 않는다.
- [ ] 기존 monolith AI 코드는 삭제하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- API 복제, 인증 helper 조정, conditional config, route test가 같은 경계 판단을 공유한다.
- 병렬 작업으로 나누면 monolith DTO와 ai-service controller/test 정합성이 어긋날 가능성이 크다.
- 이번 PR은 skeleton 경계를 안전하게 고정하는 작업이라 직접 실행이 더 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, inbound API/web 복제, opt-in 설정, 테스트 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest
cd ..
git diff --check
rg -n "com\.qtai\.domain\.admin|Repository|@Entity" qtai-server\ai-service\src\main\java\com\qtai\domain\ai
$restrictedPattern = "개역" + "개정|ES" + "V|NI" + "V|성서" + "유니온|두" + "란노|plain " + "secret|private " + "key"
rg -n $restrictedPattern `
  qtai-server\ai-service `
  "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-inbound-api-skeleton.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-inbound-api-skeleton_report.md"
```

## 후속 작업으로 남길 항목

- full OpenAPI schema parity는 별도 PR에서 진행한다.
- 실제 AI usecase/internal/DB 이관은 DB 소유권 분리 PR에서 진행한다.
- gateway route 전환은 provider readiness와 service-token/JWKS 준비 후 진행한다.
- 기존 monolith AI controller 제거는 ai-service live smoke와 route 전환 후 별도 PR에서 진행한다.
