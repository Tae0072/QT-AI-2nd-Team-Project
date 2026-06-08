# Workflow - 2026-06-08 ai-http-client-runtime-toggle-verification

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-http-client-runtime-toggle-verification` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `test/ai-http-client-runtime-toggle` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | provider endpoint가 아직 열리지 않은 상태에서 AI HTTP client adapter가 설정값으로 mock/http 구현체를 정확히 전환하는지 Spring context 수준으로 검증해야 함 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `07_요구사항_정의서.md`, `18_코드_품질_게이트.md`, `2026-06-08_ai-http-client-adapter-foundation.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/client/http/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`qtai.ai.client.mode` 설정에 따라 AI outbound client 구현체가 mock 또는 HTTP adapter로 정확히 전환되는지 검증한다. 상대 provider endpoint가 아직 열리지 않았으므로 실제 HTTP 통합 호출은 하지 않고, 최소 Spring context에서 bean 등록 조건과 fail-fast 조건만 검증한다.

## 범위

- `ApplicationContextRunner` 기반 runtime toggle 테스트를 추가한다.
- `test` profile + mode 미지정 시 mock 5종이 등록되고 HTTP adapter가 등록되지 않는지 검증한다.
- `test` profile + `mode=mock` 시 mock 5종이 등록되는지 검증한다.
- `test` profile + `mode=http` + token/base-url 완비 시 HTTP adapter 5종이 등록되고 mock이 등록되지 않는지 검증한다.
- `prod` profile + mode 미지정 시 mock과 HTTP adapter가 모두 등록되지 않는지 검증한다.
- `mode=http`에서 service-token 또는 base-url 누락 시 context가 fail-fast 되는지 검증한다.

## 제외 범위

- 실제 provider endpoint 호출
- provider Controller 구현
- AI business flow를 HTTP adapter로 강제 전환
- 서비스 토큰 발급, JWKS, mTLS 구현
- retry/circuit breaker 라이브러리 도입
- gateway, Docker, Kubernetes 변경
- DB schema, migration, seed 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/client/http/AiHttpClientRuntimeToggleTest.java` | Spring context 기반 mock/http bean 전환 조건 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-http-client-runtime-toggle-verification_report.md` | 작업 결과와 검증 결과 기록 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-http-client-runtime-toggle-verification.md` | 작업 범위와 실행 기준 기록 |

## 구현 순서

1. `dev` 최신화 후 `test/ai-http-client-runtime-toggle` 브랜치에서 작업한다.
2. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
3. `AiHttpClientRuntimeToggleTest`를 추가한다.
4. `ApplicationContextRunner`에 `AiClientConfiguration`, `JacksonConfig`, mock 5종, HTTP adapter 5종을 등록한다.
5. profile/property 조합별 bean 등록 조건을 검증한다.
6. fail-fast 케이스에서 context startup failure가 발생하는지 검증한다.
7. 검증 명령 실행 후 report 문서에 결과를 기록한다.
8. 변경 파일을 stage하고 `test(ai): HTTP client 런타임 전환 조건 검증`으로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiHttpClientRuntimeToggleTest` | 기본/test profile에서 mock 5종 등록, HTTP adapter 미등록 |
| `AiHttpClientRuntimeToggleTest` | `mode=mock`에서 mock 5종 등록 |
| `AiHttpClientRuntimeToggleTest` | `mode=http`와 설정 완비 시 HTTP adapter 5종 등록, mock 미등록 |
| `AiHttpClientRuntimeToggleTest` | `prod` profile 기본값에서 mock/HTTP adapter 미등록 |
| `AiHttpClientRuntimeToggleTest` | `mode=http`에서 service-token 또는 base-url 누락 시 fail-fast |

## 수용 기준

- [ ] 실제 provider endpoint나 네트워크를 사용하지 않는다.
- [ ] 기본/test profile에서는 기존 mock 기반 개발 흐름이 유지된다.
- [ ] `mode=http`에서는 HTTP adapter가 실제 bean으로 선택된다.
- [ ] `prod` profile에서 mock이 실수로 등록되지 않는다.
- [ ] HTTP mode 설정 누락은 context 단계에서 실패한다.
- [ ] production code 변경 없이 테스트만으로 검증한다. 단, 테스트가 실제 결함을 드러내면 AI client bean 조건/설정 범위 안에서만 최소 수정한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 테스트 파일과 문서에 집중되어 있다.
- mock/http adapter bean 조건을 같은 테스트 context에서 함께 검증해야 하므로 직접 실행이 더 안전하다.
- provider endpoint 통합이나 병렬 구현 작업이 없다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 테스트 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat compileJava
.\gradlew.bat test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest
```

```powershell
git diff --check
```

## 후속 작업으로 남길 항목

- provider endpoint가 실제로 열리면 `ai-http-client-provider-integration` 작업에서 base-url 기반 실제 연결 smoke test를 수행한다.
- 운영용 서비스 토큰/JWKS, retry/circuit breaker, 배포 인프라는 별도 MSA 단계에서 다룬다.
