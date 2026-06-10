# 2026-06-10 · admin-web(FE) 워크플로우 — 김지민 (MSA 8090 전환 후)

> 출처: 팀원별 TODO(2026-06-10 결정 반영본) 中 **김지민(admin-web)** 파트.
> 브랜치: `feature/admin-web-kakao-login` (최종 PR 대상은 공통규칙상 `dev`).
> 이전 트랙(중단/이관): [AD010206 백엔드 워크플로우](2026-06-09_admin-backend-AD010206-워크플로우.md) · [체크리스트](../todos/2026-06-09_admin-backend-AD010206-체크리스트.md)
> 의존 계약: [카카오 인증(이승욱)](2026-06-10_admin-kakao-auth-contract.md) · [qt-passages(이지윤)](2026-06-10_admin-qt-passages-api-contract.md) · dashboard·notices(강상민, 계약 대기)
> 짝 문서: [admin-web FE 체크리스트](../todos/2026-06-10_admin-web-FE-체크리스트.md)

---

## 0. 왜 다시 MSA를 분리했고, 내 작업이 왜 여기서 멈추고 새 작업으로 가나 (입문자용)

**타임라인**

| 날짜 | 무슨 일 | 김지민 작업 |
|---|---|---|
| 2026-06-08 | Lead가 MSA 분리(v2) 결정. **1차 추출 = ai-service·bible**(+게이트웨이). admin·qt·notification·member은 아직 모놀리식 `qtai-server`에 남아 "작업 안전". | — |
| 2026-06-09 | 그 "안전" 전제로 **김지민이 모놀리식에서 관리자 백엔드 AD-01/02/06을 직접 구현** 착수(`feature/admin-backend-ad010206`). AD-01 대시보드까지 구현·테스트 통과. | 관리자 **백엔드** 구현 |
| 2026-06-10 | MSA가 **한 단계 더 분리**: **service-user(인증)** + **admin-server(관리자 API)** 를 별도 서비스로 떼어냄. **"admin-web은 처음부터 8090(admin-server) 기준"** 으로 확정. | 관리자 **프런트(admin-web)** 로 전환 |

**그래서 무엇이 바뀌나**

- 김지민이 모놀리식에서 짜던 **AD-01/02/06 백엔드는 admin-server(8090)로 재배치**된다 →
  - **dashboard·notices = 강상민**, **qt-passages = 이지윤**, **카카오 인증 = 이승욱(service-user)** 이 각자 admin-server/service-user에 신설.
- **김지민의 역할은 "관리자 백엔드 구현자" → "admin-web(FE) 구현자 + 계약 소비자"로 이동.** 즉 내가 모놀리식에서 짜던 백엔드는 **여기서 멈추고(이관)**, 나는 admin-web 화면이 **8090 API를 호출**하도록 붙이는 일을 한다.
- 모놀리식 원본(`qtai-server/src`)은 admin-web이 8090으로 옮겨간 뒤 **Lead가 제거**(취합 게이트 ③).

> **한 줄 요약** — "왜 멈췄나" = 관리자 백엔드가 내 손(모놀리식)에서 **admin-server(강상민·이지윤·이승욱)** 로 옮겨갔기 때문. "새 작업" = 그 **8090 서비스들을 소비하는 admin-web 프런트**.

---

## 1. 확정된 결정 (2026-06-10)

1. **카카오 = JS SDK 방식** — 서버 `/oauth2` 미사용, 앱과 동일. 카카오 토큰만 서버로 전달.
2. **관리자 인증 = 카카오 단일** — `members.role=ADMIN` + `admin_users.admin_role` 확인.
3. **admin-web = 처음부터 8090(admin-server) 기준.**

