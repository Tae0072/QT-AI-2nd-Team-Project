# Report - 2026-06-08 ai-http-client-runtime-toggle-verification

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업 브랜치 | `test/ai-http-client-runtime-toggle` |
| 기준 브랜치 | `dev` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-http-client-runtime-toggle-verification.md` |
| 커밋 메시지 | `test(ai): HTTP client 런타임 전환 조건 검증` |

## 작업 요약

상대 provider endpoint가 아직 열리지 않은 상태에서 `qtai.ai.client.mode` 설정만으로 AI outbound client 구현체가 mock 또는 HTTP adapter로 정확히 전환되는지 Spring context 수준에서 검증했다. 실제 provider endpoint 호출, provider Controller 구현, AI business flow 전환은 수행하지 않았다.

## 주요 변경

- `ApplicationContextRunner` 기반 `AiHttpClientRuntimeToggleTest`를 추가했다.
- 최소 context에 `AiClientConfiguration`, `JacksonConfig`, mock client 5종, HTTP adapter 5종을 명시적으로 import했다.
- profile/property 조합별 bean 등록 결과를 검증했다.
  - `test` profile + mode 미지정: mock 5종 등록, HTTP adapter 미등록
  - `test` profile + `mode=mock`: mock 5종 등록
  - `test` profile + `mode=http` + token/base-url 완비: HTTP adapter 5종 등록, mock 미등록
  - `prod` profile + mode 미지정: mock/HTTP adapter 모두 미등록
  - `mode=http` + service-token 누락: fail-fast
  - `mode=http` + bible base-url 누락: fail-fast

## 수용 기준 결과

- [x] 실제 provider endpoint나 네트워크를 사용하지 않는다.
- [x] 기본/test profile에서는 기존 mock 기반 개발 흐름이 유지된다.
- [x] `mode=http`에서는 HTTP adapter가 실제 bean으로 선택된다.
- [x] `prod` profile에서 mock이 실수로 등록되지 않는다.
- [x] HTTP mode 설정 누락은 context 단계에서 실패한다.
- [x] production code 변경 없이 테스트만 추가했다.

## 검증 결과

```powershell
cd qtai-server
.\gradlew.bat compileJava
```

- 결과: 성공

```powershell
cd qtai-server
.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest --rerun-tasks
```

- 결과: 성공
- 비고: 멀티모듈 Gradle에서 `test --tests ...`는 `lib-common:test`에도 필터가 전파될 수 있어 서버 모듈 테스트를 명확히 지정하기 위해 `:test`를 사용했다.

```powershell
git diff --check
```

- 결과: 성공

## 제외 확인

- provider service endpoint Controller를 추가하지 않았다.
- 실제 HTTP provider 호출을 수행하지 않았다.
- DB migration/seed를 변경하지 않았다.
- 서비스 토큰 발급, JWKS, mTLS를 구현하지 않았다.
- retry/circuit breaker 라이브러리를 추가하지 않았다.
- AI business flow를 HTTP adapter로 강제 전환하지 않았다.

## 후속 작업

- provider endpoint가 실제로 열리면 `ai-http-client-provider-integration` 작업에서 base-url 기반 실제 연결 smoke test를 수행한다.
- 운영용 서비스 토큰/JWKS, retry/circuit breaker, 배포 인프라 설정은 MSA 인프라 단계에서 별도 작업으로 진행한다.
