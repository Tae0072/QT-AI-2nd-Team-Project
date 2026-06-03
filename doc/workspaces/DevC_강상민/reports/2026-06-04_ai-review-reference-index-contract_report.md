# Report - 2026-06-04 ai-review-reference-index-contract

## Summary

- 브랜치: `feature/ai-review-reference-index-contract`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-contract.md`
- 작업 유형: 계약 설계 문서
- `validation_reference_jobs.indexStorageUri`가 가리킬 범위별 해설 index JSON 계약을 정의했다.
- 후속 `AiReviewReferenceExcerptService` 구현이 사용할 최소 필드, 매칭 기준, 저장/노출 정책을 정리했다.

## 정리 내용

- index JSON 최상위 구조를 `schemaVersion`, `sourceFileHash`, `generatedAt`, `entries`로 정의했다.
- entry 구조를 `bookCode`, 시작/종료 장절, `referenceRangeLabel`, `referenceText`, `referenceHash`로 정의했다.
- 후속 매칭 기준은 asset 성경 범위와 index entry 범위가 겹치는 항목을 찾는 방식으로 정리했다.
- 여러 entry가 매칭되면 성경 순서대로 합치되, 후속 구현에서 prompt 최대 길이를 제한하도록 남겼다.
- 매칭 실패, index 읽기 실패, source hash 불일치 시 layer 2 `NEEDS_REVIEW`로 처리하는 방향을 정리했다.
- `referenceText`는 LLM prompt 입력으로만 사용하고, 검증 로그에는 hash/range/job id 같은 metadata만 저장하는 정책을 명시했다.

## 제외한 작업

- DB schema 변경 없음
- Java production/test code 변경 없음
- OpenAPI 변경 없음
- PDF 파싱, OCR, 임베딩, index 파일 생성 구현 없음
- `indexStorageUri` 실제 저장소 연동 없음
- `AiReviewReferenceExcerptService` 구현 없음
- layer 2 prompt에 `referenceExcerpt`를 실제로 주입하는 구현 없음

## 검증

```powershell
& 'C:/Program Files/Git/bin/git.exe' status --short --branch
```

- 결과: `feature/ai-review-reference-index-contract` 브랜치에서 workflow/report 문서만 변경 대상이다.

- workflow/report 문서에 placeholder marker나 불완전한 결정 표현이 남아 있는지 검색했다.
- 결과: 매치 없음.

- Java production/test code, DB migration, OpenAPI 변경 여부를 확인했다.
- 결과: 문서 파일 외 변경 없음.

## 실행하지 않은 검증

- Gradle 테스트는 실행하지 않았다.
- 이유: 이번 작업은 코드 변경 없는 계약 설계 문서 작업이다.

## 후속 작업

- `AiReviewReferenceExcerptService` 구현
- `indexStorageUri` JSON reader/adapter 구현
- 산출물 성경 범위와 index entry 범위 매칭 구현
- layer 2 prompt에 `referenceExcerpt` 또는 `referenceSummary` 주입
- reference text 길이 제한과 초과 시 요약/절단 정책 구현
