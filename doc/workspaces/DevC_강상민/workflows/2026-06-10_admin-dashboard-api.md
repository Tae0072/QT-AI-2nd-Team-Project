# Workflow — 2026-06-10 admin-dashboard-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-dashboard-api` |
| PR 대상 | `dev-msa` |
| 관련 F-ID | F-06 |
| 트리거 | 관리자 웹 AD-01 대시보드가 호출할 `GET /api/v1/admin/dashboard` 백엔드 API 신설 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/admin-server/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`admin-server`에 AD-01 관리자 대시보드 API를 추가한다. 응답은 운영자가 첫 화면에서 확인할 최소 요약 지표만 포함하고, 관리자 웹 화면 구현은 별도 담당자 범위로 남긴다.

## 범위

- `GET /api/v1/admin/dashboard`를 `ApiResponse<AdminDashboardResponse>` envelope로 제공한다.
- `ROLE_ADMIN` 1차 검증 후 `VerifyAdminRoleUseCase.verifyAnyRole(memberId, ["OPERATOR", "REVIEWER"])`로 2차 검증한다. `SUPER_ADMIN`은 기존 우월권으로 통과하고 `CONTENT_CREATOR`는 403이다.
- AI 대기 검증 건수는 신규 summary UseCase 없이 기존 `GetAdminAiMonitoringUseCase`의 `validation.waitingAssets`를 재사용한다.
- 신고 건수는 `ReportStatus.RECEIVED`, `ReportStatus.REVIEWING` count 기준으로 제공한다.
- 최근 감사 로그는 dashboard 전용 sanitized DTO로 제공한다.
- OpenAPI에 `/api/v1/admin/dashboard`와 응답 schema를 추가한다.

## 제외 범위

- `admin-web` 화면, 타입, API 호출 코드 변경.
- QT 본문 관리 상세, 공지, 회원 통계, 관리자 계정 관리.
- AI 산출물 원문, prompt/provider 원문, reason 원문 노출.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/api/GetAdminDashboardUseCase.java` | AD-01 조회 UseCase 계약 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/api/dto/AdminDashboardResponse.java` | AD-01 응답 DTO |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/internal/AdminDashboardService.java` | 권한 검증 후 요약 지표 조립 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/report/api/GetAdminReportDashboardSummaryUseCase.java` | 신고 상태별 count 계약 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/report/api/dto/AdminReportDashboardSummary.java` | 신고 요약 DTO |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/report/internal/*` | RECEIVED/REVIEWING count 구현 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/audit/api/*`, `domain/audit/internal/*` | 최근 감사 로그 sanitized 조회 계약/구현 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/web/AdminController.java` | `/dashboard` 엔드포인트 추가 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | AD-01 OpenAPI 계약 추가 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/admin/**` | controller/service 테스트 |

## 구현 순서

1. workflow 문서를 저장한다.
2. admin dashboard service/controller 테스트를 먼저 추가한다.
3. `AdminDashboardResponse`와 `GetAdminDashboardUseCase`를 추가한다.
4. 신고 dashboard summary UseCase와 repository count 메서드를 추가한다.
5. audit dashboard recent UseCase와 sanitized DTO를 추가한다.
6. `AdminDashboardService`에서 AI monitoring, report summary, today QT, audit recent를 조립한다.
7. `AdminController`에 `/dashboard` GET을 추가한다.
8. OpenAPI schema와 path를 추가한다.
9. 검증 명령을 실행하고 report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `AdminDashboardControllerTest` | OPERATOR/REVIEWER/SUPER_ADMIN 200, CONTENT_CREATOR 403, ROLE_USER 403, 미인증 401/403 |
| `AdminDashboardServiceTest` | `waitingAssets` → `pendingAiValidationCount`, 신고 count, todayQt non-null/missing, 감사 로그 sanitized DTO |
| `AdminReportDashboardSummaryServiceTest` | RECEIVED/REVIEWING count 매핑 |
| `AdminAuditDashboardRecentServiceTest` | 최근 감사 로그 정렬과 민감 필드 제외 |

## 수용 기준

- [ ] `GET /api/v1/admin/dashboard`가 확정 중첩 응답 구조를 반환한다.
- [ ] `todayQt`는 항상 non-null이며 오늘 QT가 없으면 `MISSING` 규칙을 따른다.
- [ ] 최근 감사 로그 응답에 `beforeJson`, `afterJson`, AI payload, prompt/provider 원문, reason 원문이 없다.
- [ ] admin role 2차 검증이 테스트로 확인된다.
- [ ] `admin-web` 파일은 수정하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `admin-server`의 API 계약, 조립 서비스, 테스트, OpenAPI에 집중되어 있다.
- controller/service/DTO/OpenAPI 응답 구조를 한 흐름으로 맞춰야 하므로 직접 실행이 안전하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :admin-server:build
cd ..
npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml
git diff --check
```

## 후속 작업으로 남길 항목

- `admin-web` AD-01 실제 화면/타입/API 연동.
- 관리자 공지, 회원 통계, QT 관리 상세 지표.
