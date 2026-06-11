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

## ⏳ P3a `feat/admin-web-dashboard-dto` — 진행 중 (렌더 A)

- [ ] `src/api/dashboard.ts`: `[key:string]:unknown` → 실타입(`DashboardSummary`/`TodayQt`/`TodayQtStatus`/`RecentAuditLog`, 백엔드 DTO 1:1)
- [ ] `DashboardPage.tsx` 카운트 3개 = `Statistic` 카드(AI 검증 대기·신고 접수·신고 검토)
- [ ] Today QT = `Descriptions`(날짜·제목·상태 Tag·시뮬상태·해설유무·캐시), `MISSING` 처리
- [ ] 최근 감사로그 = `Table`(시각·작업유형·대상·작업자) + 빈 상태
- [ ] "백엔드 준비 중" Alert 철거 → 에러 Alert + 재시도 버튼
- [ ] `npm run typecheck` + `npm run build` 통과
- [ ] 워크플로우·리포트 갱신 → 커밋 → push → PR(base dev)

## ⏳ P3b `feat/admin-web-qt-passages-dto` — AD-02 (예정)

- [ ] `src/api/qtPassages.ts` 실타입(`AdminQtPassageResponse`/`AdminQtPassageListResponse`, #454 계약서 기준)
- [ ] `QtPassagesPage.tsx` 실데이터 테이블 + 빈/에러 + "준비 중" 철거 + 페이징(`usePagedList` 재사용)

## ⏳ P3c `feat/admin-web-notices-dto` — AD-06 (예정)

- [ ] `src/api/notices.ts` 실타입(`AdminNoticeListResponse`/`DetailResponse`/`PublishResponse`, #450)
- [ ] `NoticesPage.tsx` 실데이터 + 빈/에러 + "준비 중" 철거

## ⏳ P5c `chore/admin-web-code-split` (예정)

- [ ] `vite.config.ts` `build.rollupOptions.output.manualChunks`(antd/vendor) → 청크 분리 확인(1.15MB 단일청크 해소)

## ⏳ P5b `feat/admin-web-kakao-sdk-notice` (예정)

- [ ] LoginPage에서 `kakao.ts` 키 미설정 에러를 AntD `Alert`로 노출(VITE_KAKAO_JS_KEY 안내)

## ⏳ P4 `docs/admin-web-token-storage-review` (예정, 강태오 확인)

- [ ] `tokenStorage.ts` XSS 위험 인지 주석 보강 + 운영 전 HttpOnly 전환 계획 기록(시점 강태오 결정)

---

## ⛔ 보류 (의존 해제 시)

- [ ] **P2 refresh** — 관리자 토큰 갱신을 기본 `/api/v1/auth/refresh` 재사용 vs 전용 경로: 이승욱 계약 결정 후 `client.ts` single-flight + `tokenStorage` refresh 저장
- [ ] **P5a 찬양 숨김** — admin-server `AdminPraiseController`(PATCH/hide) 백엔드 구현 후 `hidePraiseSong` 행 액션 연결(AD-03 Popconfirm 패턴)
