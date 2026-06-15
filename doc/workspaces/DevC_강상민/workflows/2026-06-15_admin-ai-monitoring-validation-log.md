# Workflow - 2026-06-15 admin-ai-monitoring-validation-log

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevC 강상민 |
| 작업 브랜치 | `bugfix/admin-ai-monitoring-validation-log` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03, AD-08 |
| 트리거 | AD-08 AI 운영 모니터링의 검증 지표와 AD-03 AI 산출물 검증 목록/승인/반려 결과가 서로 다르게 보이는 문제 확인 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`, `admin-web/src/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

AD-08 AI 운영 모니터링의 대기 수와 AD-03 AI 산출물 검증 목록 기본 필터가 같은 기준을 보도록 정렬한다. AD-03에서 관리자가 산출물을 승인하거나 반려했을 때 `ai_generated_assets` 상태뿐 아니라 `ai_validation_logs`에도 관리자 검증 판정이 남도록 연결한다.

운영 화면에서는 현재 산출물 상태 집계와 검증 로그 실행 결과 집계를 분리해서 표시한다. 이로써 `VALIDATING` 대기 건수, 관리자 승인/반려/숨김 상태, 자동/어드바이저/관리자 검증 로그 결과가 한 카드 안에서 섞이지 않게 한다.

## 범위

- AD-08 모니터링 API 응답에 산출물 상태 집계 `approvedAssets`, `rejectedAssets`, `hiddenAssets`를 추가한다.
- AD-08 모니터링 쿼리에서 `ai_generated_assets.reviewed_at` 기준 승인/반려/숨김 건수를 조회한다.
- AD-08 화면의 상단 요약을 `생성 작업`, `산출물 상태`, `검증 로그`, `Q&A` 4개 카드로 분리한다.
- AD-03 관리자 승인/반려 시 `ai_validation_logs`에 `reviewerType=ADMIN`, `layer=3` 로그를 저장한다.
- 관리자 반려 사유는 `ai_validation_logs.error_message`에 저장해서 AD-08 실패 사유 분포에 반영되게 한다.
- `admin-server`와 `service-ai`의 중복 AI 도메인 코드가 같은 계약을 유지하도록 양쪽 모두 반영한다.
- API 명세의 AD-08 응답 스키마에 새 산출물 상태 집계 필드를 반영한다.

## 제외 범위

