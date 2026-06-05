# Workflow - 2026-06-04 ai-review-reference-candidate-promotion

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-candidate-promotion` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | PDF 자동 추출 candidate JSON에 `bookCode`가 없어 운영용 `ai-review-reference-index.v1`로 바로 사용할 수 없으므로, page range 기반 book section map으로 승격하는 도구가 필요함 |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-contract.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-pdf-index-diagnostics.md` |
| 대상 경로 | `.gitignore`, `qtai-server/build.gradle.kts`, `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`ai-review-reference-index-candidate.v1` 후보 JSON과 `ai-review-reference-book-section-map.v1` map JSON을 입력으로 받아 운영용 `ai-review-reference-index.v1` JSON을 생성하는 승격 CLI를 추가한다. 이번 PR은 도구, 계약 검증, 테스트용 샘플만 구현하며 실제 IVP 전체 책별 page range map 작성과 restricted 배포는 후속 작업으로 둔다.

## 범위

- book section map reader를 추가한다.
- candidate promotion service를 추가한다.
- 승격 CLI와 Gradle task `aiReviewReferencePromoteCandidateIndex`를 추가한다.
- output/summary 경로는 `build/**` 아래만 허용한다.
- candidate, promotion summary, 운영용 reference-index 산출물이 Git에 포함되지 않도록 `.gitignore`를 보강한다.
- focused test와 전체 test를 실행한다.
- report를 작성하고 커밋/push한다.

## 제외 범위

- 실제 IVP 전체 book section map 작성
- `restricted/validation/index/reference-index.json` 배포
- layer 2 prompt 연결 변경
- PDF 원본, candidate JSON, 운영용 index 산출물 커밋
- DB migration, OpenAPI, 관리자 API 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `.gitignore` | 승격 산출물 커밋 방지 |
| Modify | `qtai-server/build.gradle.kts` | promotion CLI task 추가 |
| Create | `AiReviewReferenceBookSectionMapReader.java` | book section map JSON 파싱/검증 |
| Create | `AiReviewReferenceCandidatePromotionService.java` | candidate를 운영용 index로 승격 |
| Create | `AiReviewReferenceCandidatePromotionTool.java` | CLI entry point |
| Test | `AiReviewReferenceBookSectionMapReaderTest.java` | map 계약 검증 |
| Test | `AiReviewReferenceCandidatePromotionServiceTest.java` | 승격 규칙 검증 |
| Test | `AiReviewReferenceCandidatePromotionToolTest.java` | CLI 인자와 output 경로 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-candidate-promotion_report.md` | 실행 결과 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. workflow-spec-runner 절차로 spec을 읽고 직접 실행을 선택한다.
3. book section map reader 테스트를 추가하고 구현한다.
4. candidate promotion service 테스트를 추가하고 구현한다.
5. promotion CLI 테스트와 Gradle task를 추가한다.
6. `.gitignore`를 보강한다.
7. focused test와 전체 test를 실행한다.
8. report를 작성한다.
9. 커밋 메시지 규칙에 맞춰 커밋하고 작업 브랜치에 push한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceBookSectionMapReaderTest` | 정상 map 읽기 |
| `AiReviewReferenceBookSectionMapReaderTest` | schemaVersion/sourceFileHash 불일치 실패 |
| `AiReviewReferenceBookSectionMapReaderTest` | page range 겹침, 역전, 필수 필드 누락 실패 |
| `AiReviewReferenceCandidatePromotionServiceTest` | pageStart 기준 bookCode 매핑 |
| `AiReviewReferenceCandidatePromotionServiceTest` | `UNUSABLE` entry 제외 |
| `AiReviewReferenceCandidatePromotionServiceTest` | map에 없는 page는 summary의 unmapped count로 기록 |
| `AiReviewReferenceCandidatePromotionServiceTest` | summary에 `referenceText` 원문 미포함 |
| `AiReviewReferenceCandidatePromotionToolTest` | 필수 CLI 인자 누락 실패 |
| `AiReviewReferenceCandidatePromotionToolTest` | output/summary가 `build/**` 밖이면 실패 |

## 수용 기준

- [ ] 운영 output schemaVersion은 `ai-review-reference-index.v1`이다.
- [ ] candidate schemaVersion은 `ai-review-reference-index-candidate.v1`만 허용한다.
- [ ] map schemaVersion은 `ai-review-reference-book-section-map.v1`만 허용한다.
- [ ] candidate와 map의 `sourceFileHash`가 다르면 실패한다.
- [ ] `UNUSABLE` entry는 승격하지 않는다.
- [ ] page range로 찾은 `bookCode`와 `<bookName> <detectedHeading>` label을 운영 entry에 반영한다.
- [ ] summary에는 원문 `referenceText`가 포함되지 않는다.
- [ ] focused test와 전체 test가 통과한다.
- [ ] report가 작성된다.
- [ ] 작업 브랜치에 push된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- reader, service, CLI가 같은 JSON 계약과 record를 공유해 한 흐름에서 맞추는 편이 안전하다.
- 테스트와 구현 경로가 모두 `ai/internal` 안에서 겹친다.
- workflow, report, commit, push까지 순차 검증이 필요한 작업이다.

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
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReference*Promotion*Test" --tests "*AiReviewReferenceBookSectionMapReaderTest"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

## 후속 작업으로 남길 항목

- 실제 IVP 전체 book section map 작성
- 운영용 `restricted/validation/index/reference-index.json` 배포
- `validation_reference_jobs.indexStorageUri`와 운영 index 연결 검증
