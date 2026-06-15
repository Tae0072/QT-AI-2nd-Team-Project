# AI 프롬프트 평가 게이트 Admin Web/Docs 리포트

| 항목 | 내용 |
| --- | --- |
| 작성일 | 2026-06-15 |
| 브랜치 | `feature/ai-prompt-eval-admin-web-docs` |
| PR 대상 | `feature/ai-prompt-eval-backend-openapi` 또는 backend PR merge 후 `dev` |
| 범위 | admin-web, frontend docs |

## 요약

Backend/OpenAPI PR에서 추가한 EXPLANATION 프롬프트 관리 API와 평가 run API를 관리자 웹에 연결했다. 관리자는 새 프롬프트를 만들고, AD-11 평가 세트에서 해당 프롬프트를 실행한 뒤 최근 실행 결과와 상세 결과를 확인할 수 있다.

이 리포트는 admin-web과 문서 PR 전용이다. 서버 API, DB migration, OpenAPI 변경은 선행 backend PR에서 다룬다.

## 구현 내용

- `AI 프롬프트 관리` 화면 추가.
- 프롬프트 목록/상세/생성/활성화/폐기 API client 추가.
- AD-11 평가 세트 화면에 평가 실행 버튼 추가.
- 최근 평가 run 결과와 case별 결과 상세 Drawer 추가.
- 메뉴와 라우트 추가.
- frontend 전용 workflow/report 작성.

## 주요 파일

- `admin-web/src/api/aiPromptVersions.ts`
- `admin-web/src/api/aiEvaluations.ts`
- `admin-web/src/pages/AiPromptVersionsPage.tsx`
- `admin-web/src/pages/AiEvaluationsPage.tsx`
- `admin-web/src/constants/menu.ts`
- `admin-web/src/App.tsx`
- `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-prompt-eval-admin-web-docs.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-15_ai-prompt-eval-admin-web-docs-report.md`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `npm.cmd run typecheck` | 통과 |
| `npm.cmd test` | 통과 |
| `npm.cmd run build` | 통과 |
| `git diff --check` | 통과. Windows 줄끝 경고만 존재 |

참고:

- Vite build에서 기존 antd chunk size 경고가 발생한다.
- 실제 서버 연동 E2E는 backend API 실행 환경에서 별도 확인이 필요하다.

## 리뷰 포인트

- frontend PR은 backend PR의 API 계약에 의존한다.
- stacked PR로 올릴 경우 base를 `feature/ai-prompt-eval-backend-openapi`로 둔다.
- backend PR이 먼저 merge되면 frontend PR은 `dev`로 rebase 후 base를 `dev`로 열 수 있다.
