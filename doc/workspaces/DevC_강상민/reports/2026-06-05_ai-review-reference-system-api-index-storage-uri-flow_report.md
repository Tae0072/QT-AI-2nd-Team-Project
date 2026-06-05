# Report - 2026-06-05 ai-review-reference-system-api-index-storage-uri-flow

## 개요

- 브랜치: `test/ai-reference-system-api-index-storage-uri-flow`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-system-api-index-storage-uri-flow.md`
- 목표 URI: `restricted://validation/index/reference-index.json`
- 목적: system API 실제 Spring MockMvc 호출이 최종 URI를 DB에 저장하고 최신 ACTIVE reference metadata로 조회되는지 검증

## 변경 요약

- `SystemValidationReferenceJobControllerTest`
  - request fixture와 command assertion을 최종 URI로 갱신
- `AiReviewReferenceSystemApiFlowIntegrationTest`
  - Spring Boot + MockMvc 통합 테스트 추가
  - `ROLE_SYSTEM_BATCH` authority로 system API POST 호출
  - API 응답에서 민감 URI/hash 미노출 확인
  - DB row의 `sourceFileHash`, `storageUri`, `indexStorageUri`, `status` 저장 확인
  - `AiReviewReferenceService.latestActiveReference()` metadata의 id/hash/URI 반환 확인

## 검증 결과

### Focused test

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*SystemValidationReferenceJobControllerTest" --tests "*AiReviewReferenceSystemApiFlowIntegrationTest" --tests "*ValidationReferenceJob*Test" --tests "*AiReviewReferenceServiceTest"
```

결과: `BUILD SUCCESSFUL`

### 흐름 검증

- system API POST 호출: 통과
- system batch authority 인증 경로: 통과
- API 응답 민감 URI/hash 미노출: 통과
- DB row 최종 URI 저장: 통과
- latest ACTIVE metadata 최종 URI 반환: 통과

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

system API 실제 호출 경로에서 `indexStorageUri` 저장과 latest metadata 조회 흐름은 테스트로 검증됐다.

이번 작업은 생산 코드, DB schema, OpenAPI, API 응답 필드를 변경하지 않았다. 운영 system token/service account와 로컬 서버 curl 방식 검증은 후속 작업으로 남긴다.
