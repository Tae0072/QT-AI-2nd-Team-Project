# Workflow - 2026-06-08 ai-client-contract-followup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-client-contract-followup` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `refactor/ai-client-contract-followup` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | `ai-service-boundary-contract` PR 머지 후 리뷰어가 후속 PR 전에 반영을 권장한 client 계약 WARN 정리 |
| 기준 문서 | `03_아키텍처_정의서.md`, `07_요구사항_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/client/**`, `qtai-server/src/test/java/com/qtai/domain/ai/client/**`, `qtai-server/apis/ai-service/openapi.yaml` |

## 작업 목표

AI service 분리 전 경계 client 계약의 모호성을 줄인다. 이번 작업은 운영 런타임 동작을 바꾸지 않고, `BibleVerseClient`의 오버로드 메서드명을 의도별로 분리하며 mock 부정 입력 처리 규칙을 `AiClientException.FailureCode.VALIDATION_FAILED`로 고정한다.

## 범위

- `BibleVerseClient.getVerses(List<Long>)`를 `getVersesByIds(List<Long>)`로 변경한다.
- `BibleVerseClient.getVerses(String, int, Integer, Integer)`를 `getVersesInRange(String, int, Integer, Integer)`로 변경한다.
- `BibleVerseClientMock`에서 `verseId`, `verseIds`, `verseIds` 내부 원소가 null이거나 `verseIds`가 empty이면 `AiClientException`을 던진다.
- `AdminAuthClientMock.verifyAnyRole(...)`에서 role collection이 null, empty, 내부 null이면 `AiClientException`을 던진다.
- `BibleVerseClientMock`의 `java.util.stream.IntStream` FQN 사용을 import로 정리한다.
- `qtai-server/apis/ai-service/openapi.yaml`의 `x-ai-service-dependencies.bible.operations`를 새 메서드명과 맞춘다.
- `AiBoundaryClientContractTest`에 부정 입력 계약 테스트를 추가한다.

## 제외 범위

- HTTP client 구현체 추가
- 운영 handler가 기존 UseCase 대신 AI client interface를 호출하도록 바꾸는 작업
- DB schema, migration, seed 변경
- 실제 관리자 권한 정책 변경
- `x-required-admin-role` OpenAPI 확장 추가
- `StudyPublishClient` javadoc 보강
- mock 컬렉션 동시성 구조 변경
- idempotency 세부 정책 추가

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-client-contract-followup.md` | 작업 범위와 실행 기준 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-08_ai-client-contract-followup_report.md` | 작업 결과와 검증 결과 기록 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/client/bible/BibleVerseClient.java` | Bible client 메서드명 분리 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/client/bible/BibleVerseClientMock.java` | 부정 입력 실패 모델과 import 정리 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/client/admin/AdminAuthClientMock.java` | 빈 role collection 실패 모델 고정 |
| Modify | `qtai-server/apis/ai-service/openapi.yaml` | bible operation 이름 정합화 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/client/AiBoundaryClientContractTest.java` | 새 메서드명과 부정 입력 계약 검증 |

## 구현 순서

1. `AiBoundaryClientContractTest`의 기존 `getVerses(...)` 호출을 `getVersesInRange(...)`로 변경한다.
2. `BibleVerseClient` 메서드를 `getVersesByIds`, `getVersesInRange`로 분리한다.
3. `BibleVerseClientMock` 구현 메서드명을 맞추고 null/empty 입력을 `AiClientException(FailureCode.VALIDATION_FAILED, "bible", ...)`로 처리한다.
4. `AdminAuthClientMock.verifyAnyRole(...)`의 null/empty/내부 null 입력을 `AiClientException(FailureCode.VALIDATION_FAILED, "admin-auth", ...)`로 처리한다.
5. `AiBoundaryClientContractTest`에 Bible/AdminAuth 부정 입력 테스트를 추가한다.
6. `openapi.yaml`의 `x-ai-service-dependencies.bible.operations`를 `getVerse`, `getVersesByIds`, `getVersesInRange`로 정리한다.
7. 검증 명령을 실행하고 report 문서를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiBoundaryClientContractTest` | `BibleVerseClientMock.getVerse(null)`이 `VALIDATION_FAILED`를 던진다 |
| `AiBoundaryClientContractTest` | `BibleVerseClientMock.getVersesByIds(null)`, empty list, 내부 null이 `VALIDATION_FAILED`를 던진다 |
| `AiBoundaryClientContractTest` | `AdminAuthClientMock.verifyAnyRole(...)`의 null, empty, 내부 null role 입력이 `VALIDATION_FAILED`를 던진다 |

## 수용 기준

- [ ] `BibleVerseClient`에 `getVerses` 오버로드가 남아 있지 않다.
- [ ] `BibleVerseClientMock`의 부정 입력은 NPE가 아니라 `AiClientException.FailureCode.VALIDATION_FAILED`로 실패한다.
- [ ] `AdminAuthClientMock.verifyAnyRole(...)`의 부정 입력은 SUPER_ADMIN fallback이 아니라 `VALIDATION_FAILED`로 실패한다.
- [ ] ai-service OpenAPI dependency operation 이름이 Java interface와 일치한다.
- [ ] 운영 handler, DB, HTTP 구현체, 실제 권한 정책은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 AI client interface, mock, 단일 계약 테스트에 집중되어 있어 순차 확인이 안전하다.
- 테스트와 구현이 같은 계약을 공유하므로 한 에이전트가 직접 수정하고 바로 검증하는 편이 충돌 위험이 낮다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 코드 수정, 테스트 보강, 검증, 리포트 작성, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat compileJava
.\gradlew.bat test --tests com.qtai.domain.ai.client.AiBoundaryClientContractTest --tests com.qtai.domain.ai.client.qt.AiQtClientContractTest
```

```powershell
python -c "<openapi yaml parse and bible operation assertion>"
git diff --check
```

## 후속 작업으로 남길 항목

- Admin API별 `x-required-admin-role` OpenAPI 확장 검토
- `StudyPublishClient` 계약 설명 보강
- mock 컬렉션 동시성 구조 변경 필요 여부 검토
- F-15 idempotency 세부 정책 검토
