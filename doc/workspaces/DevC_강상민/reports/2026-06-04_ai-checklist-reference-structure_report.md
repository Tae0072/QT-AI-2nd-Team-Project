# Report - 2026-06-04 ai-checklist-reference-structure

## Summary

- 브랜치: `feature/ai-checklist-content-storage`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-03_ai-checklist-reference-structure.md`
- 작업 유형: 문서/명세 정리
- `ai_validation_checklist_versions`, `validation_reference_jobs`, `ai_validation_logs`의 역할을 구분해 정리했다.
- PDF 해설 원문은 외부 SSoT로 유지하고, 서버는 버전/해시/참조 작업/검수 로그만 추적한다는 기준을 명확히 했다.

## 정리 내용

- `ai_validation_checklist_versions`는 PDF 원문이나 해설 본문 저장소가 아니라 checklist 기준 버전 registry로 정의했다.
- `checklistVersionId`는 승인 요청 입력값이 아니라, 검증 로그가 어떤 기준 버전을 사용했는지 추적하는 FK로 정리했다.
- `validation_reference_jobs`는 PDF 파일명, 파일 해시, 제한 저장소 URI, 인덱스 URI, 상태를 추적하는 참조자료 처리 이력으로 정리했다.
- `ai_validation_logs`는 실제 검수 결과와 함께 `checklistVersionId`, `validationReferenceJobId`를 기록해 검수 근거를 남기는 위치로 정리했다.
- 현재 layer 2 검수 AI prompt는 코드에서 조립되며, PDF 범위별 참고자료 주입은 아직 구현되지 않았다는 점을 명시했다.

## 제외한 작업

- DB schema 변경 없음
- PDF 업로드, 파싱, 임베딩, 인덱싱 구현 없음
- 성경 범위와 PDF 해설 범위 매칭 구현 없음
- `AiReviewValidationService.userPrompt()` 변경 없음
- 관리자 API 또는 OpenAPI 변경 없음
- `ai_validation_checklist_versions`에 원문 본문, prompt 본문, PDF 전문 저장 컬럼 추가 없음

## 검증

```powershell
& 'C:/Program Files/Git/bin/git.exe' status --short --branch
```

- 결과: `feature/ai-checklist-content-storage` 브랜치에서 workflow 문서와 report 문서만 변경 대상이다.

- workflow 문서와 report 문서에 placeholder marker나 불완전한 결정 표현이 남아 있는지 검색했다.
- 결과: 매치 없음.

## 실행하지 않은 검증

- Gradle 테스트는 실행하지 않았다.
- 이유: 이번 작업은 코드, DB, OpenAPI 변경 없는 문서/명세 정리 작업이다.

## 후속 작업

- `validation_reference_jobs`와 활성 checklist version의 연결 정책 확정
- PDF 범위별 해설 추출/인덱싱 방식 설계
- 산출물 target range와 PDF 해설 range 매칭 방식 설계
- `AiReviewValidationService.userPrompt()`에 범위별 참고자료를 주입하는 구현
- layer 2 검수 로그에 `validationReferenceJobId`를 실제로 채우는 구현
- OpenAPI와 프로젝트 문서에서 registry/reference/log 역할 설명 보강
