# Report - 2026-06-12 admin-dashboard-qa-stabilization

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-12_admin-dashboard-qa-stabilization.md` |
| 관련 화면 | admin-web `/dashboard` |
| 관련 API | `GET /api/v1/admin/dashboard` |
| 관련 F-ID | AD-01, F-14 |

## 요약

관리자 웹 QA 루틴의 첫 대상으로 대시보드를 확인했다. API는 정상 응답했고, 화면도 지표 카드, 오늘 QT, 최근 관리자 활동을 렌더링했다. 수정이 필요한 항목은 두 가지였다.

- 권한 확인 로딩에서 Ant Design `Spin tip` 경고가 발생했다.
- 최근 관리자 활동에서 `actorType=ADMIN`이지만 `adminUserId=null`인 로그가 `ADMIN -`로 표시되어 운영자가 의미를 알기 어려웠다.

## 변경 내용

### 1. 권한 확인 로딩 경고 제거

`ProtectedRoute`에서 단독 `<Spin tip="관리자 권한 확인 중" />`을 사용하던 구조를 변경했다.

변경 후:

- `Spin`은 아이콘만 담당한다.
- 안내 문구는 `Typography.Text`로 분리한다.
- Ant Design의 `tip only work in nest or fullscreen pattern` 경고를 유발하지 않는다.

수정 파일:

- `admin-web/src/routes/ProtectedRoute.tsx`

### 2. 대시보드 행위자 표시 개선

최근 관리자 활동에서 `adminUserId`가 없는 `ADMIN` 로그를 `-`로 표시하지 않고 `관리자 정보 없음`으로 표시하도록 정리했다.

수정 전:

- `ADMIN -`

수정 후:

- `ADMIN 관리자 정보 없음`

시스템 배치 로그는 `SYSTEM_BATCH 시스템`으로 표시할 수 있도록 분기 함수를 추가했다.

수정 파일:

- `admin-web/src/pages/DashboardPage.tsx`

## 확인한 API 응답

`GET /api/v1/admin/dashboard`는 로컬 superadmin 토큰으로 정상 응답했다.

주요 값:

- `pendingAiValidationCount`: `856`
- `receivedReportCount`: `1`
- `reviewingReportCount`: `0`
- `todayQt.status`: `READY`
- `todayQt.simulatorStatus`: `MISSING`
- `recentAuditLogs`: 5건

## 브라우저 확인

확인 URL:

- `http://localhost:5173/dashboard`

확인 결과:

- 대시보드 헤더와 설명 표시.
- 지표 카드 3개 표시.
- 오늘 QT 날짜, 상태, 제목, 시뮬레이터, 해설, 캐시 표시.
- 최근 관리자 활동 테이블 표시.
- `AI_REGENERATE_REQUEST` 행의 행위자 상세가 `관리자 정보 없음`으로 표시.

## 검증 결과

| 명령 / 확인 | 결과 |
| --- | --- |
| `cd admin-web; npm.cmd run typecheck` | PASS |
| `git diff --check -- admin-web/src/routes/ProtectedRoute.tsx admin-web/src/pages/DashboardPage.tsx doc/workspaces/DevC_강상민/workflows/2026-06-12_admin-dashboard-qa-stabilization.md` | PASS, CRLF warning만 출력 |
| Browser `/dashboard` | PASS |

## 남은 경고

브라우저 콘솔에는 이전 화면 이동 중 기록된 경고가 남아 있었다.

- React Router v7 future flag warning: 기존 라이브러리 경고.
- Ant Design Modal `destroyOnClose` deprecated warning: AI 산출물 화면의 modal에서 발생한 로그로, 대시보드 코드 수정 범위가 아니다.

대시보드 진입 후 새로 발생하는 `Spin tip` 경고는 재현되지 않았다.

## 후속 작업

- AI 산출물 화면 QA 시 `destroyOnClose`를 `destroyOnHidden`으로 바꿀지 판단한다.
- 대시보드 지표 의미가 운영자에게 더 명확해야 하면 서버/문서 계약 확인 후 문구를 조정한다.
- 다음 QA 대상은 사용자가 보는 화면 기준으로 별도 workflow/report를 작성한 뒤 진행한다.
