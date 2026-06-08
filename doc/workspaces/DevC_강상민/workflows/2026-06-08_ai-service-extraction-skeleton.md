# Workflow - 2026-06-08 ai-service-extraction-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-extraction-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feat/ai-service-extraction-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | AI MSA 분리를 오늘 1차 완료하기 위해 AI outbound client 계약을 자체 보유한 독립 Spring Boot 모듈 skeleton이 필요함 |
| 기준 문서 | `2026-06-08_ai-service-boundary-contract.md`, `2026-06-08_ai-system-endpoint-contract-sync.md`, `2026-06-08_ai-http-client-adapter-foundation.md`, `2026-06-08_ai-provider-smoke-test-skeleton.md` |
| 대상 경로 | `qtai-server/settings.gradle.kts`, `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`qtai-server` Gradle 멀티모듈에 `:ai-service`를 추가하고 독립 Spring Boot 앱으로 compile/test 가능한 skeleton을 만든다. 이번 단계에서는 AI outbound client interface, HTTP adapter, mock client를 새 모듈에 포함해 provider endpoint가 열리면 base-url 설정만으로 연결 검증에 들어갈 수 있게 한다.

## 범위

- `:ai-service` Gradle 모듈을 추가한다.
- `AiServiceApplication`과 최소 `application.yml`을 추가한다.
- 기존 `qtai-server`의 `com.qtai.domain.ai.client` 패키지를 `ai-service`에 복사한다.
- `ai-service`에서 기본 mock mode context load, client boundary, HTTP mode fail-fast 테스트를 추가한다.
- 기존 monolith `qtai-server` AI 코드는 삭제하거나 이동하지 않는다.

## 제외 범위

- 기존 `qtai-server` AI internal, web, api 삭제 또는 이동
- AI DB table migration
- provider Controller 구현
- 실제 provider endpoint 호출
- gateway route 전환
- service-token 발급, JWKS, Docker, Kubernetes 변경
- DeepSeek 호출 또는 AI business flow를 새 서비스로 강제 전환

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/settings.gradle.kts` | `:ai-service` 모듈 등록 |
| Create | `qtai-server/ai-service/build.gradle.kts` | 독립 Spring Boot 서비스 빌드 설정 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceApplication.java` | ai-service 앱 entrypoint |
| Create | `qtai-server/ai-service/src/main/resources/application.yml` | ai-service 기본 설정 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/client/**` | AI outbound client 계약, mock, HTTP adapter |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/**` | context load, client boundary, HTTP fail-fast 테스트 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-service-extraction-skeleton_report.md` | 작업 결과와 검증 결과 기록 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-service-extraction-skeleton.md` | 작업 범위와 실행 기준 기록 |

## 구현 순서

1. `dev` 최신화 후 `feat/ai-service-extraction-skeleton` 브랜치로 이동한다.
2. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
3. `settings.gradle.kts`에 `include(":ai-service")`를 추가한다.
4. `ai-service` build file, application class, application config를 추가한다.
5. 기존 AI client 패키지를 `ai-service` main source로 복사한다.
6. `ai-service` context load, 기본 mock client boundary, HTTP mode fail-fast 테스트를 추가한다.
7. 검증 명령을 실행하고 report 문서에 결과를 기록한다.
8. 지정 파일만 stage하고 `feat(ai): ai-service 분리 skeleton 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceApplicationContextTest` | `test` profile 기본 mock mode에서 ai-service context load |
| `AiServiceClientBoundaryTest` | mock client 5종이 기본 등록되고 HTTP adapter는 기본 등록되지 않음 |
| `AiServiceHttpModeFailFastTest` | `mode=http`에서 service token 또는 base-url 누락 시 fail-fast |

## 수용 기준

- [ ] `:ai-service:compileJava`가 통과한다.
- [ ] `:ai-service:test`가 통과한다.
- [ ] 기존 `qtai-server` AI runtime toggle 테스트가 통과한다.
- [ ] `ai-service`는 다른 도메인 Entity, Repository, Service를 import하지 않는다.
- [ ] 실제 인증 값, 민감 키 값은 추가하지 않는다.
- [ ] 기존 monolith AI 코드는 삭제하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 새 모듈 생성, client 복사, 테스트, 문서가 같은 경계 판단을 공유한다.
- 병렬 작업으로 나누면 복사 범위와 테스트 설정이 어긋날 가능성이 크다.
- 이번 PR은 skeleton 기준을 작게 고정하는 작업이라 직접 실행이 더 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 모듈 생성, client 복사, 테스트 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest
cd ..
git diff --check
```

```powershell
$placeholderPattern = "TB" + "D|TO" + "DO|추" + "후 정리|나" + "중에 정리|미" + "정"
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-08_ai-service-extraction-skeleton.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-08_ai-service-extraction-skeleton_report.md"

$restrictedPattern = "개역" + "개정|ES" + "V|NI" + "V|성서" + "유니온|두" + "란노|plain " + "secret|private " + "key"
rg -n $restrictedPattern `
  "qtai-server\ai-service" `
  "doc\workspaces\DevC_강상민\workflows\2026-06-08_ai-service-extraction-skeleton.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-08_ai-service-extraction-skeleton_report.md"
```

## 후속 작업으로 남길 항목

- provider endpoint가 열리면 `ai-service` 기준 live smoke를 실행한다.
- gateway route 전환은 provider readiness와 service-token/JWKS 준비 후 별도 PR에서 진행한다.
- 중복된 AI client 코드는 전환 완료 후 `qtai-server` 잔여 제거 또는 공통 모듈 추출 여부를 결정한다.
