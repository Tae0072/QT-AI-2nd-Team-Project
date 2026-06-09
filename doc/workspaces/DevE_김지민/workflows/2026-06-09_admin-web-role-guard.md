# 2026-06-09 · admin-web D2 권한 가드 워크플로우

## 목적

`dev-msa` 백엔드 분리 작업과 충돌하지 않도록 `admin-web/` 안에서만 관리자 웹 D2 권한 가드를 구현한다. 로그인 토큰만 보던 현재 구조를 `GET /api/v1/admin/me` 기반 세부 관리자 권한 확인으로 확장한다.

## 추천 순서

1. **D2 권한 가드**: 관리자 본인 정보 조회, 메뉴 노출, 라우트 접근 제한.
2. **AD-05 로컬 QA**: 찬양 큐레이션 목록·등록 동작 확인.
3. **F2 환경/배포 정리**: `.env.example`, Vite proxy, 실행 문서 보강.

## 이번 작업 범위

- `admin-web/src/api/adminMe.ts` 추가: `GET /admin/me` 호출.
- `AuthContext`에 `adminInfo`, `adminLoading`, `refreshAdminInfo` 추가.
- `ProtectedRoute`에서 토큰뿐 아니라 관리자 정보 조회 완료를 확인.
- 메뉴는 `MENU_ITEMS.requiredRoles` 기준으로 노출.
- 라우트는 화면별 필요 권한 기준으로 차단.

## 제외

- `qtai-server/**` 백엔드 수정.
- AD-01, AD-02, AD-06 본격 구현.
- AD-05 수정·숨김 연결.
- 카카오 웹 로그인 정식화.

## 검증

- `npm.cmd run typecheck`
- `npm.cmd run build`
