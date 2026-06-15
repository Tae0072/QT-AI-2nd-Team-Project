# Report - 2026-06-15 AD-03/AD-08 AI 산출물 검증 로그 연동

## 요약

AD-08 AI 운영 모니터링의 `검증` 카드가 두 종류의 데이터를 한 줄에서 섞어 보여주고 있었다. `대기`는 `ai_generated_assets.status = VALIDATING` 기준의 현재 산출물 상태였고, `통과/실패/검토`는 `ai_validation_logs.result` 기준의 기간 내 검증 로그였다.

또한 AD-03에서 관리자가 산출물을 반려해도 `ai_generated_assets.status`만 `REJECTED`로 바뀌고 `ai_validation_logs`에는 관리자 반려 로그가 남지 않았다. 그래서 AD-08에서는 산출물 반려 상태와 검증 로그 실패/사유 집계가 연결되지 않았다.

이번 작업으로 AD-08은 상단 요약을 `생성 작업`, `산출물 상태`, `검증 로그`, `Q&A` 4개 카드로 분리하고, AD-03 관리자 승인/반려는 `ai_validation_logs`에 `reviewerType=ADMIN`, `layer=3` 로그를 남기도록 연결했다.

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 작업일 | 2026-06-15 |
| 브랜치 | `bugfix/admin-ai-monitoring-validation-log` |
| PR 대상 | `dev` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-15_admin-ai-monitoring-validation-log.md` |
| 관련 F-ID | AD-03, AD-08 |

## 원인

- AD-08 `AdminAiMonitoringResponse.Validation.waitingAssets`는 `AiGeneratedAssetStatus.VALIDATING` 산출물 수를 의미했다.
- 같은 응답의 `passCount`, `failCount`, `needsReviewCount`는 `ai_validation_logs.result` 값을 의미했다.
- AD-03 `AiAssetReviewService.reviewAiAsset`는 관리자 승인/반려/숨김 후 감사 로그만 남기고 `AiValidationLogRepository.save(...)`를 호출하지 않았다.
- 문서와 OpenAPI에는 `AiValidationReviewerType.ADMIN`이 이미 정의되어 있었지만, 수동 관리자 리뷰 결과 저장 경로가 비어 있었다.

## 변경 내용

### 백엔드

- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/api/admin/monitoring/dto/AdminAiMonitoringResponse.java`
  - AD-08 검증 응답에 `approvedAssets`, `rejectedAssets`, `hiddenAssets`를 추가했다.
  - 기존 생성자 호환성을 유지했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepository.java`
  - `reviewed_at` 기간 기준 승인/반려/숨김 산출물 상태 집계를 추가했다.
  - 기존 `ai_validation_logs` 결과 집계와 산출물 상태 집계를 분리했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryService.java`
  - 새 산출물 상태 집계를 응답 DTO에 매핑했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java`
  - 관리자 승인 성공 시 `ADMIN/PASSED/layer=3` 검증 로그를 저장한다.
  - 관리자 반려 성공 시 `ADMIN/REJECTED/layer=3` 검증 로그와 반려 사유를 저장한다.
  - 숨김은 검증 실패 로그로 저장하지 않는다.
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`
  - admin-server와 동일한 DTO, 쿼리, 리뷰 서비스 변경을 반영했다.

### 프런트엔드

- `admin-web/src/api/aiMonitoring.ts`
  - AD-08 검증 타입에 `approvedAssets`, `rejectedAssets`, `hiddenAssets`를 추가했다.
- `admin-web/src/pages/AiMonitoringPage.tsx`
  - 상단 요약을 `생성 작업`, `산출물 상태`, `검증 로그`, `Q&A` 4개 카드로 분리했다.
  - 산출물 상태는 `대기/승인/반려/숨김`을 표시한다.
  - 검증 로그는 `통과/실패/검토`를 표시한다.
  - 실패 사유 표 제목을 `검증 로그 실패 사유`로 명확히 했다.
- `admin-web/scripts/admin-page-contracts.test.mjs`
  - AD-08 상단 요약이 산출물 상태와 검증 로그를 별도 카드로 표시하는지 계약 테스트를 추가했다.

### 계약

- `qtai-server/apis/api-v1/openapi.yaml`
  - `AdminAiMonitoringValidation` 스키마에 `approvedAssets`, `rejectedAssets`, `hiddenAssets`를 추가했다.

