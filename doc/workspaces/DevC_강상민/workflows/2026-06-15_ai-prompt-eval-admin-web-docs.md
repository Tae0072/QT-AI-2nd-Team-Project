# Workflow - 2026-06-15 ai-prompt-eval-admin-web-docs

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-prompt-eval-admin-web-docs` |
| PR 대상 | `feature/ai-prompt-eval-backend-openapi` 또는 backend PR merge 후 `dev` |
| 관련 F-ID | F-02, F-06, F-14 |
| 트리거 | Backend/OpenAPI PR에서 추가한 프롬프트 관리/평가 run API를 관리자 화면에 연결하기 위함 |
| 기준 문서 | `04_API_명세서.md`, `18_코드_품질_게이트.md`, backend OpenAPI PR |
| 해당 경로 | `admin-web/src/**`, admin-web 리포트/워크플로우 문서 |

## 작업 목표

관리자가 EXPLANATION 프롬프트를 DRAFT로 생성하고, AD-11 평가 세트 화면에서 후보 프롬프트를 실행한 뒤 결과를 확인할 수 있게 한다.

이 PR은 frontend와 문서만 다룬다. 서버 API와 OpenAPI 계약은 선행 backend PR에서 제공한다.

## 범위

- `AI 프롬프트 관리` 화면 추가.
- 프롬프트 목록/상세/생성/활성화/폐기 API client 추가.
- AD-11 평가 세트 화면에 평가 실행 버튼, 최근 실행 결과, 실행 상세 Drawer 추가.
- 메뉴와 라우트 추가.
- admin-web 전용 workflow/report 작성.

## 제외 범위

- backend API 구현.
- DB migration.
- OpenAPI 계약 변경.
- QA/SIMULATOR 화면 연결.
- 실제 운영 서버 E2E 검증.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `admin-web/src/api/aiPromptVersions.ts` | 프롬프트 관리 API client |
| Modify | `admin-web/src/api/aiEvaluations.ts` | 평가 run API client |
| Create | `admin-web/src/pages/AiPromptVersionsPage.tsx` | 프롬프트 관리 화면 |
| Modify | `admin-web/src/pages/AiEvaluationsPage.tsx` | AD-11 평가 실행/최근 결과/상세 연결 |
| Modify | `admin-web/src/constants/menu.ts` | 메뉴 추가 |
| Modify | `admin-web/src/App.tsx` | 라우트 추가 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-prompt-eval-admin-web-docs.md` | frontend PR workflow |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-15_ai-prompt-eval-admin-web-docs-report.md` | frontend PR report |

## 구현 순서

1. backend PR의 API 계약을 기준으로 API client를 추가한다.
2. 프롬프트 관리 화면을 추가한다.
3. 메뉴와 라우트를 연결한다.
4. AD-11 평가 세트 화면에 평가 실행 UI를 추가한다.
5. 최근 실행 결과와 상세 Drawer를 연결한다.
6. 타입체크, 계약 테스트, 빌드를 실행한다.

## 테스트 보강 목록

| 테스트/검증 | 검증 |
| --- | --- |
| `npm.cmd run typecheck` | TypeScript 타입 정합성 |
| `npm.cmd test` | admin page contract 유지 |
| `npm.cmd run build` | Vite production build |

## 수용 기준

- [ ] `/ai-prompt-versions` 화면이 REVIEWER 권한 메뉴에 노출된다.
- [ ] 프롬프트 DRAFT 생성/상세/활성화/폐기 API client가 연결된다.
- [ ] AD-11에서 평가 set별 평가 run 실행과 최근 결과 확인이 가능하다.
- [ ] 실행 결과 상세가 case별로 확인 가능하다.
- [ ] backend 없는 임시 mock이나 금지된 raw prompt 저장이 없다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- API client와 화면 구현은 backend 파일과 분리되어 병렬 검토 가능하다.
- 다만 최종 타입체크와 빌드는 메인 에이전트가 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | 프롬프트 관리 화면 구현 | `admin-web/src/pages/AiPromptVersionsPage.tsx` |
| Worker 2 | AD-11 평가 run UI 연결 | `admin-web/src/pages/AiEvaluationsPage.tsx` |
| Worker 3 | API client/라우팅 정리 | `admin-web/src/api/**`, `admin-web/src/App.tsx`, `admin-web/src/constants/menu.ts` |

### 직접 실행 판단

메인 에이전트가 API 계약 일치 여부와 최종 frontend 검증을 직접 확인한다.

## 검증 계획

- `npm.cmd run typecheck`
- `npm.cmd test`
- `npm.cmd run build`
- `git diff --check`

## 후속 작업으로 남길 항목

- backend PR이 merge된 뒤 dev 기준으로 frontend PR을 rebase하거나, stacked PR로 backend branch를 base로 둔다.
- 실제 서버 연결 E2E는 backend 배포/로컬 실행 환경에서 별도 확인한다.
