# 2026-06-09 · admin-web Lead 작업 문서 경로 정정 워크플로우

## 목적

PR #421 머지 후 확인된 작업자 경로 오류를 바로잡는다.
강태오 작업으로 진행한 #418/#419/#421 관련 workflow/report/study note/todo가 `DevE_김지민` 아래에 작성되어 있어 `Lead_강태오` 아래로 이동한다.

## 작업 순서

1. `dev-admin-web` 최신 머지 상태로 동기화
2. PR #421 종합 의견 확인
3. #418/#419/#421 커밋 파일 목록 확인
4. 잘못 작성된 2026-06-09 admin-web 작업 문서 12개를 `Lead_강태오`로 이동
5. PR #421 리뷰의 권한표 정합성 의견 반영
6. 타입 검사, 빌드, 운영 의존성 audit, diff check 실행

## 범위

- 포함: 작업 문서 경로 정정, README 권한표 보강, AD-08/AD-07 권한 안내 문구 정정
- 제외: 백엔드 권한 로직 변경, 관리자 API 계약 변경, 실제 ADMIN 토큰 브라우저 QA

## 확인 기준

- AD-03: `AdminAiAuthentication.requireReviewer` 기준 REVIEWER, SUPER_ADMIN
- AD-07: `AdminAuditAuthentication.requireAudit` 기준 OPERATOR, REVIEWER, SUPER_ADMIN
- AD-08: `AdminAiAuthentication.requireMonitoring` 기준 OPERATOR, REVIEWER, SUPER_ADMIN
