# Report - 2026-06-15 AD-08 AI 운영 모니터링 빈 화면 수정

## 요약

`http://localhost:5173/ai-monitoring` 화면이 빈 화면으로 보인 원인은 프론트 런타임 오류였다. Chrome 콘솔에서 `AiMonitoringPage`가 `data.assetStatuses.validating`을 읽는 중 `assetStatuses`가 `undefined`인 응답을 받아 React 루트가 비워지는 것을 확인했다.

OpenAPI와 `admin-web` 타입은 `assetStatuses`를 요구하고, `admin-server` 구현도 해당 필드를 내려주고 있었다. 반면 `service-ai`의 AD-08 DTO/서비스/리포지토리에는 `assetStatuses`가 누락되어 있었다. `service-ai` 계약을 `admin-server`와 맞추고 회귀 테스트를 추가했다.

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 작업일 | 2026-06-15 |
| 브랜치 | `fix/ai-monitoring-asset-statuses` |
| PR 대상 | `dev` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-monitoring-asset-statuses.md` |
| 관련 F-ID | AD-08 |

## 원인

- `admin-web/src/pages/AiMonitoringPage.tsx`는 AD-08 응답의 `data.assetStatuses.validating` 값을 사용한다.
- `admin-web/src/api/aiMonitoring.ts`와 OpenAPI는 `assetStatuses`를 필수 응답 필드로 정의한다.
- `qtai-server/admin-server` 구현은 `assetStatuses`를 포함한다.
- `qtai-server/service-ai` 구현은 같은 AD-08 응답에서 `assetStatuses`를 빠뜨리고 있었다.
- 실행 중인 로컬 컨테이너는 이전 이미지였기 때문에 코드 수정 후에도 컨테이너 재빌드 전까지 화면 증상이 유지될 수 있었다.

## 변경 내용

### Backend

- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/api/admin/monitoring/dto/AdminAiMonitoringResponse.java`
  - `AdminAiMonitoringResponse`에 `AssetStatuses assetStatuses` 필드 추가
  - `AssetStatuses(validating, approved, rejected, hidden)` record 추가
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepository.java`
  - `countAssetStatuses()` 추가
  - `AiGeneratedAssetStatus`별 전체 산출물 상태 집계 추가
  - `Summary`에 `AssetStatusCounts` 추가
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryService.java`
  - `summary.assetStatuses()`를 AD-08 응답 DTO로 매핑

### Test

- `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java`
  - 산출물 상태 집계와 검증 로그 집계가 분리되는지 검증
  - `GetAdminAiMonitoringUseCase` 응답에 `assetStatuses`가 포함되는지 검증

### Runtime

- `:admin-server:bootJar`, `:service-ai:bootJar` 실행
- `docker compose up -d --build service-admin service-ai`로 로컬 컨테이너 재빌드/재시작
- Chrome에서 `http://localhost:5173/ai-monitoring` 새로고침 후 화면 렌더링 확인

## TDD 기록

1. `service-ai`에 `AdminAiMonitoringQueryRepositoryTest`를 추가했다.
2. 구현 전 `.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest`를 실행했다.
3. 테스트는 `summary.assetStatuses()` 및 `response.assetStatuses()` 메서드 부재로 컴파일 실패했다.
4. DTO/리포지토리/서비스를 최소 수정했다.
5. 같은 테스트를 재실행해 통과를 확인했다.

## 검증 결과

| 구분 | 명령 또는 확인 | 결과 |
| --- | --- | --- |
| RED 확인 | `.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest` | 예상 실패. `assetStatuses()` 메서드 부재 컴파일 오류 |
| 대상 테스트 | `.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest` | 통과 |
| service-ai 전체 테스트 | `.\gradlew :service-ai:test` | 통과 |
| admin-web 타입체크 | `npm.cmd run typecheck` | 통과 |
| admin-web 계약 테스트 | `npm.cmd test` | 통과 |
| service-ai build/coverage gate | `.\gradlew :service-ai:build :service-ai:jacocoTestCoverageVerification` | 통과 |
| bootJar | `.\gradlew :admin-server:bootJar :service-ai:bootJar` | 통과 |
| Docker 재기동 | `docker compose up -d --build service-admin service-ai` | 통과 |
| Docker health | `docker compose ps service-admin service-ai` | 두 컨테이너 healthy |
| 브라우저 확인 | Chrome `http://localhost:5173/ai-monitoring` 새로고침 | AD-08 화면 렌더링 확인, 신규 콘솔 에러 없음 |
| diff 공백 검사 | `git diff --check` | 공백 오류 없음. Windows 줄끝 경고만 출력 |

## 미실행 또는 제외 검증

- `.\gradlew -p qtai-server build`
  - 이번 수정은 `service-ai` AD-08 응답 계약 복원에 한정되어 `:service-ai:build`와 관련 테스트로 범위를 좁혔다.
- `.\gradlew -p qtai-server test jacocoTestReport`
  - `:service-ai:test` 및 `:service-ai:jacocoTestCoverageVerification`를 실행했다.
- `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`
  - OpenAPI 파일 변경이 없고, 이번 원인은 구현체 누락이다.
- `gitleaks detect --source . --redact --exit-code 1`
  - 코드/테스트 변경에 secret 예시를 추가하지 않았다. PR 준비 단계에서 전체 보안 스캔 대상으로 남긴다.

## 수용 기준 확인

- AD-08 응답에 `assetStatuses.validating`, `approved`, `rejected`, `hidden`이 포함된다.
- 산출물 상태 집계와 검증 로그 결과 집계가 분리된다.
- `admin-web`의 기존 `AiMonitoringPage`가 `assetStatuses` 누락으로 크래시 나지 않는다.
- 로컬 컨테이너 재기동 후 `AI 운영 모니터링` 화면이 실제 데이터와 함께 표시된다.
- 새로 발생한 브라우저 콘솔 에러는 없다.

## 후속 확인 사항

- `service-ai`와 `admin-server` 중복 AI 도메인 코드 간 계약 drift 방지 장치가 필요하다.
- PR 준비 시 전체 Gradle build, Spectral, gitleaks를 실행해 저장소 전체 품질 게이트를 확인한다.
