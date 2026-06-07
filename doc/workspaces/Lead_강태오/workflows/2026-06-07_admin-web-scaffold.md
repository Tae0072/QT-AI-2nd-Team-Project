# 2026-06-07 · 관리자 웹(admin-web) 전체 구조 골격 — 워크플로우

작성: Claude (Lead 강태오/T 지시) · 대상 저장소: `QT-AI-2nd-Team-Project` (구현)

## 1. 요약 (한 줄)

사용자 Flutter 앱과 분리된 **관리자 전용 웹 프런트엔드**(`admin-web/`)의 전체 구조(골격)를 만든다.
React + Vite + TypeScript + Ant Design 기반으로, `qtai-server`의 `/api/v1/admin/**`를 호출하는 API 계층·인증·라우팅·공통 레이아웃과 AD-01~08 화면 골격을 둔다.

## 2. 배경 / 근거 문서

- `03_아키텍처_정의서.md` v1.3 §4.9 / §13.6: 관리자 UI는 **Flutter 앱이 아닌 별도 웹**. 같은 `qtai-server`의 `/api/v1/admin/**` 호출. SSR/SPA 채택은 "v1.2 이후 결정"으로 **미확정**.
- `22_구현_저장소_반영_체크리스트.md` v0.3 §3: 관리자 웹 코드 위치는 `admin-web/`. `flutter-app/` PR에 관리자 화면 포함 금지.
- `04_API_명세서.md` §AD-01~08: 관리자 API 목록 및 권한.
- `CLAUDE.md` §5(권한), §6(QT 시각), §7(AI), §8(금지).

## 3. 결정 사항 (Lead T 승인)

문서상 미확정이던 항목을 이번 작업에서 확정한다.

| 항목 | 결정 | 비고 |
|---|---|---|
| 기술 스택 | React + Vite + TypeScript | 표준적·학습자료 풍부(입문자 기준) |
| UI 라이브러리 | Ant Design (antd) | 관리자 표/폼/모달 컴포넌트 풍부 |
| 작업 범위 | 골격 + 라우팅 + 레이아웃 + AD-01~08 빈 화면 | "전체 구조 먼저" |
| 로그인 | 임시 토큰 입력 방식 | 카카오 웹 로그인/서버 OAuth 연동 전까지 |

> 기술 스택 확정은 `03_아키텍처_정의서.md`의 "v1.2 이후 결정" 항목을 채우는 것이므로, 문서 저장소(`2nd-Team-Project`)에도 반영 필요 → **문서 PR 별도 진행 권장**.

## 4. 만든 구조

```
admin-web/
├─ index.html, vite.config.ts, tsconfig.json, package.json, .env.example, .gitignore
└─ src/
   ├─ main.tsx (Provider/Router 부트스트랩), App.tsx (라우팅)
   ├─ config/env.ts
   ├─ api/        client.ts(axios+Bearer+envelope 해제), types.ts, dashboard/qtPassages/aiAssets/reports/praiseSongs/notices/auditLogs/aiMonitoring.ts
   ├─ auth/       AuthContext.tsx, useAuth.ts, tokenStorage.ts
   ├─ routes/     ProtectedRoute.tsx
   ├─ components/ PagePlaceholder.tsx, layout/AdminLayout.tsx
   ├─ constants/  menu.ts, roles.ts
   └─ pages/      Login, Dashboard, QtPassages, AiAssets, Reports, PraiseSongs, Notices, AuditLogs, AiMonitoring, NotFound
```

### 4.1 API 계층

- 공통 응답 봉투(`success/data/error/timestamp/traceId`)와 페이징(`content/page/size/...`)을 타입으로 정의(`04 §1.4~1.6` 기준).
- `client.ts`: 요청 시 `Authorization: Bearer {token}` 자동 첨부, 401이면 토큰 정리, `unwrap()`으로 `data`만 추출.
- 화면별 API 함수에 엔드포인트·권한·관련 규칙(QT 00:00/04:00, AI 원문 미노출, 음원 미저장)을 주석으로 남김.

### 4.2 인증·권한

- 임시 토큰 입력 → `localStorage` 저장 → 모든 요청 자동 첨부.
- `ProtectedRoute`로 미로그인 시 `/login` 리다이렉트.
- `roles.ts`에 `OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN` 상수. `menu.ts`에 화면별 `requiredRoles`(일부 추정, TODO 표기).

### 4.3 레이아웃·라우팅

- `AdminLayout`: 좌측 사이드바(메뉴) + 상단 헤더(로그아웃) + 콘텐츠(Outlet).
- `App.tsx`: `/login` 공개, 나머지는 `ProtectedRoute` + `AdminLayout` 하위 보호, `/`→`/dashboard`, `*`→404.

## 5. 검증

- `npm install`(161 packages) → `tsc --noEmit`(통과) → `vite build`(성공, 1486 modules, `dist` 생성).
- 검증은 사용자 폴더를 오염시키지 않도록 sandbox 임시 경로에서 수행(Windows용 `node_modules`와 분리).
- 소스 전체 null 바이트 오염 검사 → 없음.

## 6. 작업 중 발견·해결한 이슈

1. **파일 덮어쓰기 시 null 바이트 잔존**: 더 긴 파일을 짧게 덮어쓰면 끝에 `\x00`가 남아 JSON 파싱이 깨짐(`tsconfig.json`). → bash로 truncate 재작성하여 해결. 이후 덮어쓰기는 bash 기반으로 처리.
2. **`tsconfig.node.json` emit 충돌**: references + `noEmit` 충돌(TS6310). → references 제거하고 단일 `tsconfig.json`(`include: ["src","vite.config.ts"]`)으로 단순화, 미사용 `tsconfig.node.json` 삭제.
3. **git 비정상 상태**: 현재 체크아웃 브랜치 `feature/music-background-play`가 커밋 없는(unborn) 상태이며 워킹트리에 프로젝트 전체가 staged됨. → 다른 작업 훼손 위험으로 **자동 커밋 보류**, 안전 절차를 리포트에 안내.

## 7. Git / PR 안내 (보류 — 사용자 확인 필요)

- 규칙: `dev`에서 작업 브랜치 생성, 브랜치명 `{type}/{scope}-{desc}`, PR 대상 `dev`.
- 권장 브랜치: `feat/admin-web-scaffold`.
- 단, 현 워킹트리 git 상태가 꼬여 있어 브랜치 전환/커밋 전에 현재 staged 내용(배경음악 등) 처리 방향을 T가 확인해야 함.
- 신규 프로젝트 스캐폴드라 파일 수(38개)가 PR 권장치(10 files)를 초과하나, 단일 스캐폴드 성격상 불가피.

## 8. 다음 단계

1. 화면별 실제 구현(목록 표·필터·페이지네이션, 등록/수정 폼, 승인·반려 모달).
2. 세부 권한별 메뉴 노출·접근 제어.
3. 카카오 웹 로그인/서버 OAuth 연동.
4. `03_아키텍처_정의서.md` 기술 스택 확정 내용 문서 반영(문서 저장소 PR).
