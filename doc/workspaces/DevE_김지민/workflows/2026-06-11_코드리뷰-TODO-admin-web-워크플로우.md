# 코드리뷰 TODO 수정 워크플로우 — admin-web (2026-06-11)

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 트랙 | `dev` (모든 브랜치 base = `origin/dev`, push 후 dev 대상 PR) |
| PR 대상 | `dev` |
| 출처 | `doc/workspaces/DevE_김지민/2026-06-10_코드리뷰_TODO_김지민.md` (P1~P5) |
| 기준 문서 | `04_API_명세서.md`, 백엔드 계약서(아래 의존성 표) |
| 담당 경로 | `admin-web/` (React + TS + Vite + AntD) |
| 검증 게이트 | `npm run typecheck`(strict) + `npm run build` (테스트 프레임워크 없음) |
| 상태 | P1 구현·커밋 완료(`fix/admin-web-auth-proxy`), 다음 P3 |

---

## 작업 목표

코드리뷰 후속 TODO 5항목(P1~P5)을 작은 PR로 순차 수정. **dev 최신화로 P3(AD-01/02/06)이 새로 풀림** — 백엔드 계약 머지 완료로 FE 타입 교체 즉시 가능.

---

## 의존성 재확인 (2026-06-11, 현재 dev 기준)

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| P1 로그인 라우팅 | ✅ 가능 (조율 1건) | vite `/api`→8090 단일룰. 관리자 로그인은 service-user(8081, #452)에만, admin-server엔 auth 컨트롤러 없음 |
| P3 AD-01 대시보드 | ✅ 풀림 | `AdminDashboardResponse`(#449) + 계약서 |
| P3 AD-02 QT관리 | ✅ 풀림 | `AdminQtPassageResponse`(#454) + 김지민 확인 계약서 |
| P3 AD-06 공지 | ✅ 풀림 | `AdminNoticeListResponse`/`Detail`/`Publish`(#450) |
| P4 토큰 보관 | ✅ 가능 (주석/결정) | localStorage `qtai_admin_token`, HttpOnly 전환 시점 강태오 결정 |
| P5b SDK 안내 | ✅ 가능 | `kakao.ts` 키 미설정 throw → LoginPage Alert |
| P5c code-split | ✅ 가능 | vite manualChunks 없음(1.15MB 단일청크, 빌드 경고 확인) |
| P2 refresh | ⚠️ 부분 막힘 | service-user `/api/v1/auth/refresh` 있으나 관리자 전용 경로 미분리 → 이승욱 계약 결정 |
| P5a 찬양 숨김 | ❌ 막힘 | admin-server praise web 컨트롤러 없음(PATCH/hide 미구현) |

---

## 브랜치 로드맵 (순차, base=origin/dev)

| 순서 | 브랜치 | 항목 | 상태 |
| --- | --- | --- | --- |
| 1 | `fix/admin-web-auth-proxy` | P1 로그인 라우팅 | ✅ 구현·커밋(`5ecf935`) |
| 2 | `feat/admin-web-dashboard-dto` | P3 AD-01 | 예정 |
| 3 | `feat/admin-web-qt-passages-dto` | P3 AD-02 | 예정 |
| 4 | `feat/admin-web-notices-dto` | P3 AD-06 | 예정 |
| 5 | `chore/admin-web-code-split` | P5c | 예정 |
| 6 | `feat/admin-web-kakao-sdk-notice` | P5b | 예정 |
| 7 | `docs/admin-web-token-storage-review` | P4 | 예정(강태오 확인) |
| 보류 | — | P2 refresh | 이승욱 계약 결정 후 |
| 보류 | — | P5a 찬양 숨김 | admin-server praise 컨트롤러 구현 후 |

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

## 검증 계획

- 각 브랜치: `cd admin-web && npm run typecheck && npm run build` 통과.
- P1: dev 기동 시 카카오 로그인 → service-user(8081) 로그 도달, 비관리자 `403 ADMIN_USER_NOT_FOUND`·관리자 토큰 발급(이승욱 합동, VITE_KAKAO_JS_KEY 필요).

## 예상 리스크

| 리스크 | 대응 |
| --- | --- |
| 결정③ vs P1 라우팅 방식 | 이승욱/강태오 한 줄 확인, PR 본문 명시 |
| P3 백엔드 DTO 변경 가능성 | 계약서 기준 타입 작성, 변경 시 04 갱신 후 반영 |
| P2/P5a 보류 장기화 | 의존(계약/백엔드) 해제 시 재개, 로드맵에 명시 |

## 참고

- 승인 플랜: `C:\Users\G\.claude\plans\commit-prancy-yao.md`
- 리포트: [reports/2026-06-11_코드리뷰-P1-auth-proxy_리포트.md](../reports/2026-06-11_코드리뷰-P1-auth-proxy_리포트.md)
