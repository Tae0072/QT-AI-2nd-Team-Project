# Workflow - 2026-06-08 ai-system-endpoint-contract-sync

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-system-endpoint-contract-sync` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `chore/ai-system-endpoint-contract-sync` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | AI MSA 분리 준비 중 제공 서비스 작업자가 내부 system endpoint 계약 6종을 확정해 AI 계약 문서와 OpenAPI 동기화가 필요함 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `07_요구사항_정의서.md`, `18_코드_품질_게이트.md`, `2026-06-08_ai-service-boundary-contract.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/**`, `qtai-server/apis/ai-service/openapi.yaml`, `qtai-server/src/test/java/com/qtai/domain/ai/client/**` |

## 작업 목표

AI 도메인을 별도 `ai-service`로 분리할 때 AI가 호출할 provider service의 system endpoint 계약을 문서와 OpenAPI에 고정한다. 이번 작업은 계약 동기화만 수행하며, 실제 HTTP client 구현체나 provider service Controller는 만들지 않는다.

## 범위

- AI boundary 문서에 `/api/v1/system/**` 기반 outbound system endpoint 계약을 추가한다.
- `qtai-server/apis/ai-service/openapi.yaml`에 AI가 호출할 outbound endpoint 6종을 vendor extension으로 명시한다.
- provider endpoint는 ai-service가 제공하는 API가 아니므로 OpenAPI `paths`에는 추가하지 않는다.
- `QtContextResult`에는 `cacheStatus`를 넣지 않고, today QT status 응답에만 `cacheStatus` enum을 둔다.
- `passageContext`는 본문 원문 전체가 아니라 AI 생성/검증에 필요한 허용된 메타/context 블록으로 정의한다.
- `QtContextClient`와 `QtContextResult`가 잘려 있는지 확인하고 결과를 report에 기록한다.
- client 계약 테스트에 QT context DTO와 today status DTO의 경계 조건을 보강한다.

## 제외 범위

- HTTP client adapter 구현
- 실제 `/api/v1/system/qt/**`, `/bible/**`, `/study/**`, `/audit/**`, `/admin/**` Controller 개설
- DB schema, migration, seed 변경
- 서비스 토큰 발급, JWKS, mTLS 구현
- Pact 또는 Spring Cloud Contract 실제 도입
- provider service의 실제 UseCase 구현 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-system-endpoint-contract-sync.md` | 작업 범위와 실행 기준 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-system-endpoint-contract-sync_report.md` | 결과와 검증 기록 |
| Modify | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-service-boundary-contract.md` | 기존 AI boundary 문서에 system endpoint 계약 동기화 섹션 추가 |
| Modify | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-service-boundary-contract_report.md` | 기존 boundary report에 후속 동기화 결과 요약 추가 |
| Modify | `qtai-server/apis/ai-service/openapi.yaml` | outbound system endpoint 계약과 DTO schema 명시 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/client/qt/AiQtClientContractTest.java` | QT context/today status client 계약 검증 보강 |

## 구현 순서

1. `dev` 최신화 후 `chore/ai-system-endpoint-contract-sync` 브랜치에서 작업한다.
2. `QtContextClient`와 `QtContextResult` 파일 상태를 확인한다.
3. 기존 AI boundary workflow/report에 system endpoint 계약 섹션을 추가한다.
4. OpenAPI 최상위 vendor extension으로 `x-ai-outbound-system-endpoints`를 추가한다.
5. extension에 공통 규약을 고정한다: `/api/v1/system/**`, service-token bearer, `SYSTEM_BATCH`, `ApiResponse<T>`, write endpoint `Idempotency-Key`, `traceparent`, `traceId`, `AiClientException` 매핑.
6. extension에 QT, today QT status, Bible, Study, Audit, Admin/Auth endpoint 6종을 method/path/client operation/schema 기준으로 추가한다.
7. components schema에 outbound DTO와 command shape를 추가한다.
8. `AiQtClientContractTest`에 `QtContextResult` field set과 today status field set/cache enum 검증을 추가한다.
9. 검증 명령을 실행하고 결과를 report에 기록한다.
10. 변경 파일을 stage하고 `chore(ai): system endpoint 계약 동기화`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiQtClientContractTest` | `QtContextResult` record component가 `passageId`, `bibleBook`, `chapter`, `startVerse`, `endVerse`, `passageReference`, `title`, `summary`, `passageContext`로 고정됨 |
| `AiQtClientContractTest` | `QtContextResult`에 `cacheStatus`가 없고 today status record에만 `cacheStatus`가 있음 |
| `AiQtClientContractTest` | `CacheStatus` enum이 `HIT`, `MISS`, `STALE_FALLBACK`, `EMPTY`로 고정됨 |

## 수용 기준

- [ ] OpenAPI에 `x-ai-outbound-system-endpoints`가 존재한다.
- [ ] 6개 outbound endpoint 계약의 method/path가 상대가 준 계약과 일치한다.
- [ ] publish, hide, audit 쓰기 endpoint에 `Idempotency-Key` 필수 조건이 명시된다.
- [ ] provider service endpoint가 ai-service OpenAPI `paths`에 추가되지 않는다.
- [ ] `QtContextResult`에는 `cacheStatus`가 없고, today QT status에만 `cacheStatus`가 있다.
- [ ] `passageContext` 설명이 허용된 메타/context 블록으로 고정된다.
- [ ] HTTP 구현체, Controller, DB, JWKS, Pact 변경이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 문서, OpenAPI, 계약 테스트가 모두 같은 endpoint 계약을 공유하므로 한 흐름에서 정합성을 확인하는 편이 안전하다.
- 변경 파일 수는 여러 개지만 구현 범위는 AI client 계약과 문서에 집중되어 있어 병렬화 이점이 작다.
- provider endpoint를 `paths`에 넣지 않는 판단이 전체 변경에 일관되게 적용되어야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 계약 동기화, 테스트 보강, 검증, report 작성, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat compileJava
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
```

```powershell
python -c "<openapi yaml parse and outbound endpoint assertion>"
git diff --check
```

## 후속 작업으로 남길 항목

- 각 provider service 추출 PR에서 실제 system endpoint Controller를 개설한다.
- AI service HTTP adapter 구현은 다음 단계에서 `*Client` interface 뒤에 추가한다.
- Pact 또는 Spring Cloud Contract는 provider endpoint가 실제 개설되는 단계에서 도입한다.
