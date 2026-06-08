# Report - 2026-06-08 ai-client-contract-followup

## 개요

- 작업명: `ai-client-contract-followup`
- 브랜치: `chore/ai-client-contract-followup`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-client-contract-followup.md`
- 관련 F-ID: F-02, F-14, F-15
- 목적: `ai-service-boundary-contract` 머지 후 후속 PR 전에 정리하기로 한 AI client 계약 WARN 중 핵심 2건을 반영했다.

## 변경 내용

- `BibleVerseClient`의 오버로드 메서드를 의도별 이름으로 분리했다.
  - `getVerses(List<Long>)` -> `getVersesByIds(List<Long>)`
  - `getVerses(String, int, Integer, Integer)` -> `getVersesInRange(String, int, Integer, Integer)`
- `BibleVerseClientMock`의 부정 입력을 `AiClientException.FailureCode.VALIDATION_FAILED`로 고정했다.
  - `verseId == null`
  - `verseIds == null`
  - `verseIds.isEmpty()`
  - `verseIds` 내부 null
- `AdminAuthClientMock.verifyAnyRole(...)`의 부정 입력을 `AiClientException.FailureCode.VALIDATION_FAILED`로 고정했다.
  - `requiredRoles == null`
  - `requiredRoles.isEmpty()`
  - `requiredRoles` 내부 null
- `BibleVerseClientMock`의 `IntStream` FQN 사용을 import로 정리했다.
- `qtai-server/apis/ai-service/openapi.yaml`의 `x-ai-service-dependencies.bible.operations`를 Java interface 메서드명과 맞췄다.
- `AiBoundaryClientContractTest`에 부정 입력 계약 테스트를 추가했다.

## 제외한 내용

- HTTP client 구현체 추가 없음
- 운영 handler 호출 구조 변경 없음
- DB schema, migration, seed 변경 없음
- 실제 관리자 권한 정책 변경 없음
- `x-required-admin-role`, `StudyPublishClient` javadoc, mock 컬렉션 동시성, idempotency 세부 정책은 후속 작업으로 유지

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
python -c "<openapi yaml parse and bible operation assertion>"
```

- 결과: PASS
- 확인 내용: `x-ai-service-dependencies.bible.operations`가 `getVerse`, `getVersesByIds`, `getVersesInRange` 순서로 고정됨

```powershell
git diff --check
```

- 결과: PASS
- 비고: 일부 파일에서 LF가 CRLF로 변환될 수 있다는 Git 경고만 출력됨

## 수용 기준 확인

- [x] `BibleVerseClient`에 `getVerses` 오버로드가 남아 있지 않다.
- [x] `BibleVerseClientMock`의 부정 입력은 NPE가 아니라 `AiClientException.FailureCode.VALIDATION_FAILED`로 실패한다.
- [x] `AdminAuthClientMock.verifyAnyRole(...)`의 부정 입력은 SUPER_ADMIN fallback이 아니라 `VALIDATION_FAILED`로 실패한다.
- [x] ai-service OpenAPI dependency operation 이름이 Java interface와 일치한다.
- [x] 운영 handler, DB, HTTP 구현체, 실제 권한 정책은 변경하지 않았다.
