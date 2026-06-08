# Report - 2026-06-08 ai-http-client-adapter-foundation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업 브랜치 | `chore/ai-http-client-adapter-foundation` |
| 기준 브랜치 | `dev` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-http-client-adapter-foundation.md` |
| 커밋 메시지 | `chore(ai): HTTP client adapter 기반 추가` |

## 작업 요약

AI MSA 분리 준비를 위해 기존 mock client를 기본값으로 유지하면서, `qtai.ai.client.mode=http`일 때만 outbound HTTP adapter 5종이 활성화되는 기반을 추가했다. 실제 provider Controller, 서비스 토큰 발급/JWKS, retry/circuit breaker, gateway/Docker/K8s, AI business flow 전환은 수행하지 않았다.

## 주요 변경

- `qtai.ai.client.*` 설정을 추가했다.
  - 기본값: `mode=mock`
  - `service-token`, 공통 `timeout-ms`, client별 `base-url`
- 기존 AI client mock 5종의 bean 조건을 `qtai.ai.client.mode=mock`, `matchIfMissing=true`로 변경했다.
  - `local/test` profile, `@ConditionalOnMissingBean`, `@Primary` 미사용 원칙은 유지했다.
- HTTP adapter 5종을 추가했다.
  - `QtContextClientHttpAdapter`
  - `BibleVerseClientHttpAdapter`
  - `StudyPublishClientHttpAdapter`
  - `AuditLogClientHttpAdapter`
  - `AdminAuthClientHttpAdapter`
- 공통 HTTP support를 추가했다.
  - `Authorization: Bearer {service-token}`
  - MDC `traceparent` 전파
  - write 요청의 `Idempotency-Key` 자동 생성
  - tolerant envelope parsing
  - `ApiResponse.error(code,message)`와 HTTP status를 `AiClientException`으로 매핑
- `MockWebServer` 테스트 의존성을 추가하고 HTTP adapter 계약 테스트를 작성했다.

## 계약 반영 상태

| 영역 | 반영 내용 |
| --- | --- |
| QT context | `GET /api/v1/system/qt/passages/{passageId}/context`, `viewerId`는 wire에 포함하지 않음 |
| 오늘 QT status | `GET /api/v1/system/qt/passages/today/status?date=YYYY-MM-DD` |
| Bible | 단건 GET, batch POST, range GET 계약 반영 |
| Study | publish/hide POST와 `Idempotency-Key` 반영 |
| Audit | audit log POST와 `Idempotency-Key` 반영 |
| Admin/Auth | active, verify, verify-any GET 계약 반영 |

## 수용 기준 결과

- [x] 기본 설정은 `mock`이며 HTTP adapter는 기본 조건에서 활성화되지 않는다.
- [x] `qtai.ai.client.mode=http`일 때만 HTTP adapter 5종이 bean 후보가 된다.
- [x] HTTP mode에서는 service token과 client별 base-url 누락을 fail-fast로 감지한다.
- [x] 모든 HTTP 요청에 Bearer token을 넣고, MDC의 `traceparent`가 있으면 전파한다.
- [x] publish/hide/audit 요청에 `Idempotency-Key`를 추가한다.
- [x] success envelope는 각 client DTO로 파싱된다.
- [x] error envelope와 HTTP 오류는 `AiClientException`으로 변환된다.
- [x] 실제 provider Controller, DB, JWKS, retry/circuit breaker 변경이 없다.

## 검증 결과

```powershell
cd qtai-server
.\gradlew.bat compileJava
```

- 결과: 성공

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest --tests com.qtai.domain.ai.client.http.AiHttpClientAdapterContractTest
```

- 결과: 성공

```powershell
git diff --check
```

- 결과: 성공
- 참고: Windows 작업트리의 LF/CRLF 경고만 출력되었고 whitespace error는 없었다.

## 제외 확인

- provider service endpoint Controller를 추가하지 않았다.
- DB migration/seed를 변경하지 않았다.
- 서비스 토큰 발급, JWKS, mTLS를 구현하지 않았다.
- retry/circuit breaker 라이브러리를 추가하지 않았다.
- AI 생성/검증 business flow를 HTTP adapter로 강제 전환하지 않았다.

## 후속 작업

- provider 서비스가 실제 system endpoint를 열면 `qtai.ai.client.mode=http`와 base-url을 설정해 통합 연결 검증 PR을 진행한다.
- 운영용 서비스 토큰 발급/JWKS, retry/circuit breaker, 배포 인프라 설정은 MSA 인프라 단계에서 별도 작업으로 진행한다.
