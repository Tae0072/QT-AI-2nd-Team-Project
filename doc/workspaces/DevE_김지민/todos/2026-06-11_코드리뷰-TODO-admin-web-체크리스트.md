# 코드리뷰 TODO admin-web 체크리스트 (2026-06-11) — 브랜치별

> **트랙**: `dev` / **담당**: 김지민(admin-web FE)
> **완료 기준(공통)**: 브랜치별 `npm run typecheck` + `npm run build` 통과 + dev 대상 PR (테스트 프레임워크 없음)
> **워크플로우**: [workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md](../workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md)
> **출처**: `2026-06-10_코드리뷰_TODO_김지민.md` (P1~P5)

---

## ✅ P1 `bugfix/admin-web-auth-proxy` — 머지 완료 (#482)

- [x] vite proxy `/api/v1/admin/auth`→8081 분리 + `.env.example` + LoginPage 주석
- [x] typecheck + build 통과, push → PR → **자동 머지(#482)**

---

## ✅ P3a `feature/admin-web-dashboard-dto` — 머지 완료 (#490, 렌더 A)

- [ ] `src/api/dashboard.ts`: `[key:string]:unknown` → 실타입(`DashboardSummary`/`TodayQt`/`TodayQtStatus`/`RecentAuditLog`, 백엔드 DTO 1:1)
- [ ] `DashboardPage.tsx` 카운트 3개 = `Statistic` 카드(AI 검증 대기·신고 접수·신고 검토)
- [ ] Today QT = `Descriptions`(날짜·제목·상태 Tag·시뮬상태·해설유무·캐시), `MISSING` 처리
- [ ] 최근 감사로그 = `Table`(시각·작업유형·대상·작업자) + 빈 상태
- [ ] "백엔드 준비 중" Alert 철거 → 에러 Alert + 재시도 버튼
- [ ] `npm run typecheck` + `npm run build` 통과
- [ ] 워크플로우·리포트 갱신 → 커밋 → push → PR(base dev)

## ✅ P3b `feature/admin-web-qt-passages-dto` — AD-02 풀 CRUD 머지 완료 (#496)

- [x] `qtPassages.ts` 실타입(`QtPassage`+`QtPassageStatus` 5종)+필터 파라미터+`QtPassageRequest`+create/update API
- [x] `QtPassagesPage.tsx` `usePagedList` 목록 + 명시 컬럼 + 상태 Tag(한글 라벨)
- [x] 필터(상태/기간/검색) + 에러 Alert+재시도 + "준비 중" 철거 + 페이징
- [x] 행 액션(상태별 수정/게시/숨김, Popconfirm) — 계약서 §7 버튼 정책
- [x] 등록/수정 Modal 폼(qtDate·bookId·chapter·startVerse~endVerse·title·mainVerseRef) + 검증(필수·날짜형식·startVerse≤endVerse)
- [x] antd 함정 보정: Modal `forceRender`(수정 시 빈 폼 방지), validateFields 검증/저장 분리
- [x] `npm run typecheck` + `npm run build` 통과
- [ ] 워크플로우·리포트 갱신 → 커밋 → push → PR(base dev)

## ⏳ P3c `feature/admin-web-notices-dto` — AD-06 풀 CRUD (진행 중, 계획 확정 B)

- [ ] `notices.ts` 실타입(`Notice`·`NoticeDetail`·`NoticeStatus` 3종·`NoticeRequest`·`PublishResult`) + create(POST)/update(PATCH)/hide API (#450 계약)
- [ ] `NoticesPage.tsx` `usePagedList` 목록 + 명시 컬럼(제목·미리보기·상태 Tag·발행시각) + 페이징
- [ ] 등록/수정 Modal 폼(제목 Input + 본문 TextArea, **plain text 검증** `<`·`>` 금지)
- [ ] 행 액션(상태별 수정/발행/숨김, Popconfirm) — DRAFT→수정·발행, PUBLISHED→숨김, HIDDEN→없음
- [ ] 발행 성공 시 **알림 결과**(생성/실패 수) 메시지 + 에러 Alert+재시도 + "준비 중" 철거
- [ ] antd 함정 보정(Modal forceRender, validateFields 분리) — P3b 패턴 재사용
- [ ] `npm run typecheck` + `npm run build` 통과
- [ ] 워크플로우·리포트 갱신 → 커밋 → push → PR(base dev)

## ⏳ P5c `chore/admin-web-code-split` (예정)

- [ ] `vite.config.ts` `build.rollupOptions.output.manualChunks`(antd/vendor) → 청크 분리 확인(1.15MB 단일청크 해소)

## ⏳ P5b `feature/admin-web-kakao-sdk-notice` (예정)

- [ ] LoginPage에서 `kakao.ts` 키 미설정 에러를 AntD `Alert`로 노출(VITE_KAKAO_JS_KEY 안내)

## ⏳ P4 `docs/admin-web-token-storage-review` (예정, 강태오 확인)

- [ ] `tokenStorage.ts` XSS 위험 인지 주석 보강 + 운영 전 HttpOnly 전환 계획 기록(시점 강태오 결정)

---

## ⏳ P2 `feature/admin-web-token-refresh` — 진행 중 (리다이렉트 방식 B 확정)

> 공용 `POST /api/v1/auth/refresh` 재사용(계약서 §6). 재발급 role=ADMIN 유지. 리다이렉트=B(AuthContext 콜백, 새로고침 없음).

- [ ] `tokenStorage`: `getRefreshToken`/`setRefreshToken` 추가 + `clearTokens`(access+refresh 동시)
- [ ] `client.ts`: 401 → `/auth/refresh`(**single-flight**, `_retry` 플래그) → 새 access로 원요청 재시도. `/auth/refresh`·`/admin/auth/kakao` 제외
- [ ] `client.ts`: `setAuthExpiredHandler(fn)` export — refresh 실패/`M0006`/`M0007` 시 `clearTokens` + 콜백 호출
- [ ] `AuthContext`: `login(access, refresh?)` 확장(둘 다 저장) + `useEffect`로 `setAuthExpiredHandler(clearSession)` 등록
- [ ] `LoginPage`: 카카오 로그인 시 `login(res.accessToken, res.refreshToken)` (dev 토큰은 access만)
- [ ] `npm run typecheck` + `npm run build` 통과 → 워크플로우·리포트 → 커밋 → push → PR(base dev)
- [ ] 검증: access 만료 시나리오에서 화면 유지 / refresh 실패 시 로그인 이동

## ⛔ 보류 (의존 해제 시)

- [ ] **P5a 찬양 숨김** — admin-server `AdminPraiseController`(PATCH/hide) 백엔드 구현 후 `hidePraiseSong` 행 액션 연결(AD-03 Popconfirm 패턴)
