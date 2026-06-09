# Workflow - 2026-06-08 ai-provider-smoke-test-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-provider-smoke-test-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `test/ai-provider-smoke-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | provider `/api/v1/system/**` endpoint가 열렸을 때 AI HTTP adapter로 즉시 연결 확인할 수 있는 opt-in smoke test 골격이 필요함 |
| 기준 문서 | `2026-06-08_ai-provider-endpoint-readiness.md`, `2026-06-08_ai-http-client-adapter-foundation.md`, `2026-06-08_ai-http-client-runtime-toggle-verification.md`, `2026-06-08_ai-http-client-contract-fixtures.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/client/http/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

실제 provider endpoint가 아직 열리지 않은 상태에서도 기본 CI를 깨지 않는 smoke test skeleton을 추가한다. endpoint가 준비된 날에는 환경 변수만 주입해 AI HTTP adapter가 실제 `/api/v1/system/**` provider endpoint를 호출하는지 확인할 수 있게 한다.

## 범위

- `AiProviderSmokeTest`를 추가하고 `QTAI_PROVIDER_SMOKE_ENABLED=true`일 때만 실행되게 한다.
- 기존 `QtContextClientHttpAdapter`, `BibleVerseClientHttpAdapter`, `AdminAuthClientHttpAdapter`를 직접 생성해 read 계열 endpoint만 호출한다.
- QT context, Today QT status, Bible single/batch/range, Admin/Auth active/verify/verify-any 호출 결과의 핵심 필드를 검증한다.
- service token, provider base-url, smoke 입력 ID/date/role은 환경 변수로 받는다.
- workflow와 report에 실행 방법, 제외 범위, 후속 write smoke 기준을 남긴다.

## 제외 범위

- provider Controller 구현
- 실제 provider endpoint를 기본 CI에서 호출
- Study publish/hide, Audit log write smoke 실행
- DB schema, migration, seed 변경
- service-token 발급, JWKS, gateway, Docker, Kubernetes 변경
- production code 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-provider-smoke-test-skeleton.md` | 작업 범위와 실행 기준 기록 |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/client/http/AiProviderSmokeTest.java` | env opt-in 실제 provider read smoke test skeleton |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-provider-smoke-test-skeleton_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `test/ai-provider-smoke-skeleton` 브랜치로 이동한다.
2. 브랜치에서 `dev`를 merge하고 충돌이 없음을 확인한다.
3. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
4. `AiProviderSmokeTest`를 추가한다.
5. `@EnabledIfEnvironmentVariable`로 기본 비활성화 조건을 설정한다.
6. 환경 변수 기반 `AiClientProperties`와 smoke 입력 helper를 구성한다.
7. QT, Bible, Admin/Auth read smoke 테스트를 추가한다.
8. 검증 명령을 실행하고 report 문서에 결과를 기록한다.
9. 지정 파일만 stage하고 `test(ai): provider smoke test skeleton 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiProviderSmokeTest` | env 미설정 기본 CI에서 smoke test가 실행되지 않음 |
| `AiProviderSmokeTest` | `QTAI_PROVIDER_SMOKE_ENABLED=true`일 때 QT context와 Today QT status를 HTTP adapter로 호출 |
| `AiProviderSmokeTest` | Bible single/batch/range read endpoint를 HTTP adapter로 호출 |
| `AiProviderSmokeTest` | Admin/Auth active/verify/verify-any read endpoint를 HTTP adapter로 호출 |

## 수용 기준

- [ ] 기본 CI와 로컬 기본 테스트는 외부 네트워크를 호출하지 않는다.
- [ ] smoke test는 `QTAI_PROVIDER_SMOKE_ENABLED=true`일 때만 실제 provider를 호출한다.
- [ ] service token과 base-url 값은 코드나 문서에 실제 값으로 기록하지 않는다.
- [ ] write endpoint smoke는 이번 PR에서 실행하지 않는다.
- [ ] production code, DB, provider Controller는 변경하지 않는다.
- [ ] workflow, report, smoke test skeleton만 변경한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 smoke test 파일과 문서 두 개에 집중되어 있다.
- 환경 변수 계약과 테스트 helper를 한 흐름에서 맞춰야 하므로 직접 실행이 더 안전하다.
- provider endpoint 실제 연결은 아직 수행하지 않으므로 병렬 구현 이점이 작다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, smoke test skeleton 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat compileTestJava
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiProviderSmokeTest
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest
cd ..
git diff --check
$placeholderPattern = "TB" + "D|TO" + "DO|추" + "후 정리|나" + "중에 정리|미" + "정"
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-08_ai-provider-smoke-test-skeleton.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-08_ai-provider-smoke-test-skeleton_report.md"
```

## 후속 작업으로 남길 항목

- provider endpoint가 실제로 열리면 환경 변수를 주입해 live smoke를 실행한다.
- Study publish/hide, Audit log write smoke는 테스트 데이터와 명시적 opt-in 정책이 정해진 뒤 별도 PR에서 추가한다.
- gateway, service-token 발급, JWKS 검증은 MSA 인프라 단계에서 다룬다.
