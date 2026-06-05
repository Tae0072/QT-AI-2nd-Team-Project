# Report - 2026-06-05 ai-review-reference-index-storage-uri-flow

## 개요

- 브랜치: `test/ai-reference-index-storage-uri-flow`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-index-storage-uri-flow.md`
- 목표 URI: `restricted://validation/index/reference-index.json`
- 목적: `validation_reference_jobs.indexStorageUri`가 DB metadata로 저장/조회되고 layer 2 검수 흐름에서 동일 URI로 index reader에 전달되는지 검증

## 변경 요약

- `ValidationReferenceJobRepositoryTest`
  - `.json` 파일명까지 포함한 최종 URI 저장/조회 검증 추가
- `AiReviewReferenceServiceTest`
  - 최신 ACTIVE reference metadata가 최종 URI를 반환하도록 fixture와 assertion 보강
- `AiReviewReferenceIndexStorageUriFlowTest`
  - synthetic 소형 index 파일을 임시 restricted root에 생성
  - DB에 저장된 ACTIVE job metadata를 service로 조회
  - 조회된 URI와 hash로 reader를 호출한 뒤 asset verse range와 겹치는 항목 선택 검증
- `AiReviewValidationServiceTest`
  - layer 2가 최종 URI를 reader에 그대로 전달하는지 mock verification 추가
  - prompt/checklist metadata의 URI 값이 최종 URI인지 검증
  - checklist/log에는 hash와 range label 중심의 metadata만 남는 정책 유지

## 검증 결과

### Focused test

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*ValidationReferenceJobRepositoryTest" --tests "*AiReviewReferenceServiceTest" --tests "*AiReviewReferenceIndexStorageUriFlowTest" --tests "*AiReviewValidationServiceTest"
```

결과: `BUILD SUCCESSFUL`

비고: 기존 안내 경로 `C:\Users\HSystem\.jdks\temurin-21`는 현재 PC에 없어 `C:\TOOLS\jdk-21`로 실행했다.

### 흐름 검증

- DB row 저장/조회: 통과
- 최신 ACTIVE metadata 반환: 통과
- metadata URI 기반 restricted reader 호출: 통과
- asset verse range 기준 선택: 통과
- layer 2 reader 전달 URI 보존: 통과
- checklist/log 민감 텍스트 미저장 정책: 통과

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

### report 안전 키워드 확인

지정된 안전 검색 명령 실행 결과: 출력 없음

## 판정

DB row 저장/조회, reference metadata 반환, restricted reader 연결, layer 2 내부 전달 흐름은 테스트로 검증됐다.

이번 작업은 실제 system API 서버 호출과 운영 restricted root 환경 검증은 포함하지 않는다. 해당 검증은 후속 작업으로 남긴다.
