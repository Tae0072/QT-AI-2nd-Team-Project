# 코드리뷰 TODO 수정 워크플로우 — admin-web (2026-06-11)


| 항목     | 내용                                                                                              |
| ------ | ----------------------------------------------------------------------------------------------- |
| 담당자    | 김지민 (admin-web FE)                                                                              |
| 트랙     | `dev` (모든 브랜치 base = `origin/dev`, push 후 dev 대상 PR)                                            |
| PR 대상  | `dev`                                                                                           |
| 출처     | `doc/workspaces/DevE_김지민/2026-06-10_코드리뷰_TODO_김지민.md` (P1~P5)                                   |
| 기준 문서  | `04_API_명세서.md`, 백엔드 계약서(아래 의존성 표)                                                              |
| 담당 경로  | `admin-web/` (React + TS + Vite + AntD)                                                         |
| 검증 게이트 | `npm run typecheck`(strict) + `npm run build` (테스트 프레임워크 없음)                                    |
| 상태     | P1(#482)·P3a(#490)·P3b(#496)·P3c(#501)·P2(#504) 머지 · P5b PR대기 · **P5c 재적용 필요**(#508→#510 롤백 revert) · P4/P5a 대기. 상세: `2026-06-11_admin-web-진행상황-핸드오프.md` |


---

## 작업 목표

코드리뷰 후속 TODO 5항목(P1~P5)을 작은 PR로 순차 수정. **dev 최신화로 P3(AD-01/02/06)이 새로 풀림** — 백엔드 계약 머지 완료로 FE 타입 교체 즉시 가능.

---

## 의존성 재확인 (2026-06-11, 현재 dev 기준)


| 항목             | 상태               | 근거                                                                                                                                            |
| -------------- | ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| P1 로그인 라우팅     | ✅ 가능 (조율 1건)     | vite `/api`→8090 단일룰. 관리자 로그인은 service-user(8081, #452)에만, admin-server엔 auth 컨트롤러 없음                                                         |
| P3 AD-01 대시보드  | ✅ 풀림             | `AdminDashboardResponse`(#449) + 계약서                                                                                                          |
| P3 AD-02 QT관리  | ✅ 풀림             | `AdminQtPassageResponse`(#454) + 김지민 확인 계약서                                                                                                   |
| P3 AD-06 공지    | ✅ 풀림             | `AdminNoticeListResponse`/`Detail`/`Publish`(#450)                                                                                            |
| P4 토큰 보관       | ✅ 가능 (주석/결정)     | localStorage `qtai_admin_token`, HttpOnly 전환 시점 강태오 결정                                                                                        |
| P5b SDK 안내     | ✅ 가능             | `kakao.ts` 키 미설정 throw → LoginPage Alert                                                                                                      |
| P5c code-split | ✅ 가능             | vite manualChunks 없음(1.15MB 단일청크, 빌드 경고 확인)                                                                                                   |
| P2 refresh     | ✅ 풀림(2026-06-11) | 공용 `POST /api/v1/auth/refresh` 재사용 합의 — 재발급 role=ADMIN 유지(서버 테스트 보증). 근거: `DevD_이승욱/contracts/2026-06-10_admin-kakao-auth-api-contract.md` §6 |
| P5a 찬양 숨김      | ❌ 막힘             | admin-server praise web 컨트롤러 없음(PATCH/hide 미구현)                                                                                               |


---

## 브랜치 로드맵 (순차, base=origin/dev)


| 순서  | 브랜치                                   | 항목         | 상태                            |
| --- | ------------------------------------- | ---------- | ----------------------------- |
| 1   | `bugfix/admin-web-auth-proxy`         | P1 로그인 라우팅 | ✅ 머지(#482)                    |
| 2   | `feature/admin-web-dashboard-dto`     | P3 AD-01   | ✅ 머지(#490)                    |
| 3   | `feature/admin-web-qt-passages-dto`   | P3 AD-02   | ✅ 머지(#496)                    |
| 4   | `feature/admin-web-notices-dto`       | P3 AD-06   | 진행 중(풀 CRUD)                  |
| 5   | `chore/admin-web-code-split`          | P5c        | 예정                            |
| 6   | `feature/admin-web-kakao-sdk-notice`  | P5b        | 예정                            |
| 7   | `docs/admin-web-token-storage-review` | P4         | 예정(강태오 확인)                    |
| 8   | `feature/admin-web-token-refresh`     | P2 refresh | 착수 가능(계약 확정)                  |
| 보류  | —                                     | P5a 찬양 숨김  | admin-server praise 컨트롤러 구현 후 |


> P3는 화면당 3개 PR로 분리(TODO 권장, 500라인 준수). 페이징은 기존 `hooks/usePagedList.ts` 재사용.

---

## P1 작업 순서 (완료)

1. `origin/dev`에서 `fix/admin-web-auth-proxy` 분기 (upstream 미설정).
2. `vite.config.ts`: `/api/v1/admin/auth`→`VITE_AUTH_PROXY_TARGET`(8081) 룰을 `/api`보다 위에 추가.
3. `.env.example`: `VITE_AUTH_PROXY_TARGET` 항목·설명 추가.
4. `LoginPage.tsx`: "백엔드 준비 전" 주석 정정.
5. `npm run typecheck` + `npm run build` 통과 → 커밋.

### ⚠️ 조율 (이승욱/강태오)

vite 주석 결정③(admin-web=8090 단일, 게이트웨이 라우팅)과 P1(auth만 8081 분리)이 겹침. dev용 분리는 가역적·게이트웨이 뜨면 `.env`로 통합 → "dev vite에 `/api/v1/admin/auth`→8081 분리룰 추가" 공유 후 PR. admin-server가 인증을 8081로 포워딩하는 방향이면 P1 불필요.

## P3a 대시보드 DTO 작업 순서 (진행 중, 렌더 A)

기준: 백엔드 `AdminDashboardResponse`(admin-server) + 계약서 `DevC_강상민/reports/2026-06-10_admin-dashboard-api_report.md`.

- DTO: `pendingAiValidationCount`·`receivedReportCount`·`reviewingReportCount`(number) + `todayQt`(**항상 non-null**, status `READY`/`MISSING`, 없으면 nullable 필드 null) + `recentAuditLogs[]`(sanitized: id·adminUserId·actorType·actionType·targetType·targetId·createdAt).

1. `src/api/dashboard.ts`: `[key:string]:unknown` → 백엔드 DTO 1:1 미러 타입(`DashboardSummary`, `TodayQt`, `TodayQtStatus`, `RecentAuditLog`).
2. `src/pages/DashboardPage.tsx` 렌더 A:
  - 상단 카운트 3개 = AntD `Statistic` 카드(AI 검증 대기·신고 접수·신고 검토).
  - Today QT = `Descriptions`(날짜·제목·상태 `Tag`·시뮬레이터 상태·해설 유무·캐시 상태). `MISSING` 시 빈값/안내.
  - 최근 감사로그 = `Table`(시각·작업유형·대상·작업자). 빈 배열이면 빈 상태.
  - "백엔드 준비 중" Alert 철거 → 실패 시 **에러 Alert + 재시도 버튼**.
3. `npm run typecheck` + `npm run build` 통과 → 커밋.

## P3c 공지 작업 순서 (계획, 풀 CRUD)

기준: 백엔드 `AdminNotice*Response`(admin-server) + 계약서 `DevC_강상민/reports/2026-06-10_admin-notices-api_report.md`.

- DTO: 목록 Item(`id`·`title`·`bodyPreview`·`status`·`publishedAt`·`createdAt`·`updatedAt`) / 상세(create·update 응답: `id`·`title`·`body`·`status`·...) / 발행 응답(`id`·`status`·`publishedAt`·`notificationResult`{requestedCount·createdCount·failedCount}).
- 상태 3종: `DRAFT`/`PUBLISHED`/`HIDDEN`. 버튼: DRAFT→수정·발행, PUBLISHED→숨김, HIDDEN→(없음).
- 공지 특성(강상민 리포트): **본문 plain text만**(`<`,`>` 거부) · **PATCH는 DRAFT만**(status 필드 보내면 400) · 발행 시 알림 fan-out.

1. `src/api/notices.ts`: 실타입(`Notice`·`NoticeDetail`·`NoticeStatus`·`NoticeRequest`·`PublishResult`) + create(POST)/update(PATCH) API. 기존 list/publish + hide 추가.
2. `src/pages/NoticesPage.tsx` 풀 CRUD:
  - `usePagedList` 목록 + 명시 컬럼(제목·미리보기·상태 Tag·발행시각) + 페이징.
  - 등록/수정 Modal 폼(제목 Input + 본문 TextArea, plain text 검증: `<`,`>` 금지).
  - 행 액션(상태별 수정/발행/숨김, Popconfirm). 발행 성공 시 **알림 결과**(생성/실패 수) 메시지.
  - "준비 중" Alert 철거 → 에러 Alert + 재시도.
3. `npm run typecheck` + `npm run build` 통과 → 커밋.

## P2 토큰 자동 갱신 작업 순서 (진행 중)

기준: 계약서 §6(2026-06-11) — 공용 `POST /api/v1/auth/refresh` 재사용, 재발급 role=ADMIN 유지.

- 토큰 2종: access(30분, 매 요청) / refresh(14일, access 재발급용). 로그인 응답 `AdminLoginResponse.refreshToken`이 현재 미사용 → 저장·활용.
- **single-flight**: 동시 다발 401에 refresh는 1회만(공유 Promise). `_retry` 플래그로 무한루프 방지. `/auth/refresh`·`/admin/auth/kakao`는 재시도 제외.
- **리다이렉트 방식 = B(AuthContext 콜백) 확정**: `client.ts`는 React/라우팅 비결합 유지 → `setAuthExpiredHandler(fn)` export, `AuthContext`가 `clearSession`을 등록. refresh 최종 실패/`M0006`(탈퇴)/`M0007`(정지) 시 토큰 정리 + 콜백 호출 → `ProtectedRoute`가 `/login`으로(새로고침 없음). 근거: 프로젝트가 ProtectedRoute SPA 네비게이션 사용, AuthContext가 인증 상태 단일 소유자.

1. `tokenStorage.ts`: `getRefreshToken`/`setRefreshToken` 추가, `clearTokens`(access+refresh 동시 제거).
2. `client.ts`: 응답 인터셉터에 single-flight refresh + 원요청 재시도 + `setAuthExpiredHandler`. refresh 실패/M0006/M0007 → `clearTokens` + 콜백.
3. `AuthContext.tsx`: `login(access, refresh?)`로 확장(둘 다 저장), `useEffect`로 `setAuthExpiredHandler(clearSession)` 등록.
4. `LoginPage.tsx`: 카카오 로그인 시 `login(res.accessToken, res.refreshToken)`. dev 토큰 로그인은 refresh 없음(access만).
5. `npm run typecheck` + `npm run build` 통과 → 커밋.

## 검증 계획

- 각 브랜치: `cd admin-web && npm run typecheck && npm run build` 통과.
- P1: dev 기동 시 카카오 로그인 → service-user(8081) 로그 도달, 비관리자 `403 ADMIN_USER_NOT_FOUND`·관리자 토큰 발급(이승욱 합동, VITE_KAKAO_JS_KEY 필요).

## 예상 리스크


| 리스크               | 대응                                                           |
| ----------------- | ------------------------------------------------------------ |
| 결정③ vs P1 라우팅 방식  | 이승욱/강태오 한 줄 확인, PR 본문 명시                                     |
| P3 백엔드 DTO 변경 가능성 | 계약서 기준 타입 작성, 변경 시 04 갱신 후 반영                                |
| P5a 보류 장기화        | admin-server praise 컨트롤러 구현 시 재개 (P2는 2026-06-11 계약 확정으로 해제) |


## 참고

- 승인 플랜: `C:\Users\G\.claude\plans\commit-prancy-yao.md`
- 리포트: [reports/2026-06-11_코드리뷰-P1-auth-proxy_리포트.md](../reports/2026-06-11_코드리뷰-P1-auth-proxy_리포트.md)