- AD-05 찬양 큐레이션 404 문구 처리 변경은 이번 코드 변경 범위에서 제외한다. 기존 구현 여부 확인만 수행한다.
- `ai_validation_logs` DB 스키마 변경은 하지 않는다.
- `AiValidationResult`, `AiValidationReviewerType` enum 값 추가는 하지 않는다.
- 숨김 처리는 검증 실패가 아니라 노출 제어이므로 `ai_validation_logs`에 저장하지 않는다.
- 관리자 반려 사유의 코드화 또는 별도 사유 카테고리 테이블 추가는 하지 않는다.
- 전체 Gradle build, 전체 Jacoco 검증, secrets 스캔 환경 구축은 이번 변경 자체에 포함하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/api/admin/monitoring/dto/AdminAiMonitoringResponse.java` | AD-08 응답 DTO에 산출물 상태 집계 필드 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepository.java` | 대기/승인/반려/숨김 산출물 상태와 검증 로그 결과를 분리 집계 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryService.java` | 새 집계 값을 응답으로 매핑 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | 관리자 승인/반려 결과를 `ai_validation_logs`에 저장 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java` | 산출물 상태 집계와 검증 로그 집계 분리 검증 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | 관리자 승인/반려 시 ADMIN 검증 로그 저장 검증 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**` | admin-server와 동일한 AI 도메인 계약 유지 |
| Test | `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | service-ai 쪽 관리자 검증 로그 저장 검증 |
| Modify | `admin-web/src/api/aiMonitoring.ts` | AD-08 API 타입에 산출물 상태 집계 필드 추가 |
| Modify | `admin-web/src/pages/AiMonitoringPage.tsx` | AD-08 검증 카드 표시 구조 분리 |
| Test | `admin-web/scripts/admin-page-contracts.test.mjs` | AD-08 상단 요약 카드 분리 계약 검증 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | AD-08 응답 스키마 필드 추가 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-15_admin-ai-monitoring-validation-log_report.md` | 작업 결과와 검증 기록 |

## 구현 순서

1. AD-08 모니터링 쿼리와 FE 타입을 확인해서 현재 대기 수가 `AiGeneratedAssetStatus.VALIDATING` 기준인지 확인한다.
2. AD-03 산출물 목록 기본 필터가 `VALIDATING`인지 확인해 대시보드 대기 수와 목록 기본값의 기준을 맞춘다.
3. AD-08 기존 `Validation` 응답에서 `waitingAssets`는 산출물 상태, `passCount/failCount/needsReviewCount`는 `ai_validation_logs` 결과라는 혼합 구조를 확인한다.
4. `AdminAiMonitoringQueryRepository`에 `reviewed_at` 기간 기준 승인/반려/숨김 산출물 상태 집계를 추가한다.
5. AD-08 응답 DTO, service 매핑, OpenAPI, FE 타입을 새 필드에 맞춘다.
6. AD-08 화면의 상단 요약을 `생성 작업`, `산출물 상태`, `검증 로그`, `Q&A` 4개 카드로 분리한다.
7. `AiAssetReviewService.reviewAiAsset`에서 관리자 `APPROVE` 성공 후 `ADMIN/PASSED/layer=3` 로그를 저장한다.
8. `AiAssetReviewService.reviewAiAsset`에서 관리자 `REJECT` 성공 후 `ADMIN/REJECTED/layer=3` 로그와 반려 사유를 저장한다.
9. `HIDE`는 검증 로그로 저장하지 않고 기존 감사 로그와 산출물 상태 변경만 유지한다.
10. admin-server와 service-ai의 동일 코드 경로를 같은 방식으로 반영한다.
11. 저장소 테스트와 프런트 계약 테스트를 실행한다.
12. workflow와 report를 작성하고 브랜치가 `dev`가 아닌 작업 브랜치인지 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepositoryTest.java` | `VALIDATING` 대기 1건, 승인 1건, 반려 1건, 숨김 1건, 검증 로그 실패 1건이 각각 분리 집계되는지 검증 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | 관리자 승인 시 `ADMIN/PASSED/layer=3` 검증 로그 저장 검증 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | 관리자 반려 시 `ADMIN/REJECTED/layer=3` 검증 로그와 반려 사유 저장 검증 |
| `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | service-ai 복제 도메인에서도 동일한 관리자 검증 로그 저장 검증 |
| `admin-web/scripts/admin-page-contracts.test.mjs` | AD-08 상단 요약이 `산출물 상태`와 `검증 로그`를 별도 카드로 표시하는지 검증 |

## 수용 기준

- [ ] AD-08 대기 수는 `ai_generated_assets.status = VALIDATING` 기준으로 집계된다.
- [ ] AD-03 산출물 검증 목록 기본 상태 필터는 `VALIDATING`이다.
- [ ] AD-08 화면은 상단 요약을 4개 카드로 나누고, 산출물 상태 `대기/승인/반려/숨김`과 검증 로그 `통과/실패/검토`를 분리 표시한다.
- [ ] AD-03 승인 성공 시 `ai_validation_logs`에 `reviewerType=ADMIN`, `layer=3`, `result=PASSED` 로그가 저장된다.
- [ ] AD-03 반려 성공 시 `ai_validation_logs`에 `reviewerType=ADMIN`, `layer=3`, `result=REJECTED`, `errorMessage=<반려 사유>` 로그가 저장된다.
- [ ] AD-03 숨김 성공은 검증 로그 실패로 집계되지 않고 산출물 상태 숨김으로만 집계된다.
- [ ] `admin-server`와 `service-ai`의 AI 도메인 API/DTO/서비스 계약이 어긋나지 않는다.
- [ ] OpenAPI AD-08 응답 스키마가 실제 응답 DTO와 일치한다.
- [ ] 검증 명령 결과와 미실행 사유가 report에 기록된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 AD-03 리뷰 서비스, AD-08 모니터링 쿼리, FE 표시, OpenAPI가 같은 계약을 공유하는 변경이다.
- `admin-server`와 `service-ai`의 중복 코드가 동시에 맞아야 해서 병렬 분리보다 한 에이전트가 순서대로 대조하는 편이 안전하다.
- 산출물 상태 집계와 검증 로그 집계의 의미를 일관되게 유지해야 하므로 분산 작업 시 용어와 집계 기준이 어긋날 위험이 있다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 코드 조사, BE 수정, FE 타입/화면 수정, OpenAPI 반영, 테스트 실행, 문서 작성을 직접 수행한다.

## 검증 계획

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest" --tests "com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest" --rerun-tasks --console=plain
.\gradlew.bat :service-ai:test --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest" --rerun-tasks --console=plain
npm.cmd run typecheck
npm.cmd test
git diff --check
npx.cmd @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
```

검증 명령은 가능한 범위에서 실행하고, 로컬 도구나 규칙 파일 부재로 실패하는 항목은 report에 원인을 남긴다.

## 후속 작업으로 남기는 항목

- 관리자 반려 사유를 자유 텍스트가 아니라 사유 코드/카테고리로 표준화할지 별도 결정이 필요하다.
- AD-08 검증 로그 집계에서 자동/어드바이저/관리자 reviewerType별 분리 표시가 필요한지 운영 요구를 확인한다.
- Spectral 규칙 파일 위치와 gitleaks 설치 방식은 팀 공통 개발 환경 문서와 맞춰 정리할 필요가 있다.
