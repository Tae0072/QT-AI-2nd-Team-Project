# Workflow - 2026-06-04 ai-review-reference-excerpt-injection

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-excerpt-injection` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | layer 2 검수 AI가 참조자료 index metadata만 받던 상태에서, 산출물 성경 범위와 매칭되는 제한 excerpt를 prompt에 주입해야 함 |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-reader.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`AiReviewReferenceIndexReader`가 읽은 `ai-review-reference-index.v1` entries 중 산출물 `payloadJson.sourceMetadata.verses[]` 범위와 겹치는 항목만 선택한다. 선택된 excerpt는 layer 2 검수 AI prompt에만 넣고, DB 저장 필드와 감사 로그에는 원문 `referenceText`를 남기지 않는다.

## 범위

- `AiReviewReferenceExcerptSelector`를 추가해 산출물 verse metadata와 index entry 범위를 매칭한다.
- 매칭 기준은 같은 `bookCode`이고, 산출물 `chapterNo/verseNo`가 entry `chapterStart/verseStart`부터 `chapterEnd/verseEnd` 사이에 포함되는 것이다.
- 선택 결과는 index 순서를 유지하고 최대 3개, 각 `referenceText`는 prompt용으로 1200자까지 자른다.
- `AiReviewValidationService`가 최신 ACTIVE reference metadata 기준으로 index를 읽고 selector 결과를 prompt `reference.excerpts[]`에 넣는다.
- 매칭 실패, verse metadata 누락, index reader 오류는 LLM 호출 없이 layer 2 `ADVISOR/NEEDS_REVIEW` 로그로 남긴다.
- report를 작성하고 검증 결과를 기록한다.

## 제외 범위

- 새 관리자 API/OpenAPI 변경
- DB schema migration
- index JSON 생성, PDF 파싱, OCR
- 성경 본문 원문 저장 또는 외부 SSoT 원문 저장
- prompt excerpt 요약/랭킹 모델 추가
- `payloadJson`, `checklistJson`, 감사 로그에 `referenceText` 원문 저장

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferenceExcerptSelector.java` | 산출물 verse metadata 파싱, index entry 범위 매칭, prompt용 excerpt 제한 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewValidationService.java` | index reader/selector 연결, prompt 확장, 실패 시 NEEDS_REVIEW 로그 처리 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceExcerptSelectorTest.java` | 범위 매칭, bookCode 불일치, 최대 3개, 1200자 제한 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewValidationServiceTest.java` | prompt excerpt 주입, 저장 금지, 실패 분기 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-excerpt-injection_report.md` | 실행 결과, 검증 결과, 제외 범위 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. `workflow-spec-runner` 절차로 spec을 읽고 직접 실행 경로를 선택한다.
3. `AiReviewReferenceExcerptSelectorTest`를 먼저 추가하고 실패를 확인한다.
4. `AiReviewReferenceExcerptSelector`를 구현해 selector focused test를 통과시킨다.
5. `AiReviewValidationServiceTest`에 prompt 주입과 실패 분기 테스트를 추가하고 실패를 확인한다.
6. `AiReviewValidationService`에 `AiReviewReferenceIndexReader`와 selector를 주입하고 prompt/checklistJson/NEEDS_REVIEW 분기를 구현한다.
7. focused test를 실행한다.
8. 전체 test를 실행한다.
9. report를 작성한다.
10. 커밋 메시지 규칙에 따라 커밋하고 작업 브랜치에 push한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceExcerptSelectorTest` | 산출물 verse가 entry 범위 안에 있으면 선택 |
| `AiReviewReferenceExcerptSelectorTest` | bookCode가 다르면 선택하지 않음 |
| `AiReviewReferenceExcerptSelectorTest` | 여러 entry가 매칭되면 index 순서로 최대 3개만 선택 |
| `AiReviewReferenceExcerptSelectorTest` | `referenceText`가 1200자를 초과하면 prompt용 값만 자름 |
| `AiReviewValidationServiceTest` | LLM `userPrompt`의 `reference.excerpts[]`에 매칭 excerpt 포함 |
| `AiReviewValidationServiceTest` | `checklistJson`에는 `referenceText` 원문 없이 count/hash/label만 저장 |
| `AiReviewValidationServiceTest` | 매칭 excerpt가 없으면 LLM 미호출, `AI_REVIEW_REFERENCE_EXCERPT_NOT_FOUND` |
| `AiReviewValidationServiceTest` | `sourceMetadata.verses[]` 누락 시 LLM 미호출, `AI_REVIEW_ASSET_VERSE_METADATA_NOT_FOUND` |
| `AiReviewValidationServiceTest` | index reader 오류 시 LLM 미호출, `AI_REVIEW_REFERENCE_INDEX_*` 메시지 저장 |

## 수용 기준

- [ ] prompt `reference.excerpts[]`에 매칭된 excerpt만 최대 3개 포함된다.
- [ ] prompt용 `referenceText`는 각 1200자 이하이다.
- [ ] 산출물 verse metadata가 없거나 매칭 excerpt가 없으면 LLM을 호출하지 않는다.
- [ ] index reader 오류는 LLM 호출 없이 `NEEDS_REVIEW` 로그로 기록된다.
- [ ] `checklistJson`과 audit 대상 snapshot에는 `referenceText` 원문이 저장되지 않는다.
- [ ] 새 API, OpenAPI, DB migration 변경이 없다.
- [ ] focused test와 전체 test가 통과한다.
- [ ] report가 작성된다.
- [ ] 커밋 후 원격 작업 브랜치에 push된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- selector 구현과 service prompt/log 정책이 같은 보안 불변식에 묶여 있어 한 흐름에서 확인하는 편이 안전하다.
- 테스트와 구현이 `ai/internal`의 같은 파일군을 함께 수정하므로 병렬 편집 이점보다 충돌 위험이 크다.
- workflow/report/commit/push까지 순차 실행이 필요한 작업이다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec 기준으로 TDD, 구현, 검증, report, 커밋, push를 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceExcerptSelectorTest" --tests "*AiReviewValidationServiceTest"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

전체 test가 환경 문제로 실패하면 focused test 결과와 실패 사유를 report에 기록한다.

## 후속 작업으로 남길 항목

- 실제 운영 index 생성/배포 자동화
- excerpt 랭킹/요약 정책 고도화
- layer 2 prompt 품질 평가 케이스 축적
