# 2026-06-09 · admin-web 작업 문서 소유권 학습 노트

## 작업 문서 경로

작업 기록은 실제 작업자의 workspace 아래에 남겨야 한다.
이번 admin-web 후속 작업은 강태오 작업이므로 `doc/workspaces/Lead_강태오/` 아래에 workflow, report, study note, todo를 둔다.

## 기존 팀원 문서는 건드리지 않는다

`DevE_김지민`에는 김지민의 일정, Flutter/admin 화면 작업, 학습 문서가 이미 많다.
이번 정정은 Codex가 잘못 작성한 2026-06-09 admin-web 문서만 이동하고, 김지민 고유 문서는 수정하지 않는다.

## 권한 문서화 원칙

권한 표는 프런트 메뉴 정의만 보고 적지 않는다.
백엔드 인증 헬퍼와 도메인 서비스의 재검증까지 함께 확인해야 한다.

- 프런트: `admin-web/src/constants/menu.ts`, `admin-web/src/constants/roles.ts`
- 백엔드: `AdminAiAuthentication`, `AdminAuditAuthentication`, `VerifyAdminRoleUseCase`

`SUPER_ADMIN` 우월권은 코드에 구현되어 있더라도 문서에서 암묵 처리만 하지 않고 표에 명시하면 리뷰 오해가 줄어든다.
