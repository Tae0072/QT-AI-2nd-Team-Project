# Report - 2026-06-03 ai-asset-approval-validation-log

## Summary

- 브랜치: `feature/ai-asset-approval-validation-log`
- PR 대상: `dev`
- 작업단위 명세서: `doc/workspaces/DevC_강상민/workflows/2026-06-03_ai-asset-approval-validation-log.md`
- 관리자 AI 산출물 승인 요청에서 `checklistVersionId` 입력을 제거했다.
- 승인 가능 여부는 서버가 최신 layer 1 AUTO 검증 로그의 `PASSED` 여부로 판단하도록 정리했다.

## 구현 내용

- `ReviewAiAssetCommand`에서 `checklistVersionId`를 제거했다.
- `AdminAiAssetController`의 approve/reject/hide 공통 request body에서 `checklistVersionId`를 제거했다.
- `AiAssetReviewService`는 승인 시 최신 `layer = 1`, `reviewerType = AUTO` 검증 로그를 조회하고, 해당 로그가 `PASSED`가 아니면 `INVALID_STATUS_TRANSITION`으로 차단한다.
- `SUMMARY`, `GLOSSARY` 산출물은 관리자 승인 대상이 아니므로 `INVALID_INPUT`으로 차단하는 가드를 명시했다.
- 사용하지 않게 된 `AiValidationLogRepository.findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc` 메서드를 제거했다.
- `qtai-server/apis/api-v1/openapi.yaml`의 approve request schema에서 `checklistVersionId`를 제거하고 설명을 최신 검증 로그 기준으로 갱신했다.
- `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/05_시퀀스_다이어그램.md`의 승인 요청/승인 조건 설명을 최신 layer 1 AUTO 검증 로그 기준으로 동기화했다.

## 리뷰 반영

- Claude 리뷰의 `REQUEST_CHANGES` 항목 중 approval 비대상 assetType 가드 회귀 가능성을 단위 테스트와 서비스 가드로 보강했다.
- 데드코드 후보 repository 메서드는 실제 호출처가 없어 제거했다.
- workflow 문서의 repository 메서드명은 실제 엔티티 필드명 `layer` 기준으로 정정했다.
- OpenAPI/관리자 클라이언트 동기화 위험을 줄이기 위해 같은 PR에서 OpenAPI schema를 갱신했다.
- 검수 AI layer 2는 아직 구현되지 않았으므로 이번 PR 범위에서 제외하고 후속 작업으로 남겼다.

## 검증 결과

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiAssetReviewServiceTest" --tests "*AdminAiAssetControllerTest" --tests "*AiAssetReviewFlowIntegrationTest" --tests "*AiUseCaseContractTest"
```

- 결과: PASS

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server build
```

- 결과: PASS

## 미실행 / 환경 제약

- `jacocoTestReport`, `jacocoTestCoverageVerification`: 현재 Gradle 프로젝트에 태스크가 없어 실행하지 못했다.
- Spectral: 현재 환경에서 `.spectral.yaml`과 `npx`를 확인하지 못해 실행하지 못했다.
- `gitleaks`: 현재 PATH에서 `gitleaks` 명령을 찾지 못해 실행하지 못했다.

## 후속 작업

- 검수 AI layer 2 구현 후 승인 조건을 layer 1 AUTO `PASSED` + layer 2 검수 AI `PASSED`로 확장한다.
- 관리자 웹 프런트엔드가 approve 요청 body에 `checklistVersionId`를 보내지 않도록 동기화한다.