> ✅ **이전 충돌 해소**: 06-09 ③A(기존 `/auth/kakao` 재사용) ↔ 06-10 신규 `admin/auth/kakao` 충돌은 → **신규 `POST /api/v1/admin/auth/kakao` 채택**으로 확정. (카카오 응답 형태 5개도 [계약](2026-06-10_admin-kakao-auth-contract.md) §7 합의 완료)
>
> ✅ **SSoT 반영(본 PR) — Lead 명시 승인 코멘트 필요(`CLAUDE.md §2`)**: SSoT 의존 2건을 **본 PR에서 반영**했다. ① `CLAUDE.md §5`에 admin `/admin/auth/kakao` 추가. ② `04_API_명세서.md §4.7.2` 요청필드를 `bookId+chapter+startVerse+endVerse`로 갱신(예시 status도 5종 `pending_review`로 정렬). 근거=2026-06-10 팀 결정. §2상 SSoT 변경은 Lead 사전 검토 대상 → **Lead(강태오)의 명시 승인 코멘트를 머지 전 첨부**. (04는 별도 문서 저장소 캐논 동기화가 추가로 필요.)

---

## 2. 김지민 작업 범위 (admin-web FE)

> 상태: **[지금]** 바로 가능 · **[계약]** 계약 합의 후

| # | 작업 | 상태 | 의존 | 주요 파일 |
|---|---|---|---|---|
| F1 | LoginPage: 카카오 JS SDK 로그인 버튼 (임시 토큰 붙여넣기 제거) | [지금] ✅ | `KAKAO_JS_KEY`(루트 `.env`)→`VITE_KAKAO_JS_KEY` | `src/pages/LoginPage.tsx`, `index.html`(SDK 로드), `src/auth/kakao.ts` |
| F2 | 카카오 토큰 → `POST /api/v1/admin/auth/kakao` → ADMIN 토큰 저장 | [지금] ✅ | 이승욱 계약(합의 완료) | `src/api/adminAuth.ts`, `src/auth/AuthContext.tsx` |
| F3 | config/env: base URL = **8090**(admin-server) 기준 | [지금] ✅ | — | `.env.example`, `src/config/env.ts`, `vite.config.ts` |
| F4 | 화면 실제 API 연결: qt-passages·dashboard·notices | [계약] | 이지윤(✅확정)·강상민(협의 불요) | `src/api/{qtPassages,dashboard,notices}.ts`, `src/pages/*` |
| F5 | 계약 정리: 이승욱(인증 ✅)·이지윤(qt-passages ✅확정)·강상민(협의 불요) | [계약] | — | (문서) |

> ✅ **F1·F2·F3 코드 구현 완료(2026-06-10), typecheck·build 통과.** 실제 로그인 실행은 백엔드·카카오키 준비 후(코드 수정 불요).

**현재 admin-web 상태(참고):**
- `LoginPage.tsx` = 임시 토큰 붙여넣기(`Input.Password`) — F1에서 교체.
- `config/env.ts` = `VITE_API_BASE_URL ?? '/api/v1'`, `vite.config.ts` proxy target = **8080** — F3에서 8090으로.
- Kakao SDK·`VITE_KAKAO_JS_KEY`·`.env` 파일 **없음** — F1/F3에서 추가.
- `qtPassages.ts`·`DashboardPage` 등은 generic stub — F4에서 계약대로 구체화.

---

## 3. 의존 계약 상태 (계약 소비자로서)

| 담당 | 대상 | 계약 문서 | 상태 |
|---|---|---|---|
| 이승욱 | 카카오 인증 (`admin/auth/kakao`) | [admin-kakao-auth-contract](2026-06-10_admin-kakao-auth-contract.md) | ✅ 응답 5개 합의 완료 · **FE F1/F2 코드 완료** · 백엔드 엔드포인트 구현 대기(이승욱 task 3) |
| 이지윤 | qt-passages (AD-02) | [admin-qt-passages-api-contract](2026-06-10_admin-qt-passages-api-contract.md) | ✅ **확정(2026-06-10)** — 요청필드 `bookId+chapter+startVerse+endVerse`(04 §4.7.2 갱신 예정) · 상태값 **5종 최종**(`active/hidden/pending_review/deletion_notified/removed`, 3종은 매핑). FE는 5종 Tag/버튼·등록폼 구현 예정 |
| 강상민 | dashboard(AD-01)·notices(AD-06) | (협의 불요) | ✅ **협의 불필요** — MSA 분리로 김지민 코드 미재사용, 강상민이 admin-server에 **독립 구현**(04 명세 기준). API 올라오면 FE는 연결만 |

