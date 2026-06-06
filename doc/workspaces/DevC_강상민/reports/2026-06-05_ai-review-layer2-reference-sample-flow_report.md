# Report - 2026-06-05 ai-review-layer2-reference-sample-flow

## 개요

- 브랜치: `test/ai-reference-layer2-sample-flow`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-layer2-reference-sample-flow.md`
- 목표 URI: `restricted://validation/index/reference-index.json`
- 목적: layer 2 검수 service가 DB metadata의 restricted index를 읽고, 산출물 verse range와 겹치는 참조 항목만 prompt에 넣으며, 저장 log에는 원문을 남기지 않는지 검증

## 변경 요약

- `AiReviewReferenceLayer2SampleFlowIntegrationTest`
  - `@SpringBootTest` 기반 layer 2 샘플 흐름 통합 테스트 추가
  - test 전용 임시 restricted root에 synthetic index 생성
  - ACTIVE reference job, ACTIVE explanation checklist, EXPLANATION asset을 DB에 저장
  - 외부 LLM은 mock bean으로 대체해 prompt를 캡처하고 `PASSED` 응답 반환
- 생산 코드, DB schema, OpenAPI, API 응답 필드는 변경하지 않음

## 검증 결과

### Focused test

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceLayer2SampleFlowIntegrationTest" --tests "*AiReviewValidationServiceTest" --tests "*AiReviewReferenceIndexStorageUriFlowTest"
```

결과: `BUILD SUCCESSFUL`

### 흐름 검증

- DB latest ACTIVE metadata 조회: 통과
- restricted URI 기반 index read: 통과
- 산출물 verse range matching 참조 항목 prompt 주입: 통과
- non-matching 참조 항목 prompt 제외: 통과
- layer 2 `ADVISOR/PASSED` log 생성: 통과
- 저장 log의 참조 job id, source hash, index URI 기록: 통과
- 저장 log의 selected hash/range/count 기록: 통과
- 저장 log의 참조 원문 미저장: 통과

## 안전 확인

### 민감 산출물 ignored 상태

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
```

결과:

```text
!! doc/TalkFile_IVP성경배경주석.pdf.pdf
!! qtai-server/build/
!! qtai-server/restricted/
```

### 민감 산출물 staged 제외

```powershell
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
```

결과: 출력 없음

### report 안전 검색

지정된 금지 문자열 검색 결과: 출력 없음

### diff whitespace 확인

`git diff --cached --check` 결과: 출력 없음

## 비고

- 이번 작업은 자동 테스트 재현성을 위해 실제 외부 LLM 호출을 수행하지 않았다.
- 실제 IVP PDF, real index JSON, restricted/build 산출물은 커밋 대상에서 제외했다.
- 운영 server/curl 기반 수동 검증과 외부 LLM까지 포함하는 검증은 후속 작업으로 남긴다.
