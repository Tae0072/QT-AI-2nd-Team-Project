# Workflow - 2026-06-04 ai-review-reference-index-reader

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-index-reader` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `validation_reference_jobs.indexStorageUri`가 가리키는 index JSON을 서버가 읽고 검증할 수 있어야 후속 범위 매칭과 prompt excerpt 주입이 가능함 |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-contract.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-service.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/main/resources/application.yml`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`restricted://validation/index/...` URI를 로컬 제한 저장소 root 하위 `Path`로 안전하게 변환하고, 해당 JSON 파일을 `ai-review-reference-index.v1` 계약에 맞춰 읽고 검증하는 Reader를 구현한다.

이번 작업은 index 읽기/검증까지만 수행한다. 성경 범위 매칭, referenceText 길이 제한/요약, layer 2 prompt에 referenceText를 넣는 구현은 후속 작업으로 둔다.

## 범위

- `qtai.validation.restricted-storage-root` 설정을 추가한다.
- `RestrictedStorageUriResolver`를 추가해 `restricted://...` URI를 root 하위 로컬 경로로 변환한다.
- `AiReviewReferenceIndexReader`를 추가해 index JSON을 읽고 계약을 검증한다.
- Reader 반환용 내부 record `ReferenceIndex`, `ReferenceIndexEntry`를 둔다.
- focused unit test를 추가한다.
- report를 작성한다.

## 제외 범위

- DB schema 변경
- 관리자 API/OpenAPI 변경
- PDF 파싱, OCR, 임베딩, index 생성 구현
- 산출물 성경 범위와 index entry 범위 매칭 구현
- layer 2 prompt에 referenceText 주입
- DB 로그/감사 로그에 referenceText 저장

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/resources/application.yml` | `qtai.validation.restricted-storage-root` 기본값 추가 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/RestrictedStorageUriResolver.java` | restricted URI를 안전한 로컬 Path로 변환 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferenceIndexReader.java` | index JSON 읽기/검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/RestrictedStorageUriResolverTest.java` | URI 변환과 차단 케이스 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceIndexReaderTest.java` | index 계약 파싱/검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-index-reader_report.md` | 실행 결과와 검증 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. `workflow-spec-runner` 절차로 spec을 다시 읽고 직접 실행을 선택한다.
3. `RestrictedStorageUriResolverTest`를 먼저 작성하고 실패를 확인한다.
4. `RestrictedStorageUriResolver`와 설정 주입을 최소 구현한다.
5. `AiReviewReferenceIndexReaderTest`를 작성하고 실패를 확인한다.
6. `AiReviewReferenceIndexReader`를 구현한다.
7. focused test를 실행한다.
8. 가능하면 전체 test를 실행한다.
9. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `RestrictedStorageUriResolverTest` | `restricted://validation/index/reference-index.json`을 설정 root 하위 경로로 변환 |
| `RestrictedStorageUriResolverTest` | 다른 scheme, 빈 URI, path traversal 거부 |
| `AiReviewReferenceIndexReaderTest` | 정상 index JSON을 읽어 entries 반환 |
| `AiReviewReferenceIndexReaderTest` | `schemaVersion`이 다르면 실패 |
| `AiReviewReferenceIndexReaderTest` | `sourceFileHash`가 reference job hash와 다르면 실패 |
| `AiReviewReferenceIndexReaderTest` | JSON이 깨졌거나 필수 필드가 없으면 실패 |

## 수용 기준

- [ ] `restricted://` URI는 설정 root 하위 안전한 경로로만 변환된다.
- [ ] path traversal과 다른 scheme은 실패한다.
- [ ] Reader는 `schemaVersion = ai-review-reference-index.v1`만 허용한다.
- [ ] Reader는 index `sourceFileHash`와 expected source hash가 다르면 실패한다.
- [ ] Reader는 비어 있지 않은 entries를 반환한다.
- [ ] `referenceText`는 Reader 결과에만 포함되고 로그/감사 로그 저장은 하지 않는다.
- [ ] DB schema, API, OpenAPI 변경이 없다.
- [ ] report를 작성한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- URI resolver와 index reader는 같은 실패 정책과 테스트 fixture를 공유한다.
- TDD red/green 순서를 한 흐름에서 확인하는 편이 안전하다.
- 변경 규모가 작고 ai internal 범위에 집중되어 있다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec을 기준으로 TDD, 구현, focused 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*RestrictedStorageUriResolverTest" --tests "*AiReviewReferenceIndexReaderTest"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

전체 test가 환경 문제나 시간 문제로 실패하면 focused test 결과와 실패 사유를 report에 남긴다.

## 후속 작업으로 남길 항목

- index entries와 산출물 성경 범위 매칭 구현
- referenceText 길이 제한/요약 정책 구현
- layer 2 prompt에 referenceExcerpt 또는 referenceSummary 주입
- 실제 index JSON 생성 스크립트 또는 배치 설계
