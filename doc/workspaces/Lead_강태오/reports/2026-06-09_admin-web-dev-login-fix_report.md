# 리포트 — admin-web dev 로그인 401 수정

- 작성자: 강태오 (Lead)
- 일자: 2026-06-09
- PR 대상: `dev`
- 브랜치: `bugfix/admin-web-dev-login-401`
- 변경 파일: `admin-web/src/api/client.ts` (+ 본 workflow/report 문서)

## 요약

dev 환경에서 admin-web이 가짜 Bearer 토큰을 보내 `JwtAuthenticationFilter`가 401을 내던
문제를 해소했다. dev 인증은 `X-Dev-*` 헤더만으로 처리하도록 정리했다.

## 변경 내용

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| dev에서 Authorization 헤더 | `Bearer dev-bypass`(가짜) 전송 | dev에서는 미전송 |
| dev 헤더 설정 방식 | 대괄호 대입 | `headers.set(...)` |
| 운영(prod) 경로 | Bearer 전송 | 변경 없음 |

## 검증 결과

- 로컬 dev: AD-07/04/03/08 화면 401 해소, 정상 데이터 응답.
- `typecheck` / `build` 통과.
- Gitleaks: 평문 시크릿 없음(가짜 토큰 문자열 제거).

## 기준 문서

- `CLAUDE.md` §5(API 규칙), 게이트웨이 단일 baseURL 계약.
- workflow: `doc/workspaces/Lead_강태오/workflows/2026-06-09_admin-web-dev-login-fix.md`

## 리스크 / 후속

- dev 전용 분기이므로 운영 영향 없음.
- 정식 로그인 연동 시 dev 헤더 경로는 dev 빌드에서만 활성 유지.
