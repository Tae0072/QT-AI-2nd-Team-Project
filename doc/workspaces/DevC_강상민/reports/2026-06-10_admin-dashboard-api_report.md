# Report — 2026-06-10 admin-dashboard-api

## Summary

- 브랜치: `feature/admin-dashboard-api`
- PR 대상: `dev-msa`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-10_admin-dashboard-api.md`
- 목표: 관리자 웹 AD-01이 호출할 `GET /api/v1/admin/dashboard` 백엔드 API를 신설한다.

## 구현 내용

- `GET /api/v1/admin/dashboard`를 추가했다.
  - 응답은 `ApiResponse<AdminDashboardResponse>` envelope다.
  - `ROLE_ADMIN` 1차 검증 후 `VerifyAdminRoleUseCase.verifyAnyRole(memberId, ["OPERATOR", "REVIEWER"])`로 2차 검증한다.
  - `SUPER_ADMIN`은 기존 우월권으로 통과하고 `CONTENT_CREATOR`는 403으로 차단된다.
- AI 대기 검증 건수는 신규 summary UseCase 없이 기존 `GetAdminAiMonitoringUseCase`를 재사용했다.
  - `pendingAiValidationCount = AdminAiMonitoringResponse.validation.waitingAssets`
- report 도메인에는 dashboard용 신고 요약 UseCase를 추가했다.
  - `receivedReportCount = ReportStatus.RECEIVED count`
  - `reviewingReportCount = ReportStatus.REVIEWING count`
- audit 도메인에는 dashboard 전용 sanitized 최근 로그 UseCase를 추가했다.
  - 포함 필드: `id`, `adminUserId`, `actorType`, `actionType`, `targetType`, `targetId`, `createdAt`
  - 제외 필드: `beforeJson`, `afterJson`, AI payload, prompt/provider 원문, reason 원문
- `todayQt`는 항상 non-null로 조립한다.
  - 오늘 QT가 없으면 `status=MISSING`, `qtDate=KST 오늘`, `qtPassageId/title/simulatorStatus/cacheStatus=null`, `hasExplanation=false`
- `qtai-server/apis/api-v1/openapi.yaml`에 AD-01 path와 `AdminDashboardApiResponse` schema를 추가했다.

## 제외한 내용

- `admin-web` 화면, 타입, API 호출 코드 변경 없음.
- QT 본문 관리 상세, 공지, 회원 통계, 관리자 계정 관리 지표는 후속 작업으로 남김.

## 테스트

- `AdminDashboardControllerTest`
  - OPERATOR/REVIEWER/SUPER_ADMIN 200
  - CONTENT_CREATOR 403
  - ROLE_USER 403
  - 미인증 401 또는 403
  - 최근 감사 로그 응답에 `beforeJson`, `afterJson` 미노출
- `AdminDashboardServiceTest`
  - AI monitoring `waitingAssets` 매핑
  - RECEIVED/REVIEWING 신고 count 매핑
  - todayQt READY/MISSING non-null 규칙
  - sanitized 최근 감사 로그 매핑
- `AdminReportDashboardSummaryServiceTest`
  - `ReportStatus.RECEIVED`, `ReportStatus.REVIEWING` count 매핑
- `AdminDashboardAuditLogServiceTest`
  - `AuditQueryRepository` row에서 dashboard DTO로 민감 snapshot 제외 매핑

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :admin-server:test --tests "*AdminDashboard*" --tests "*AdminReportDashboardSummaryServiceTest"` | PASS |
| `.\gradlew.bat :admin-server:build` | PASS |
| `git diff --check` | PASS, CRLF 변환 경고만 출력 |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | FAIL — 저장소 루트에 `.spectral.yaml` 없음 |
| Python YAML parse + AD-01 path/schema 존재 확인 | PASS |

## 참고

- `npx.ps1`은 Windows PowerShell execution policy로 실행이 차단되어 `npx.cmd`로 재시도했다.
- `npx.cmd` 실행은 가능했지만 `.spectral.yaml` ruleset 파일이 저장소에 없어 공식 Spectral 검증은 완료하지 못했다.
- `admin-web` 파일은 작업 범위에서 제외했고 수정하지 않았다.
