# 2026-06-09 · admin-web D2 권한 가드 리포트

## 요약

`dev-msa` 백엔드 분리 작업과 충돌하지 않도록 `dev-admin-web` worktree에서 관리자 웹 프론트만 수정했다. 기존 로그인 토큰 유무 기반 보호를 `GET /api/v1/admin/me` 기반 세부 관리자 권한 확인으로 확장했다.

## 변경 내용

| 영역 | 내용 |
|---|---|
| API | `admin-web/src/api/adminMe.ts` 추가, `/admin/me` 호출로 `adminRole` 조회 |
| 인증 상태 | `AuthContext`에 `adminInfo`, `adminLoading`, `refreshAdminInfo` 추가 |
| 보호 라우트 | `ProtectedRoute`가 관리자 정보 조회 완료 전 로딩을 표시하고 실패 시 로그인으로 회귀 |
| 역할 가드 | `RoleGuard` 추가, 직접 URL 접근 시 권한 부족 화면 표시 |
| 메뉴 | `MENU_ITEMS.requiredRoles` 기준으로 사이드바 노출 필터링 |
| 권한 규칙 | `SUPER_ADMIN` 우월권, `requiredRoles=[]`는 활성 관리자 공통 접근 |
| AD-05 | API 명세 기준으로 `OPERATOR` 권한에 맞춤 |

## 검증

- `npm.cmd run typecheck`: 통과
- `npm.cmd run build`: 통과
- Vite chunk size 경고는 기존 antd 번들 크기 경고로 비차단.

## 남은 일

- ADMIN 토큰으로 브라우저에서 메뉴 필터와 직접 URL 403 동작 확인.
- AD-05 찬양 큐레이션 목록·등록 로컬 QA.
- `admin-web/package-lock.json` 커밋 여부 결정.
- F2 환경/배포 정리.
