# 2026-06-14 기능 테스트(AD-18)에 '신고 처리 테스트' 추가

> 복원 메모(2026-06-15): 미커밋 stash로만 남아 있던 작업을 복원해 PR로 올린다. 백엔드는 dev의 현재 `AdminReportController`에 `test-seed` 메서드만 **외과적으로 추가**(stash에 섞여 있던 회원-통계 변경분은 이미 dev에 머지돼 있어 가져오지 않음). 프런트는 `SelfTestPage`·`QtPreviewTester`·`adminPageContracts` 헬퍼·`reports.ts seedTestReport`·라우트/메뉴(AD-18)를 복원. (참고: admin-web 계약 테스트는 #674 이후 dev에서 `PraiseSongsPage.tsx` 참조로 이미 깨져 있으며 CI 게이트 아님 — 본 PR과 무관.)

## 요청 (Lead T)
- 신고 처리(AD-04)도 기능 테스트(자가 진단) 화면에서 테스트할 수 있게 해줘.
- (배경) 신고 목록이 비어 있어 처리/반려 흐름을 점검할 수 없음.

## 문제
- 신고 접수(생성)는 사용자 앱 경로(service-note `POST /api/v1/reports`)라 관리자 웹만으로는 신고를 만들 수 없음 → 목록이 항상 비어 처리 테스트 불가.
- admin-server에는 신고 list/resolve/reject만 있고 생성 엔드포인트 없음.

## 구현
**백엔드 (admin-server, AdminReportController)**
- `POST /api/v1/admin/reports/test-seed`(신규) — 자가진단용 테스트 신고 생성.
  - 기존 `CreateReportUseCase`(ReportService 구현, 이미 빈 등록)를 주입해 호출.
  - 안전장치: 대상 존재검증이 없는 **COMMENT** 타입 + 매 호출 **고유 targetId**(`System.currentTimeMillis()`, 중복 신고 차단 회피) + 사유 **TEST**. 신고자는 호출 관리자 본인(members.id).
  - 권한: 기존 `requireOperator`(ROLE_ADMIN + admin_users OPERATOR) 재사용.
- resolve/reject는 비-POST 대상에 부작용 없음(HIDE_TARGET은 POST+action 지정 시만, 알림은 try/catch). 그래서 테스트가 실데이터를 더럽히지 않음(상태 전이만).

**프런트 (admin-web)**
- `api/reports.ts`: `seedTestReport()` + `ReportSeedResult` 추가.
- `pages/SelfTestPage.tsx`: 기존 읽기전용 점검 카드 아래 **'신고 처리 테스트(쓰기)' 카드** 추가.
  - ① 테스트 신고 생성 → ② 처리(인정)/반려 → 상태(RECEIVED→RESOLVED/REJECTED) 표시 → '신고 처리 화면에서 보기'(/reports 이동).
  - 경고 Alert로 실데이터 생성·알림 미발송 안내. 처리 시 `notifyReporter=false`.

## 검증
- ⚠️ 샌드박스 빌드 불가 → 코드 대조만. PR 전 T님 PC 빌드 필수.
- 신규 테스트 `AdminReportControllerTest`(3): test-seed 201 / 401 / 403.
- 대조: 새 경로 `/api/v1/admin/reports/test-seed`는 `AdminControllerSurfaceTest`(/api/v1/admin 하위) 통과. 컨트롤러 생성자에 `CreateReportUseCase` 추가했으나 기존 report 컨트롤러 테스트 없음(영향 없음). 자가진단 페이지 다른 GET 점검은 그대로.

### PR 전 필수 (T님 PC)
```bash
./gradlew -p qtai-server :admin-server:build :admin-server:test
cd admin-web && npm run build && npm run test
```

## 후속
- 테스트 신고 정리(삭제) 기능: 현재 신고 hard-delete UseCase 없음 → 처리/반려로 종결만. 필요 시 테스트 전용 정리 엔드포인트 검토.
- 운영 환경에서 test-seed 노출 차단(프로파일 가드) 검토 — 현재 OPERATOR 권한으로만 보호.

## Git/PR
- 브랜치 제안 `feature/selftest-report-handling` → PR `dev`.
- 커밋: `feat(report): 자가진단 테스트 신고 생성 + 기능 테스트에 신고 처리 시연(AD-18)`.
