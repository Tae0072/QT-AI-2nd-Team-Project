# 리포트 - 관리자 웹 AD-06 공지 상세 본문 수정 UX 보강 (2026-06-12)

## 요약
관리자 웹 AD-06 시스템 공지 화면에서 공지 수정 시 목록의 `bodyPreview`만 있어 본문 전체를 다시 입력해야 하던 문제를 해결했다. admin-server에 공지 상세 조회 API를 추가하고, admin-web 수정 모달이 해당 API로 전체 본문을 불러오도록 연결했다.

## 변경 내용
- `GET /api/v1/admin/notices/{id}` 상세 조회 API 추가
- `GetAdminNoticeUseCase` 추가 및 `NoticeService` 구현 연결
- `AdminNoticeControllerTest`에 상세 조회 200, 권한 부족 403, 미존재 404 케이스 추가
- admin-web `getNotice(id)` API 함수 추가
- 공지 수정 버튼 클릭 시 상세 조회 후 `title`, `body`를 모달에 자동 입력
- API 명세서 공지 관리 섹션에 상세 조회 경로 반영

## 자동 리뷰 대응
- Claude 자동 리뷰가 요청한 `GET /api/v1/admin/notices/{id}` 부정 경로 테스트를 추가했다.
- 추가 케이스: 세부 관리자 권한 부족 `403 AD0003`, 미존재 공지 `404 C0004`.

## 로그인 WIP 보호
- 로그인 화면, 인증 API, 토큰 저장, Vite 인증 프록시 파일은 수정하지 않았다.
- 현재 별도 진행 중인 관리자 아이디 로그인 작업과 파일 범위를 분리했다.

## 검증
- `npm run typecheck` 통과
- `npm run build` 통과
- `.\gradlew.bat :admin-server:compileJava --rerun-tasks` 통과
- `.\gradlew.bat :admin-server:test --tests com.qtai.domain.notification.web.AdminNoticeControllerTest --no-daemon` 통과
- `git diff --check` 통과

## 남은 리스크
- 실제 브라우저 클릭 QA는 로그인 작업 완료 후 관리자 계정으로 확인해야 한다.
- 운영 전 토큰 저장 방식(HttpOnly 쿠키 전환 여부)은 별도 로그인/보안 작업에서 결정한다.
