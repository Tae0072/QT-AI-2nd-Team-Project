# Workflow - 2026-06-12 admin-dashboard-qa-stabilization

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-01, F-14 |
| 트리거 | 관리자 웹 QA 중 대시보드부터 화면 안정화 진행 |
| 기준 문서 | `04_API_명세서.md`, `doc/workspaces/DevC_강상민/workflows/관리자_웹_페이지_백엔드.md`, `doc/workspaces/DevC_강상민/reports/2026-06-11_코드리뷰-P3a-dashboard_리포트.md` |
| 해당 경로 | `admin-web/src/pages/DashboardPage.tsx`, `admin-web/src/api/dashboard.ts`, `admin-web/src/routes/ProtectedRoute.tsx`, `admin-web/src/utils/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

관리자 웹 QA를 대화형으로 진행하면서, 대시보드(`/dashboard`)에서 발견되는 실제 문제를 작은 단위로 판단하고 수정한다. 고칠 필요가 있는 이슈는 이 workflow를 기준으로 코드 수정 후 브라우저 검증과 report 작성을 반복한다.

이번 단위의 목표는 대시보드의 첫 화면이 운영자가 이해 가능한 상태로 안정적으로 렌더링되는 것이다. API 계약은 유지하고, 서버 변경이 필요한 문제는 프론트에서 임시 우회하지 않고 후속 서버 작업으로 분리한다.

## 범위

- 대시보드 진입, 새로고침, 에러 표시, 빈 상태 표시를 점검한다.
- `GET /api/v1/admin/dashboard` 응답을 `DashboardPage`가 올바르게 렌더링하는지 확인한다.
- 지표 카드, 오늘 QT 상태, 최근 관리자 활동 테이블의 표시 문구와 로딩 상태를 다듬는다.
- 콘솔 경고 중 현재 화면 코드에서 직접 유발하는 항목만 수정한다.
- 수정 후 `npm.cmd run typecheck`, 필요 시 `npm.cmd run build`, 브라우저 수동 검증을 수행한다.

## 제외 범위

- 대시보드 서버 API 응답 필드 변경.
- 권한 정책 변경.
- 다른 관리자 페이지의 기능 수정.
- 대시보드 지표 종류 추가.
- 전체 관리자 웹 디자인 시스템 재작성.
- AI 산출물 재생성 서버 계약 변경. 해당 내용은 DevC workflow/report로 분리한다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/DashboardPage.tsx` | 대시보드 렌더링, 로딩/에러/빈 상태, 표시 문구 정리 |
| Modify | `admin-web/src/api/dashboard.ts` | 백엔드 응답 타입이 실제 계약과 어긋날 때만 수정 |
| Modify | `admin-web/src/routes/ProtectedRoute.tsx` | 대시보드 진입 시 권한 확인 로딩 경고 제거 |
| Optional | `admin-web/src/utils/datetime.ts` | 날짜 표시 문제가 재현될 때만 수정 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_admin-dashboard-qa-stabilization_report.md` | QA 결과, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. 현재 브라우저를 `/dashboard`로 맞추고 화면 텍스트, 콘솔 로그, API 응답을 확인한다.
2. `DashboardPage.tsx`와 `dashboard.ts`를 읽어 화면 증상과 코드 원인을 매칭한다.
3. 고칠지 말지 판단한다.
   - 프론트 표시/상태 처리 문제이면 이 브랜치에서 수정한다.
   - 서버 계약/데이터 문제이면 report에 후속 서버 이슈로 남긴다.
4. 수정 전 현재 증상을 report 초안에 기록할 수 있게 메모한다.
5. `DashboardPage.tsx` 중심으로 최소 변경을 적용한다.
6. `npm.cmd run typecheck`를 실행한다.
7. 브라우저에서 `/dashboard`를 다시 확인하고, 콘솔에 새 오류가 없는지 본다.
8. `2026-06-12_admin-dashboard-qa-stabilization_report.md`를 작성한다.

## 테스트 보강 목록

| 테스트/검증 | 추가 검증 |
| --- | --- |
| `npm.cmd run typecheck` | Dashboard 타입, JSX, API 타입 오류 없음 |
| `npm.cmd run build` | UI 수정 범위가 커질 경우 production build 확인 |
| Browser `/dashboard` | 지표 카드, 오늘 QT, 최근 관리자 활동이 렌더링됨 |
| Browser console | 대시보드 코드가 유발하는 error/warn 없음 |
| Manual reload | 새로고침 버튼 클릭 후 로딩/데이터 복구 확인 |

## 수용 기준

- [ ] `/dashboard` 진입 시 지표 카드와 오늘 QT 정보가 표시된다.
- [ ] 데이터가 없을 때 빈 상태가 운영자가 이해 가능한 문구로 표시된다.
- [ ] API 실패 시 재시도 버튼과 오류 설명이 표시된다.
- [ ] 새로고침 버튼이 중복 클릭/로딩 상태를 자연스럽게 처리한다.
- [ ] 대시보드 코드가 유발하는 Ant Design 경고가 남지 않는다.
- [ ] 수정 내용과 검증 결과가 report에 남는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 사용자가 QA 중 발견한 문제를 즉시 대화로 판단하고 고치는 흐름이라, 화면 상태를 보는 작업자와 수정 작업자가 같아야 맥락 손실이 적다.
- 첫 단위는 대시보드 한 화면에 한정되어 병렬화 이점보다 충돌 위험이 크다.
- 다른 페이지로 확장할 때는 페이지 단위 workflow/report를 추가로 작성한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 작업자가 화면 확인, 원인 판단, 코드 수정, report 작성을 한 흐름으로 직접 수행한다.

## 검증 계획

```powershell
cd admin-web
npm.cmd run typecheck
npm.cmd run build
```

브라우저 검증:

- `http://localhost:5173/dashboard` 진입
- 지표 카드 3개 표시 확인
- 오늘 QT 상태 표시 확인
- 최근 관리자 활동 테이블 표시 확인
- 새로고침 버튼 클릭 후 데이터 유지 확인
- 콘솔 error/warn 확인

## 후속 작업으로 남길 항목

- 서버 응답 자체의 지표 의미가 불명확하면 DevC/Lead 계약 확인으로 분리한다.
- 대시보드 외 관리자 페이지 이슈는 별도 workflow/report로 쪼갠다.
- 한 브랜치에서 계속 작업하되 커밋은 페이지 또는 이슈 단위로 분리한다.
