# Report - 2026-06-08 ai-system-endpoint-contract-sync

## 개요

- 작업명: `ai-system-endpoint-contract-sync`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-system-endpoint-contract-sync.md`
- 작업 브랜치: `chore/ai-system-endpoint-contract-sync`
- 실행 방식: 직접 실행
- 관련 F-ID: F-02, F-14, F-15
- 목적: AI MSA 분리 시 `ai-service`가 호출할 provider service system endpoint 계약 6종을 AI boundary 문서, OpenAPI, client 계약 테스트에 동기화했다.

## 변경 요약

- `qtai-server/apis/ai-service/openapi.yaml`에 `x-ai-outbound-system-endpoints` vendor extension을 추가했다.
- provider service endpoint는 ai-service가 제공하는 API가 아니므로 OpenAPI `paths`에는 추가하지 않았다.
- 공통 규약을 OpenAPI에 고정했다.
  - `/api/v1/system/**`
  - `Authorization: Bearer {service-token}` + `SYSTEM_BATCH`
  - `ApiResponse<T>` envelope
  - 쓰기 endpoint `Idempotency-Key` 필수
  - `traceparent` 전파와 응답 `traceId`
  - `ApiResponse.error(code,message)` → `AiClientException` 매핑
- outbound endpoint 6종을 method/path/client operation/schema 기준으로 고정했다.
  - QT context
  - 오늘 QT passage 상태
  - Bible verse 단건/목록/범위
  - Study publish/hide
  - Audit log
  - Admin/Auth active/verify/verify-any
- `QtContextResult` 계약은 `cacheStatus`를 제외하고, `passageContext`를 허용된 메타/context 블록으로 정의했다.
- `AiQtClientContractTest`에 `QtContextResult`와 today QT status record field set 검증을 추가했다.

## 추가 확인

- `QtContextClient` 확인 결과: 정상
  - `getQtContext(Long viewerId, Long qtPassageId)`
  - `getTodayQtPassageStatus(LocalDate qtDate)`
  - `TodayQtPassageStatus(qtDate, exists, passageId, cacheStatus)`
- `QtContextResult` 확인 결과: 정상
  - `passageId`, `bibleBook`, `chapter`, `startVerse`, `endVerse`, `passageReference`, `title`, `summary`, `passageContext`
- 파일이 잘려 있는 문제는 없었다.

## 제외 범위 준수

- HTTP client 구현체를 추가하지 않았다.
- 실제 `/api/v1/system/qt/**`, `/bible/**`, `/study/**`, `/audit/**`, `/admin/**` Controller를 개설하지 않았다.
- DB schema, migration, seed를 변경하지 않았다.
- 서비스 토큰 발급, JWKS, mTLS를 구현하지 않았다.
- Pact 또는 Spring Cloud Contract를 도입하지 않았다.

## 검증 결과

```powershell
cd qtai-server
.\gradlew.bat compileJava
```

- 결과: PASS

```powershell
cd qtai-server
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
```

- 결과: PASS

```powershell
python -c "<openapi yaml parse and outbound endpoint assertion>"
```

- 결과: PASS
- 확인 내용:
  - `x-ai-outbound-system-endpoints` 존재
  - 공통 auth/envelope/idempotency/trace/error mapping 존재
  - 6개 outbound endpoint 그룹의 method/path 일치
  - publish, hide, audit에 `idempotencyKeyRequired: true` 명시
  - provider service endpoint가 ai-service OpenAPI `paths`에 노출되지 않음
  - `QtContextResult`에 `cacheStatus` 없음
  - `TodayQtPassageStatus.cacheStatus` enum이 `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY`로 고정됨

```powershell
git diff --check
```

- 결과: PASS
- 비고: 일부 파일에서 LF가 CRLF로 변환될 수 있다는 Git 경고만 출력됨

## 후속 작업

- provider service 추출 PR에서 실제 system endpoint Controller를 개설한다.
- AI service HTTP adapter는 다음 단계에서 기존 `*Client` interface 뒤에 추가한다.
- Pact 또는 Spring Cloud Contract는 provider endpoint가 실제 개설되는 단계에서 도입한다.
