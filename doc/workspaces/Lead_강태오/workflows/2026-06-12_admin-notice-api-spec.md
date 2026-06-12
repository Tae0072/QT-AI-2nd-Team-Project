# 2026-06-12 관리자 공지 API 명세 보강 워크플로우

## 작업 원칙
- 로그인/인증 진행 브랜치와 충돌하지 않는다.
- 코드 기능은 이미 PR #540에서 머지되었으므로 이번 작업은 API 명세 정합화로 제한한다.
- 기준 구현은 `origin/dev`의 `AdminNoticeController`, `NoticeService`, `AdminNoticeControllerTest`를 따른다.

## TODO
- [x] 1. `origin/dev` 최신화 및 별도 워크트리 생성
- [x] 2. PR #540 자동 리뷰 경고 사항 확인
- [x] 3. `04_API_명세서.md`의 AD-06 상단 기능 표 보강
- [x] 4. 공지 상세 조회 응답, 입력 검증, 상태 전이, 실패 코드 보강
- [x] 5. 전체 API 요약 표에 `GET /api/v1/admin/notices/{id}` 행 추가
- [x] 6. 변경 이력과 작업 리포트 작성
- [x] 7. 문서 diff 및 검증 완료
- [x] 8. PR용 변경 범위 정리

## 검증 계획
- `git diff --check` 통과
- 문서 전용 변경이므로 Java/TypeScript 빌드는 생략한다.

## 로그인 WIP 보호
- `admin-web/src/auth/**`, `admin-web/src/api/adminAuth.ts`, `admin-web/src/api/client.ts`, `LoginPage.tsx`, Vite 인증 프록시를 수정하지 않는다.
- 기존 로그인 작업 폴더 `C:\workspace\QT-AI-2nd-Team-Project`의 미추적 파일은 건드리지 않는다.