## 테스트 보강

- `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java`
  - `VALIDATING` 대기, 승인, 반려, 숨김 산출물 상태와 `ai_validation_logs` 실패 집계가 서로 독립적으로 집계되는지 검증했다.
- `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java`
  - 관리자 승인 시 `ADMIN/PASSED/layer=3` 로그 저장을 검증했다.
  - 관리자 반려 시 `ADMIN/REJECTED/layer=3` 로그와 반려 사유 저장을 검증했다.
- `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java`
  - service-ai 복제 코드에서도 동일한 관리자 검증 로그 저장을 검증했다.
- `admin-web/scripts/admin-page-contracts.test.mjs`
  - `AI monitoring separates asset status and validation log summary cards` 테스트를 추가했다.

## 검증 결과

| 구분 | 명령 | 결과 |
| --- | --- | --- |
| admin-server 타깃 테스트 | `.\gradlew.bat :admin-server:test --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest" --tests "com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest" --rerun-tasks --console=plain` | 통과 |
| service-ai 타깃 테스트 | `.\gradlew.bat :service-ai:test --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest" --rerun-tasks --console=plain` | 통과 |
| admin-web 타입체크 | `npm.cmd run typecheck` (`admin-web`) | 통과 |
| admin-web 계약 테스트 | `npm.cmd test` (`admin-web`) | 통과 |
| diff 공백 검사 | `git diff --check` | 통과, LF to CRLF 경고만 출력 |
| Spectral | `npx.cmd @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml` | 실패, 루트 `.spectral.yaml` 파일 없음 |
| gitleaks | `gitleaks detect --source . --redact --exit-code 1` | 실패, 로컬에 `gitleaks` 명령 없음 |
| 로컬 Docker 반영 | `.\gradlew.bat :admin-server:bootJar --rerun-tasks --console=plain` 후 `docker compose up -d --build service-admin` | 통과, admin-server 컨테이너 재기동 |
| 로컬 API 확인 | `GET http://localhost:8090/api/v1/admin/ai/monitoring` | `validation` 응답에 `approvedAssets`, `rejectedAssets`, `hiddenAssets` 포함 확인, `rejectedAssets=8` 확인 |
| 로컬 화면 확인 | `http://localhost:5173/ai-monitoring` | `산출물 상태 > 반려 8` 표시 확인 |

## 수용 기준 확인

- AD-08 대기 수는 `VALIDATING` 산출물 기준으로 유지된다.
- AD-03 목록 기본 필터는 `VALIDATING` 기준으로 유지된다.
- AD-08 화면에서 상단 요약이 4개 카드로 정렬되고 산출물 상태와 검증 로그가 분리 표시된다.
- AD-03 승인 시 관리자 검증 통과 로그가 저장된다.
- AD-03 반려 시 관리자 검증 실패 로그와 반려 사유가 저장된다.
- AD-03 숨김은 검증 실패로 집계하지 않고 산출물 숨김 상태로만 집계한다.
- admin-server와 service-ai의 AI 도메인 계약을 같은 형태로 맞췄다.
- OpenAPI AD-08 응답 스키마를 실제 DTO와 맞췄다.
- 로컬 Docker admin-server 이미지가 이전 JAR를 쓰고 있으면 새 필드가 내려오지 않으므로, JAR 재빌드 후 컨테이너 재기동이 필요하다.

## 남은 확인 사항

- 반려 사유를 자유 텍스트로 집계하면 운영 화면의 실패 사유 분포가 지나치게 잘게 나뉠 수 있다. 사유 코드화가 필요하면 별도 AD-03 후속 작업으로 분리한다.
- AD-08에서 검증 로그를 reviewerType별로 분리할 필요가 있으면 API 응답 구조를 한 번 더 확장해야 한다.
- Spectral 규칙 파일과 gitleaks 설치 방식은 팀 공통 개발 환경에 맞춰 정리해야 한다.
- 이전 Docker 이미지가 떠 있던 동안 이미 반려된 산출물은 관리자 `ai_validation_logs`가 생성되지 않았다. 해당 과거 데이터까지 검증 로그 실패로 보정하려면 별도 백필 정책 결정이 필요하다.
