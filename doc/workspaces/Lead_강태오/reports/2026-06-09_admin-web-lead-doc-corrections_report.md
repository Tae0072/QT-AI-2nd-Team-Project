# 2026-06-09 · admin-web Lead 작업 문서 경로 정정 리포트

## 요약

PR #418/#419/#421에서 강태오 작업 기록을 `DevE_김지민` 아래에 작성한 오류를 정정했다.
해당 2026-06-09 admin-web 작업 문서 12개를 `Lead_강태오` 아래로 이동하고, PR #421 종합 의견의 권한표 정합성 지적을 함께 반영했다.

## 변경 내용

| 구분 | 내용 |
|---|---|
| 문서 경로 | role-guard, AD-05 QA hardening, env/deploy 문서 12개를 `Lead_강태오`로 이동 |
| README | AD-03/AD-07/AD-08 권한 표에 SUPER_ADMIN을 명시하고 백엔드 가드 확인 기준 추가 |
| 화면 문구 | AD-08 화면 권한 안내를 OPERATOR/REVIEWER/SUPER_ADMIN으로 정정 |
| 코드 주석 | AD-07, AD-08 권한 주석을 실제 백엔드 가드 기준으로 정정 |

## PR #421 종합 의견 반영

- `.env.example`의 dev-bypass 변수 제거 판단은 유지
- 임시 ADMIN 토큰 입력 방식은 운영 전 공식 로그인으로 대체해야 한다는 문구 추가
- AD-03/AD-07/AD-08 권한은 프론트 메뉴 정의와 백엔드 `AdminAiAuthentication`, `AdminAuditAuthentication`, `VerifyAdminRoleUseCase` 기준으로 재확인
- PR 본문에 workflow/report 링크를 명시할 예정

## 남은 일

- 실제 ADMIN 토큰으로 AD-05 목록/필터/등록 브라우저 QA
- Vite/esbuild dev dependency moderate 2건은 Vite 8 업그레이드 영향 검토 후 별도 처리

## 검증 결과

- `npm.cmd run typecheck`: 통과
- `npm.cmd run build`: 통과. Vite chunk size 경고는 기존 antd 번들 경고로 비차단
- `npm.cmd audit --omit=dev`: 운영 의존성 취약점 0건
- `git diff --check`: 통과. Windows CRLF 변환 경고만 표시
