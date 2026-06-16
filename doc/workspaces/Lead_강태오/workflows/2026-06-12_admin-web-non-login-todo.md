# 2026-06-12 관리자 웹 비로그인 보강 TODO

## 작업 원칙
- 로그인/인증 전환 작업과 충돌하지 않는다.
- `admin-web` 로그인 화면, 인증 API, 토큰 저장, Vite 인증 프록시 파일은 수정하지 않는다.
- 관리자 웹 AD-01~AD-10 중 실제 API 정합성과 사용성을 높이는 작은 작업부터 진행한다.

## TODO
- [x] 1. 현재 로컬 변경사항과 로그인 WIP 범위 확인
- [x] 2. AD-06 공지 수정 문제 원인 확인
- [x] 3. admin-server 공지 상세 조회 API 추가
- [x] 4. admin-web 공지 수정 모달이 상세 본문을 불러오도록 연결
- [x] 5. 변경 범위 검증 실행
- [x] 6. 최종 변경 파일과 남은 리스크 정리

## 검증 결과
- `npm run typecheck` 통과
- `npm run build` 통과
  - `antd` chunk 500KB 초과 경고는 기존 번들 크기 경고이며 빌드 실패는 아님
- `.\gradlew.bat :admin-server:compileJava --rerun-tasks` 통과
- `.\gradlew.bat :admin-server:test --tests com.qtai.domain.notification.web.AdminNoticeControllerTest --no-daemon` 통과
- `git diff --check` 통과

## 로그인 WIP 보호
- 로그인 화면, 인증 API, 토큰 저장, Vite 인증 프록시 작업은 수정 대상에서 제외했다.
- 기존 로컬 로그인 WIP 변경사항은 그대로 둔다.

## 변경 파일
- `qtai-server/admin-server/src/main/java/com/qtai/domain/notification/api/GetAdminNoticeUseCase.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/notification/internal/NoticeService.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/notification/web/AdminNoticeController.java`
- `qtai-server/admin-server/src/test/java/com/qtai/domain/notification/web/AdminNoticeControllerTest.java`
- `admin-web/src/api/notices.ts`
- `admin-web/src/pages/NoticesPage.tsx`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/workspaces/Lead_강태오/workflows/2026-06-12_admin-web-non-login-todo.md`

## 남은 리스크
- 실제 브라우저 클릭 QA는 로그인 작업 완료 후 관리자 계정으로 확인해야 한다.
- 운영 전 토큰 저장 방식(HttpOnly 쿠키 전환 여부)은 별도 로그인/보안 작업에서 결정한다.
