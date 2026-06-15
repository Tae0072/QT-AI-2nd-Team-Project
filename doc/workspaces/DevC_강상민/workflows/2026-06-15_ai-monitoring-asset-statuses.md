# Workflow - 2026-06-15 ai-monitoring-asset-statuses

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevC 강상민 |
| 작업 브랜치 | `fix/ai-monitoring-asset-statuses` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-08 |
| 트리거 | `http://localhost:5173/ai-monitoring` 화면이 빈 화면으로 보이는 문제 확인 |
| 기준 문서 | `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/admin-server-sync-rules.md` |
| 해당 경로 | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`, `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

AD-08 AI 운영 모니터링 화면이 `assetStatuses` 필드를 읽는 중 런타임 오류로 비어 보이는 문제를 해결한다. OpenAPI 및 `admin-web` 타입 계약은 `assetStatuses`를 요구하고 있으므로, `service-ai`의 AD-08 응답 계약을 `admin-server` 구현과 동일하게 복원한다.

이번 작업은 화면 우회나 프론트 방어 코드가 아니라 백엔드 응답 계약 누락을 바로잡는 것을 목표로 한다. 산출물 상태 집계는 검증 로그 결과 집계와 별도로 유지한다.

## 범위

- `service-ai`의 `AdminAiMonitoringResponse`에 `assetStatuses` 응답 필드를 추가한다.
- `service-ai`의 `AdminAiMonitoringQueryRepository`에 산출물 상태별 전체 집계를 추가한다.
- `service-ai`의 `AdminAiMonitoringQueryService`가 `assetStatuses`를 응답 DTO에 매핑하도록 수정한다.
- AD-08 응답이 산출물 상태와 검증 로그 결과를 분리 집계하는 회귀 테스트를 추가한다.
- 로컬 Docker의 `service-admin`, `service-ai` 컨테이너를 새 jar 기반 이미지로 재기동해 화면 재현 상태를 확인한다.

## 제외 범위

- `admin-web` 화면 구조 및 타입은 변경하지 않는다.
- `admin-server`는 이미 `assetStatuses` 계약을 포함하고 있으므로 코드 수정 범위에서 제외한다.
- OpenAPI 스키마는 이미 `assetStatuses`를 요구하고 있으므로 수정하지 않는다.
- 과거 데이터 보정, 신규 DB migration, AI 산출물 상태 정책 변경은 포함하지 않는다.
- 전체 멀티모듈 Gradle build, Spectral, gitleaks는 이번 국소 수정의 필수 완료 범위에서 제외하되 미실행 여부는 report에 기록한다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/api/admin/monitoring/dto/AdminAiMonitoringResponse.java` | AD-08 응답 DTO에 `AssetStatuses` record와 필드 추가 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepository.java` | `AiGeneratedAssetStatus`별 산출물 상태 집계 추가 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryService.java` | 리포지토리 집계를 응답 DTO의 `assetStatuses`로 매핑 |
| Create | `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java` | AD-08 산출물 상태/검증 로그 분리 집계 및 응답 필드 포함 회귀 테스트 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-monitoring-asset-statuses.md` | 작업 흐름 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-15_ai-monitoring-asset-statuses_report.md` | 작업 결과와 검증 기록 |

## 구현 순서

1. Chrome 콘솔에서 빈 화면 원인을 확인한다.
2. `admin-web/src/api/aiMonitoring.ts`, OpenAPI, `admin-server`, `service-ai`의 AD-08 DTO/서비스/리포지토리 계약을 비교한다.
3. `service-ai`에 AD-08 응답 계약 회귀 테스트를 먼저 추가한다.
4. 구현 전 테스트가 `assetStatuses()` 부재로 실패하는지 확인한다.
5. `service-ai` DTO에 `AssetStatuses`를 추가한다.
6. `AdminAiMonitoringQueryRepository`에 `countAssetStatuses()`와 `AssetStatusCounts`를 추가한다.
7. `AdminAiMonitoringQueryService`가 `summary.assetStatuses()`를 응답에 포함하도록 매핑한다.
8. 대상 테스트와 `service-ai` 전체 테스트를 실행한다.
9. `admin-web` 타입체크와 계약 테스트를 실행한다.
10. `service-ai` build 및 JaCoCo coverage verification을 실행한다.
11. `admin-server`, `service-ai` bootJar를 만들고 Docker 컨테이너를 재빌드/재시작한다.
12. Chrome에서 `http://localhost:5173/ai-monitoring`을 새로고침해 화면 렌더링과 콘솔 에러 여부를 확인한다.
13. workflow/report 문서를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java` | `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN` 산출물 상태 집계가 각각 분리 반환되는지 확인 |
| `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java` | 검증 로그 `REJECTED` 집계가 산출물 상태 집계와 섞이지 않는지 확인 |
| `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java` | `GetAdminAiMonitoringUseCase` 응답에 `assetStatuses` 필드가 포함되는지 확인 |

## 수용 기준

- [ ] `service-ai` AD-08 응답에 `assetStatuses.validating`, `approved`, `rejected`, `hidden`이 포함된다.
- [ ] 산출물 상태 집계와 검증 로그 결과 집계가 별도 값으로 유지된다.
- [ ] `admin-web`의 기존 `AiMonitoringPage`가 백엔드 응답 누락으로 크래시 나지 않는다.
- [ ] `service-ai` 대상 테스트와 전체 테스트가 통과한다.
- [ ] `admin-web` 타입체크와 계약 테스트가 통과한다.
- [ ] 로컬 Docker 재기동 후 `http://localhost:5173/ai-monitoring` 화면이 실제 데이터와 함께 렌더링된다.
- [ ] 신규 콘솔 에러가 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `service-ai` AD-08 응답 계약 누락 복원에 집중되어 있다.
- DTO, 리포지토리, 서비스, 테스트가 같은 계약을 공유하므로 한 흐름에서 순차 검증하는 편이 안전하다.
- 이미 원인이 Chrome 콘솔과 백엔드 계약 비교로 특정되었고, 병렬 작업 이점보다 계약 불일치 위험이 더 크다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 원인 확인, TDD 회귀 테스트, 백엔드 계약 복원, 컨테이너 재기동, 브라우저 검증, 문서 작성을 직접 수행한다.

## 검증 계획

```powershell
.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest
.\gradlew :service-ai:test
npm.cmd run typecheck
npm.cmd test
.\gradlew :service-ai:build :service-ai:jacocoTestCoverageVerification
.\gradlew :admin-server:bootJar :service-ai:bootJar
docker compose up -d --build service-admin service-ai
git diff --check
```

브라우저 검증은 Chrome에서 `http://localhost:5173/ai-monitoring` 새로고침 후 AD-08 본문, 산출물 상태 카드, 콘솔 에러 여부를 확인한다.

## 후속 작업으로 남긴 항목

- `service-ai`와 `admin-server` 중복 AI 도메인 코드의 계약 drift를 더 빨리 잡는 모듈 간 계약 테스트 또는 동기화 체크를 별도로 검토한다.
- 전체 `./gradlew -p qtai-server build`, Spectral, gitleaks는 PR 준비 단계에서 환경을 맞춘 뒤 실행한다.
