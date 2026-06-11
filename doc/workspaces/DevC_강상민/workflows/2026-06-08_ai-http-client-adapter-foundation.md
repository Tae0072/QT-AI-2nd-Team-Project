# Workflow - 2026-06-08 ai-http-client-adapter-foundation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-http-client-adapter-foundation` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `chore/ai-http-client-adapter-foundation` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | AI MSA 분리를 위해 이전 PR에서 고정한 outbound system endpoint 계약을 실제 HTTP client adapter 요청 계약으로 검증할 기반이 필요함 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `07_요구사항_정의서.md`, `18_코드_품질_게이트.md`, `2026-06-08_ai-system-endpoint-contract-sync.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/client/**`, `qtai-server/src/test/java/com/qtai/domain/ai/client/**`, `qtai-server/src/main/resources/application.yml`, `qtai-server/build.gradle.kts`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

기본 런타임은 기존 mock client를 유지하고, `qtai.ai.client.mode=http`를 명시했을 때만 AI outbound HTTP adapter 5종이 활성화되는 기반을 만든다. 실제 provider Controller나 서비스 토큰 발급/JWKS 구현은 하지 않고, MockWebServer 계약 테스트로 AI 쪽 요청/응답 파싱 계약을 먼저 고정한다.

## 범위

- `QtContextClientHttpAdapter`, `BibleVerseClientHttpAdapter`, `StudyPublishClientHttpAdapter`, `AuditLogClientHttpAdapter`, `AdminAuthClientHttpAdapter`를 추가한다.
- `qtai.ai.client.mode=mock|http`, `service-token`, 공통 timeout, client별 base-url/timeout 설정을 추가한다.
- 기존 mock bean 조건을 `qtai.ai.client.mode=mock`, `matchIfMissing=true` 기준으로 정리하되 `local/test` profile, `@ConditionalOnMissingBean`, `@Primary` 미사용 원칙은 유지한다.
- 공통 HTTP support에서 Bearer token, traceparent 전파 준비, Idempotency-Key 생성, `ApiResponse<T>` envelope 파싱, `AiClientException` 변환을 처리한다.
- MockWebServer 기반 계약 테스트로 path/query/body/header, success envelope, error envelope, 기본 mock 유지 조건을 검증한다.

## 제외 범위

- 실제 provider service `/api/v1/system/**` Controller 구현
- 실제 서비스 토큰 발급, JWKS, mTLS 구현
- retry/circuit breaker 라이브러리 도입
- gateway, Docker, Kubernetes 변경
- AI business flow를 HTTP adapter로 강제 전환
- 운영 기본값을 `http`로 변경
- DB schema, migration, seed 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/client/http/**` | AI outbound HTTP 공통 설정, envelope/error mapper, HTTP support |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/client/*/*HttpAdapter.java` | 각 AI client interface의 HTTP 구현체 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/client/**/*Mock.java` | mock 활성화 조건을 `qtai.ai.client.mode=mock` 기준으로 정리 |
| Modify | `qtai-server/src/main/resources/application.yml` | AI client 기본 설정 추가 |
| Modify | `qtai-server/build.gradle.kts` | MockWebServer test dependency 추가 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/client/http/AiHttpClientAdapterContractTest.java` | MockWebServer 기반 HTTP adapter 요청/응답 계약 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/client/AiBoundaryClientContractTest.java` | bean guard와 client exception 계약 검증 보강 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-http-client-adapter-foundation_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `chore/ai-http-client-adapter-foundation` 브랜치에서 작업한다.
2. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
3. `AiClientProperties`와 HTTP mode 설정 구조를 추가한다.
4. 기존 mock들의 conditional property를 `qtai.ai.client.mode=mock` 기준으로 변경한다.
5. 공통 HTTP support를 구현한다: URL 조립, header 생성, envelope 파싱, error mapping, timeout 적용.
6. 5개 HTTP adapter를 구현한다.
7. MockWebServer 계약 테스트와 bean guard 테스트를 추가한다.
8. report 문서를 작성하고 검증 결과를 기록한다.
9. 검증 명령 실행 후 stage하고 `chore(ai): HTTP client adapter 기반 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiHttpClientAdapterContractTest` | 5개 adapter의 method/path/query/body/header 계약 |
| `AiHttpClientAdapterContractTest` | `Authorization`, `traceparent`, write endpoint `Idempotency-Key` header |
| `AiHttpClientAdapterContractTest` | success envelope data 파싱 |
| `AiHttpClientAdapterContractTest` | `success=false`, 401/403/404/429/5xx, timeout, malformed envelope의 `AiClientException` 매핑 |
| `AiBoundaryClientContractTest` | mock은 기본/mock mode에서만 후보, HTTP adapter는 `mode=http`에서만 후보 |

## 수용 기준

- [ ] 기본 설정은 `mock`이며 HTTP adapter는 기본 조건에서 활성화되지 않는다.
- [ ] `qtai.ai.client.mode=http`일 때만 HTTP adapter 5종이 bean 후보가 된다.
- [ ] HTTP mode에서는 service token과 client별 base-url 누락을 fail-fast로 감지한다.
- [ ] 모든 HTTP 요청에 Bearer token을 넣고, MDC의 `traceparent`가 있으면 전파한다.
- [ ] publish/hide/audit 요청에 `Idempotency-Key`를 추가한다.
- [ ] success envelope는 각 client DTO로 파싱된다.
- [ ] error envelope와 HTTP 오류는 `AiClientException`으로 변환된다.
- [ ] 실제 provider Controller, DB, JWKS, retry/circuit breaker 변경이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 공통 HTTP support, 5개 adapter, 계약 테스트가 같은 error/envelope/header 규칙을 공유하므로 한 흐름에서 정합성을 맞추는 편이 안전하다.
- mock conditional 변경과 bean guard 테스트가 구현체 활성화 조건과 직접 연결되어 있어 병렬 편집 시 충돌 가능성이 있다.
- 변경 범위는 넓어 보이지만 모두 `domain.ai.client` 경계 안에 집중되어 있다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 테스트, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat compileJava
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest
```

```powershell
git diff --check
```

## 후속 작업으로 남길 항목

- provider service가 실제 `/api/v1/system/**` endpoint를 열면 base-url 연결 검증 PR을 진행한다.
- 서비스 토큰 발급/JWKS, retry/circuit breaker, gateway/Docker/K8s는 별도 MSA 인프라 단계에서 다룬다.
- 필요 시 interface에 caller-supplied idempotency key를 추가하는 계약 확장은 별도 PR에서 검토한다.
