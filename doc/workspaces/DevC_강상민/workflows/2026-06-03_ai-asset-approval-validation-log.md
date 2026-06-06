# Workflow - 2026-06-03 ai-asset-approval-validation-log

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-asset-approval-validation-log` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 관리자 AI 산출물 승인 요청에서 `checklistVersionId`를 관리자가 고르는 것처럼 보이는 흐름을 제거하고, 서버가 검증 로그를 승인 근거로 직접 판단하도록 정리 |
| 기준 문서 | `AGENTS.md`, `CODE_CONVENTION.md`, `03_아키텍처_정의서.md` 도메인 경계 기준 |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

관리자 AI 산출물 승인 API에서 `checklistVersionId` 입력을 제거한다. 승인자는 체크리스트 버전을 직접 선택하지 않으며, 서버가 `assetId` 기준 최신 layer 1 AUTO 검증 로그를 조회해 해당 로그가 `PASSED`일 때만 승인한다.

이번 작업은 현재 존재하는 서버 최소 자동 검증 흐름을 승인 근거로 연결하는 정리 작업이다. 검수 AI layer 2 구현과 layer 2 승인 조건 추가는 후속 작업으로 남긴다.

## 범위

- `ReviewAiAssetCommand`에서 `checklistVersionId` 제거
- 관리자 산출물 승인 요청 body에서 `checklistVersionId` 제거
- 승인 서비스가 최신 `validationLayer = 1`, `reviewerType = AUTO` 로그를 직접 조회하도록 변경
- 최신 layer 1 AUTO 로그가 없거나 `PASSED`가 아니면 승인 실패
- 관련 controller/service/contract/integration 테스트 갱신

## 제외 범위

- 검수 AI layer 2 구현
- `AiValidationLog` 저장 구조 변경
- 산출물 목록/상세 응답의 `checklistVersionId` 제거
- 목록 조회의 `checklistVersionId` 필터 제거
- 체크리스트 관리 API 변경
- HTTP 경로 변경
- DB schema 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/admin/asset/dto/ReviewAiAssetCommand.java` | 승인 command 입력 계약에서 checklistVersionId 제거 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | approve request body와 command 매핑 갱신 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationLogRepository.java` | 최신 layer/reviewer 검증 로그 조회 메서드 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | 승인 조건을 최신 AUTO layer 1 로그 기반으로 변경 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | 승인 조건과 실패 케이스 갱신 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | approve 요청 body와 command 검증 갱신 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | command record component 계약 갱신 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewFlowIntegrationTest.java` | 통합 승인 command 생성부 갱신 |

## 구현 순서

1. `dev` 기준 `feature/ai-asset-approval-validation-log` 브랜치에서 작업한다.
2. 테스트에서 `ReviewAiAssetCommand` 생성자와 approve 요청 body의 `checklistVersionId` 기대값을 먼저 제거한다.
3. `AiValidationLogRepository`에 `findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc` 메서드를 추가한다.
4. `AiAssetReviewService.approve()`에서 command의 checklistVersionId와 checklist version 조회를 제거한다.
5. `AiAssetReviewService.approve()`가 최신 layer 1 AUTO 로그를 조회하고 `PASSED` 여부를 검사하도록 변경한다.
6. `AdminAiAssetController` request record와 command 생성 매핑에서 checklistVersionId를 제거한다.
7. 계약/통합/컨트롤러/서비스 테스트를 갱신한다.
8. 지정된 Gradle 테스트를 실행하고 가능하면 전체 테스트를 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AiAssetReviewServiceTest` | checklistVersionId 없이 최신 layer 1 AUTO `PASSED` 로그로 승인 성공 |
| `AiAssetReviewServiceTest` | 최신 layer 1 AUTO 로그가 `REJECTED` 또는 `NEEDS_REVIEW`이면 승인 실패 |
| `AiAssetReviewServiceTest` | layer 1 AUTO 로그가 없으면 승인 실패 |
| `AiAssetReviewServiceTest` | 이미 승인된 asset은 validation log 조회 전에 실패 |
| `AdminAiAssetControllerTest` | `/approve` 요청 body에서 `checklistVersionId` 없이 성공 |
| `AiUseCaseContractTest` | `ReviewAiAssetCommand` record component 목록 갱신 |
| `AiAssetReviewFlowIntegrationTest` | 승인 command 생성자 갱신 |

## 수용 기준

- [ ] `ReviewAiAssetCommand`에 `checklistVersionId`가 없다.
- [ ] `AdminAiAssetReviewRequest`에 `checklistVersionId`가 없다.
- [ ] approve 요청은 `reason`, `activateForTarget`만으로 동작한다.
- [ ] 최신 layer 1 AUTO 검증 로그가 `PASSED`일 때만 승인된다.
- [ ] 최신 layer 1 AUTO 검증 로그가 없거나 실패/검토 필요 상태이면 승인되지 않는다.
- [ ] 목록/상세 응답의 validation log `checklistVersionId`는 유지된다.
- [ ] 검수 AI layer 2 관련 구현은 추가하지 않는다.
- [ ] 도메인 경계와 금지 데이터/민감정보 저장 규칙을 위반하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- DTO, controller, service, repository, 테스트가 하나의 승인 흐름에 강하게 연결되어 있다.
- 테스트 기대값과 구현 계약을 같은 맥락에서 맞춰야 하므로 직접 실행이 더 안전하다.
- 병렬 작업 시 같은 테스트 파일과 command 생성부를 동시에 수정할 가능성이 높다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec을 읽고 직접 구현, 테스트 갱신, 검증을 수행한다.

## 검증 계획

```powershell
.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiAssetReviewServiceTest" --tests "*AdminAiAssetControllerTest" --tests "*AiAssetReviewFlowIntegrationTest" --tests "*AiUseCaseContractTest"
.\qtai-server\gradlew.bat -p qtai-server test
```

전체 테스트가 시간/환경 문제로 실패하거나 실행이 어려우면, 실패 사유와 최소 실행한 focused test 결과를 최종 보고에 남긴다.

## 후속 작업으로 남길 항목

- 검수 AI layer 2 구현
- layer 2 검수 AI 결과가 생긴 뒤 승인 조건을 layer 1 + layer 2 모두 `PASSED`로 확장
- 필요 시 API 문서/OpenAPI에 approve request body 변경 반영
