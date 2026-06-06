# Workflow - 2026-06-04 ai-review-reference-index-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-index-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | layer 2 검수 AI에 실제 참고자료 excerpt를 주입하기 전, `validation_reference_jobs.indexStorageUri`가 가리킬 index JSON 계약을 먼저 고정해야 함 |
| 기준 문서 | `AGENTS.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `07_요구사항_정의서.md`, `23_도메인_용어사전.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-03_ai-checklist-reference-structure.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-contract.md`, `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-index-contract_report.md` |

## 작업 목표

`validation_reference_jobs.indexStorageUri`가 가리키는 범위별 해설 index JSON 계약을 문서로 고정한다. 이 계약은 후속 `AiReviewReferenceExcerptService`가 산출물의 성경 범위에 맞는 참고자료 excerpt를 찾는 기준이 된다.

이번 작업은 계약 설계 문서화만 수행한다. PDF 원문 파싱, index 파일 읽기, 성경 범위 매칭 코드, layer 2 prompt에 excerpt를 주입하는 구현은 후속 작업으로 둔다.

## 범위

- index JSON의 최상위 필드와 entries 필드 의미를 정의한다.
- 성경 범위 표현 방식과 후속 매칭 기준을 정의한다.
- match 실패, index 조회 실패, 과도한 reference 길이 처리 방향을 정의한다.
- 검증 로그와 prompt에 저장/전달 가능한 값과 금지 값을 구분한다.
- workflow spec과 report를 작성한다.

## 제외 범위

- DB schema 변경
- Java production/test code 변경
- OpenAPI 변경
- PDF 파싱, OCR, 임베딩, index 파일 생성 구현
- `indexStorageUri` 실제 저장소 연동
- `AiReviewReferenceExcerptService` 구현
- layer 2 prompt에 `referenceExcerpt`를 실제로 주입하는 구현

## Index JSON 계약

후속 구현에서 `indexStorageUri`가 가리키는 자료는 JSON object여야 한다.

```json
{
  "schemaVersion": "ai-review-reference-index.v1",
  "sourceFileHash": "sha256:reference-pdf-hash",
  "generatedAt": "2026-06-04T00:00:00+09:00",
  "entries": [
    {
      "bookCode": "JHN",
      "chapterStart": 3,
      "verseStart": 16,
      "chapterEnd": 3,
      "verseEnd": 18,
      "referenceRangeLabel": "요한복음 3:16-18",
      "referenceText": "해당 범위 검수에 사용할 짧은 해설 참고자료",
      "referenceHash": "sha256:entry-reference-hash"
    }
  ]
}
```

## 필드 의미

| 필드 | 의미 |
| --- | --- |
| `schemaVersion` | index JSON 계약 버전. v1 값은 `ai-review-reference-index.v1` |
| `sourceFileHash` | 원본 PDF 또는 외부 SSoT 자료 해시. `validation_reference_jobs.source_file_hash`와 정합성 확인에 사용 |
| `generatedAt` | index 생성 시각 |
| `entries` | 성경 범위별 참고자료 목록 |
| `bookCode` | 성경 책 코드. 후속 구현에서 서버의 성경 책 식별 체계와 매핑 |
| `chapterStart`, `verseStart`, `chapterEnd`, `verseEnd` | 참고자료가 대응하는 성경 범위 |
| `referenceRangeLabel` | 운영자/로그 확인용 사람이 읽는 범위 라벨 |
| `referenceText` | 검수 AI prompt에 넣을 범위별 참고자료. PDF 전체 원문이 아니라 해당 범위에 필요한 짧은 텍스트 |
| `referenceHash` | entry 단위 referenceText 해시 |

## 후속 매칭 기준

- asset의 성경 범위와 entry 성경 범위가 겹치는 항목을 매칭 후보로 본다.
- 여러 entry가 매칭되면 성경 순서대로 정렬해 합친다.
- 후속 구현은 prompt 최대 길이를 넘지 않도록 referenceText를 제한해야 한다.
- 매칭 entry가 없거나 index를 읽을 수 없으면 layer 2는 `NEEDS_REVIEW`로 남기고 승인 게이트에서 차단한다.
- `sourceFileHash`가 validation reference job의 `sourceFileHash`와 다르면 설정 오류로 보고 `NEEDS_REVIEW` 처리한다.

## 저장/노출 정책

- `referenceText`는 LLM prompt 입력으로만 사용한다.
- `ai_validation_logs.checklistJson`에는 `validationReferenceJobId`, `referenceRangeLabel`, `referenceHash`, `sourceFileHash` 같은 metadata만 남긴다.
- `referenceText` 전문, PDF 원문, 긴 excerpt, provider raw response, prompt 원문, secret/token/password/privateKey는 DB 로그와 감사 로그에 저장하지 않는다.
- `indexStorageUri`는 기존 정책대로 일반 API 응답에 노출하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 계약 설계 문서와 report 작성 중심이다.
- JSON shape, 용어, 후속 구현 기준이 한 문서 안에서 일관되어야 한다.
- 병렬 작업으로 나누면 같은 필드 의미와 저장 정책을 다르게 표현할 위험이 크다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec 작성, runner 기준 검증, report 작성을 직접 수행한다.

## 검증 계획

- `git status --short --branch`로 브랜치와 변경 파일을 확인한다.
- workflow/report 문서에서 placeholder marker와 불완전한 결정 표현이 없는지 확인한다.
- Java production/test code, DB migration, OpenAPI 변경이 없는지 확인한다.
- 이번 작업은 코드 변경 없는 계약 설계 문서 작업이므로 Gradle 테스트는 실행하지 않는다.

## 후속 작업으로 남길 항목

- `AiReviewReferenceExcerptService` 구현
- `indexStorageUri` JSON reader/adapter 구현
- 산출물 성경 범위와 index entry 범위 매칭 구현
- layer 2 prompt에 `referenceExcerpt` 또는 `referenceSummary` 주입
- reference text 길이 제한과 초과 시 요약/절단 정책 구현
