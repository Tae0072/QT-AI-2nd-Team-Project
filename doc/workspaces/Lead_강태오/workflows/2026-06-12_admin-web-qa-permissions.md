# 2026-06-12 관리자 웹 QA·권한표 정합화 워크플로우

## 작업 원칙
- 로그인 WIP가 남아 있는 기본 작업 폴더는 건드리지 않는다.
- `origin/dev` 기준 별도 워크트리에서 진행한다.
- 로그인 화면, 인증 API, 토큰 저장 로직은 수정하지 않는다.
- 관리자 웹 메뉴/라우트 권한이 백엔드 실제 인가보다 넓지 않게 맞춘다.

## TODO
- [x] 1. `origin/dev` 최신화 및 별도 워크트리 생성
- [x] 2. `04_API_명세서.md` AD-01~AD-10 권한 확인
- [x] 3. 백엔드 Controller/Authentication/Service 실제 인가 확인
- [x] 4. `admin-web/src/constants/menu.ts` 권한표 정합화
- [x] 5. `admin-web/src/constants/roles.ts` 역할 설명 정리
- [x] 6. `admin-web/README.md` 화면·권한 현황 갱신
- [x] 7. `npm run typecheck` 검증
- [x] 8. `npm run build` 검증
- [x] 9. 최종 diff 검토 후 PR 준비

## 확인한 핵심 사항
- AD-01 대시보드는 백엔드 `AdminDashboardService`에서 `OPERATOR`, `REVIEWER`를 요구한다.
- 프런트 메뉴는 AD-01을 빈 배열로 두고 있어 `CONTENT_CREATOR`도 접근 가능하게 보일 수 있었다.
- `SUPER_ADMIN`은 `canAccessAdminRoute`에서 우월권으로 처리하므로 각 메뉴 항목에 반복 표기하지 않는다.

## 로그인 WIP 보호
- `C:\workspace\QT-AI-2nd-Team-Project`의 기존 미추적 파일은 수정하지 않는다.
- 이번 작업 폴더는 `C:\workspace\QT-AI-admin-web-qa-permissions`이다.
