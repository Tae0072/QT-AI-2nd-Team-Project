# Workflow - 2026-06-03 ai-review-validation-layer-v2

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-validation-layer-v2` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 서버 최소 자동 검증 layer 1 이후, 검수 AI layer 2 결과까지 승인 조건에 포함해야 함 |
| 기준 문서 | `AGENTS.md`, `02_ERD_문서.md`, `04_API_명세서.md`, `03_아키텍처_정의서.md` 도메인 경계 기준 |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

EXPLANATION AI 산출물에 대해 layer 1 서버 자동 검증이 `PASSED`된 뒤 검수 AI layer 2를 자동 실행한다. layer 2 결과는 `ai_validation_logs`에 `layer = 2`, `reviewerType = ADVISOR`로 저장하고, 관리자 승인은 최신 layer 1 AUTO 로그와 최신 layer 2 ADVISOR 로그가 모두 `PASSED`일 때만 허용한다.

`checklistVersionId`는 관리자가 승인 요청에서 고르는 값이 아니라, 검증 로그가 어떤 `ai_validation_checklist_versions.id`를 기준으로 생성됐는지 추적하는 FK로 유지한다.

## 범위

- layer 1 AUTO `PASSED` 후 EXPLANATION 산출물에 대해 검수 AI layer 2 자동 실행
- layer 2 로그는 활성 EXPLANATION checklist version id를 `checklistVersionId`로 기록
- layer 2 로그는 `reviewerType = ADVISOR`, `layer = 2`로 저장
- 검수 AI 응답은 `PASSED`, `REJECTED`, `NEEDS_REVIEW` 중 하나로 정규화
- 승인 조건을 최신 layer 1 AUTO `PASSED` + 최신 layer 2 ADVISOR `PASSED`로 확장
- 관련 service/unit/integration 테스트와 OpenAPI 승인 설명 갱신

## 제외 범위

- 새 관리자 API 추가
- 관리자가 검수 AI를 수동 실행하는 기능
- DB schema 변경
- `ai_validation_checklist_versions`에 체크리스트 본문 컬럼 추가
- 관리자 웹 프런트엔드 변경
- EXPLANATION 외 SIMULATOR, QA_RESPONSE layer 2 구현
- 검증 참조 원문, provider raw response, prompt 원문 저장

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewValidationService.java` | EXPLANATION 산출물 검수 AI 호출, 응답 정규화, layer 2 ADVISOR 로그 등록 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAutoValidationService.java` | layer 1 PASSED 후 layer 2 검수 AI 후속 실행 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | 승인 조건을 layer 1 + layer 2 통과로 확장 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | approve 설명을 layer 2 승인 조건까지 반영 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewValidationServiceTest.java` | layer 2 로그 생성, 응답 정규화, 실패/검토 필요 케이스 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAutoValidationServiceTest.java` | layer 1 PASSED 후 layer 2 실행, layer 1 REJECTED 시 미실행 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | layer 2 ADVISOR PASSED 필수 승인 조건 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewFlowIntegrationTest.java` | 승인 통합 흐름에서 layer 1/2 로그 조건 검증 |

## 구현 순서

1. workflow spec을 먼저 저장한다.
2. `AiReviewValidationServiceTest`를 추가해 layer 2 `PASSED` 로그 생성 기대를 먼저 실패시킨다.
3. `AiAutoValidationServiceTest`에 layer 1 `PASSED` 후 layer 2 실행 기대를 추가해 실패를 확인한다.
4. `AiAssetReviewServiceTest`에 layer 2 로그 없음/실패/통과 승인 조건 테스트를 추가해 실패를 확인한다.
5. `AiReviewValidationService`를 최소 구현한다.
6. `AiAutoValidationService`가 layer 1 결과가 `PASSED`일 때만 `AiReviewValidationService`를 호출하도록 연결한다.
7. `AiAssetReviewService.approve()`가 최신 layer 1 AUTO 로그와 최신 layer 2 ADVISOR 로그를 각각 조회하도록 변경한다.
8. 통합 테스트와 OpenAPI 설명을 갱신한다.
9. focused test와 가능하면 전체 build를 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AiReviewValidationServiceTest` | 검수 AI가 `PASSED`를 응답하면 layer 2 ADVISOR `PASSED` 로그 생성 |
| `AiReviewValidationServiceTest` | 검수 AI가 `REJECTED`를 응답하면 layer 2 ADVISOR `REJECTED` 로그 생성 및 asset REJECTED 전이 |
| `AiReviewValidationServiceTest` | 검수 AI 응답이 JSON이 아니면 `NEEDS_REVIEW` 로그 생성 |
| `AiAutoValidationServiceTest` | layer 1 AUTO `PASSED` 후 layer 2 검수 AI 실행 |
| `AiAutoValidationServiceTest` | layer 1 AUTO `REJECTED`면 layer 2 검수 AI 미실행 |
| `AiAssetReviewServiceTest` | layer 1 AUTO + layer 2 ADVISOR 모두 `PASSED`면 승인 성공 |
| `AiAssetReviewServiceTest` | layer 2 로그 없음, `REJECTED`, `NEEDS_REVIEW`이면 승인 실패 |
| `AiAssetReviewFlowIntegrationTest` | 실제 repository 기반 승인 흐름이 layer 2 ADVISOR 로그를 요구 |

## 수용 기준

- [ ] layer 1 AUTO `PASSED` 후 layer 2 ADVISOR 검증 로그가 생성된다.
- [ ] layer 1 AUTO `REJECTED`면 layer 2 검수 AI를 실행하지 않는다.
- [ ] layer 2 검수 AI 로그는 `checklistVersionId`에 활성 EXPLANATION 체크리스트 버전 id를 저장한다.
- [ ] approve는 최신 layer 1 AUTO 로그와 최신 layer 2 ADVISOR 로그가 모두 `PASSED`일 때만 성공한다.
- [ ] layer 2 로그가 없거나 `PASSED`가 아니면 approve는 `INVALID_STATUS_TRANSITION`으로 실패한다.
- [ ] provider raw response, prompt 원문, 검증 참조 원문, secret/token/password/privateKey를 저장하지 않는다.
- [ ] 새 관리자 API나 DB schema 변경은 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- layer 2 로그 생성과 승인 게이트가 같은 검증 로그 계약에 묶여 있어 순차 확인이 안전하다.
- `AiAutoValidationService`, 새 layer 2 service, `AiAssetReviewService` 테스트가 같은 repository method와 상태 전이를 공유한다.
- 병렬 작업 시 같은 테스트 헬퍼와 승인 조건을 중복 수정할 가능성이 높다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec 저장, TDD 테스트 추가, 구현, focused 검증, 보고를 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewValidationServiceTest" --tests "*AiAutoValidationServiceTest" --tests "*AiAssetReviewServiceTest" --tests "*AiAssetReviewFlowIntegrationTest"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server build
```

전체 build가 환경 문제로 실행되지 않으면 focused test와 실패 사유를 최종 보고에 남긴다.

## 후속 작업으로 남길 항목

- `ai_validation_checklist_versions`에 체크리스트 본문을 저장할지 여부 검토
- SIMULATOR, QA_RESPONSE layer 2 검수 확장
- 관리자 웹에서 layer 1/2 검증 로그를 더 명확하게 구분 표시
- 검수 AI 프롬프트 품질 고도화와 운영 모니터링 지표 추가