---

## 4. 진행 순서 (권장)

1. **[지금] 로그인 기반 먼저** — F3(env 8090) → F1(카카오 SDK 버튼) → F2(`admin/auth/kakao` 연동 → ADMIN 토큰 저장). 로그인이 돼야 다른 화면 로컬 확인이 가능하므로 최우선.
2. **[계약] 계약 확정된 화면부터** — qt-passages(✅확정 — 5종 Tag/버튼·등록폼 구현) → dashboard·notices(강상민 API 후, 협의 불요).
3. **[정합]** admin-web을 8090 정합 → `dev`.
4. **(게이트=Lead)** dev-msa→dev → admin-web→dev → 모놀리식 원본 제거 → dev→master.

---

## 5. 검증 게이트 (각 작업 공통)

- `cd admin-web && npm run typecheck`(strict) + `npm run build`. (admin-web은 테스트 프레임워크 없음)
- 로컬: 카카오 로그인 → 대시보드 진입, 각 화면 표·필터·동작. 비관리자 로그인 시 403 안내(`ADMIN_USER_NOT_FOUND`, ErrorCode 그대로 표시).
- 백엔드 8090 미가동 시 화면은 "준비 중" 유지(현 stub 동작).

---

## 6. 공통 규칙

- 브랜치는 `dev`에서 분기 · `dev`/`master` 직접 push 금지 · PR 대상 `dev` · 브랜치명 `{type}/{scope}-{설명}` · Conventional Commits · PR 10파일·500줄 이하 · 기능 PR에 **F-06** 명시.
- 서버 `/oauth2` 미사용 — 카카오는 JS SDK 토큰을 서버로 전달.
- 작업분은 워크플로우·리포트로 정리.

---

## 7. 확인 필요 (열린 질문)

- **카카오 엔드포인트 라우팅 — 처리 방침(명문화)**: FE는 **base 8090 + `/api/v1/admin/auth/kakao` 경로로 고정 호출**한다. 실제 라우팅(admin-server가 직접 노출 vs 게이트웨이/service-user 경유)은 **BE(이승욱·강상민)가 확정**하며, **경로 문자열이 동일해 FE 코드는 영향 없음**(라우팅 확정 시 FE 변경 불필요). 확정 책임·기한은 이승욱·강상민 협의로 처리하고, 본 워크플로우는 경로 고정만 보장한다.
- **카카오 콘솔 도메인 등록(나중)**: Web 플랫폼 `http://localhost:5173` 등록 후 팝업/토큰 획득 로컬 확인 가능. 로컬 `admin-web/.env`(gitignore)에 JS 키는 주입 완료.
- qt-passages는 ✅ 확정(요청필드 `bookId+chapter+startVerse+endVerse` + 상태값 5종) — 5종 Tag/버튼·등록폼 구현 가능. (04 §4.7.2 갱신은 별도 PR)

---

## 진행 로그

- 2026-06-10: MSA 8090 전환 반영. admin-web FE 워크플로우·체크리스트 신규 작성. 카카오 경로 충돌 해소(신규 `admin/auth/kakao` 확정) 반영.
- 2026-06-10: **F3·F1·F2 코드 구현 완료** — env 8090, 카카오 JS SDK 로그인 버튼, `admin/auth/kakao` 연동, 합의 5개 반영. typecheck·build 통과. dashboard·notices(강상민)는 협의 불요로 확정.
- 2026-06-10: 로컬 `admin-web/.env`(gitignore)에 카카오 JS 키 주입 완료(`.env.example`은 플레이스홀더 유지). **카카오 콘솔 Web 도메인(`localhost:5173`) 등록은 나중.**
- 2026-06-10: **SSoT 모순 2건 해소** — ① 카카오 경로 **신규 `admin/auth/kakao` 확정** ② qt-passages(이지윤 협의) **요청필드 `bookId+chapter+startVerse+endVerse` + 상태값 5종 최종** 확정(04 §4.7.2 갱신 예정). **다음: qt-passages 5종 Tag/버튼·등록폼 구현.**
