# Report - 2026-06-03 ai-review-validation-layer-v2

## Summary

- 브랜치: `feature/ai-review-validation-layer-v2`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-03_ai-review-validation-layer-v2.md`
- EXPLANATION 산출물의 layer 1 AUTO `PASSED` 이후 검수 AI layer 2를 자동 실행하도록 연결했다.
- layer 2 검증 로그는 `layer = 2`, `reviewerType = ADVISOR`로 저장한다.
- 관리자 approve 조건을 최신 layer 1 AUTO `PASSED` + 최신 layer 2 ADVISOR `PASSED`로 확장했다.

## 구현 내용

- `AiReviewValidationService`를 추가해 검수 AI 응답을 `PASSED`, `REJECTED`, `NEEDS_REVIEW`로 정규화하고 `ai_validation_logs`에 layer 2 ADVISOR 로그를 남긴다.
- `AiAutoValidationService`는 layer 1 AUTO 로그가 `PASSED`일 때만 layer 2 검수 AI를 후속 실행한다.
- `AiAssetReviewService.approve()`는 layer 1 AUTO 로그와 layer 2 ADVISOR 로그를 각각 최신 기준으로 조회하고, 둘 다 `PASSED`일 때만 승인한다.
- `checklistVersionId`는 승인 요청 입력값이 아니라 검증 로그가 참조한 `ai_validation_checklist_versions.id`로 유지했다.
- OpenAPI, `04_API_명세서.md`, `05_시퀀스_다이어그램.md`의 승인 조건 설명을 layer 2 기준으로 동기화했다.

## 테스트

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewValidationServiceTest" --tests "*AiAutoValidationServiceTest" --tests "*AiAssetReviewServiceTest" --tests "*AiAssetReviewFlowIntegrationTest"
```

- 결과: PASS

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server build
```

- 결과: PASS

## 미실행 / 환경 제약

- `jacocoTestReport`, `jacocoTestCoverageVerification`: 현재 Gradle 프로젝트에서 task 존재 여부를 이번 작업에서 별도 확인하지 않았다.
- Spectral, gitleaks: 로컬 도구 설치 여부 확인 전이며 이번 변경 검증은 Gradle focused test와 build 중심으로 수행했다.

## 후속 작업

- `ai_validation_checklist_versions`에 실제 체크리스트 본문을 저장할지 정책 결정이 필요하다. 현재 구현은 ERD/코드에 존재하는 `version`, `contentHash` 메타데이터와 산출물 payload를 검수 AI 입력 기준으로 사용한다.
- SIMULATOR, QA_RESPONSE layer 2 검수 확장은 별도 workflow로 진행한다.
- 관리자 웹에서 layer 1 AUTO와 layer 2 ADVISOR 로그를 구분해서 보여주는 UI 정리가 필요하다.
