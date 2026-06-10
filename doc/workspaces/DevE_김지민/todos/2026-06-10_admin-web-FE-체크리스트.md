# 2026-06-10 · admin-web(FE) 체크리스트 — 김지민 (MSA 8090 전환 후)

> 전략·배경(왜 MSA 재분리·왜 내 작업이 바뀌나)은 [워크플로우](../workflows/2026-06-10_admin-web-FE-워크플로우.md) §0 참조.
> 브랜치: `feature/admin-web-kakao-login` (PR 대상 `dev`).
> 표기: `[ ]` 미완 · `[x]` 완료 · `[~]` 진행 중 · 상태태그 **[지금]** 바로 / **[계약]** 합의 후
> 의존 계약: [카카오(이승욱)](../workflows/2026-06-10_admin-kakao-auth-contract.md) · [qt-passages(이지윤)](../workflows/2026-06-10_admin-qt-passages-api-contract.md) · dashboard·notices(강상민, **협의 불요** — 독립 구현)

---

## ▶ 0단계 — 배경·결정 확인 (먼저!)

- [x] **왜 바뀌었나 이해**: 06-08 MSA 결정 → 06-09 모놀리식 백엔드 착수 → 06-10 service-user/admin-server 추가 분리로 **관리자 백엔드가 admin-server(8090)로 이관**, 김지민은 **admin-web FE**로 전환 (워크플로우 §0)
- [x] **확정 결정 3가지**: ① 카카오 JS SDK(/oauth2 미사용) · ② 관리자 인증 카카오 단일 · ③ admin-web 처음부터 8090
- [x] **카카오 경로 충돌 해소**: 신규 `POST /api/v1/admin/auth/kakao` 채택 확정
- [x] **이전 트랙 정리 인지**: 모놀리식 AD-01/02/06 백엔드(`feature/admin-backend-ad010206`)는 **이관됨** — 더 진행 안 함. dashboard·notices는 강상민이 독립 구현(내 코드 미재사용) → **협의 불요**

---

## ▶ 1단계 — [지금] 로그인 기반 (env → 카카오 → 토큰) ✅ 코드 완료(2026-06-10)

> ✅ F1·F2·F3 **코드 구현 + typecheck·build 통과**. 단 실제 로그인 *실행*은 이승욱 `admin/auth/kakao`·카카오 JS키·콘솔 도메인 준비 후 가능(코드 수정 불요). 그 전 로컬은 dev 모드 '개발용 토큰'으로.

### F3. config/env: base URL = 8090 ✅
- [x] `.env.example`: 프록시 **8090(admin-server)** + `VITE_KAKAO_JS_KEY` 추가 (`.env` 실제값은 로컬에서)
- [x] `vite.config.ts` proxy 기본 `8080 → 8090`
- [x] `src/config/env.ts`: `KAKAO_JS_KEY` export 추가
- [ ] ⚠️ `admin/auth/kakao` 실제 라우팅(8090 admin-server 경유 vs service-user) 이승욱·강상민과 확인 — **열린 질문**

### F1. 카카오 JS SDK 로그인 버튼 ✅
- [x] `VITE_KAKAO_JS_KEY` 주입 경로 마련(루트 `.env` `KAKAO_JS_KEY` → admin-web `.env`)
- [x] Kakao JS SDK 로드(`index.html` CDN) + `src/auth/kakao.ts`의 `loginWithKakao()`에서 `Kakao.init`
- [x] `LoginPage.tsx`: 임시 토큰 붙여넣기 → **카카오 로그인 버튼**(임시토큰은 dev 모드에서만 유지, prod 제거)
- [x] 카카오 로그인 실행 → access token 획득 (`loginWithKakao`)

### F2. 토큰 교환 → ADMIN 토큰 저장 ✅
- [x] `src/api/adminAuth.ts`(신규): `POST /api/v1/admin/auth/kakao` { kakaoAccessToken } 호출
- [x] `AuthContext`(`login`)로 ADMIN accessToken 저장 + 기존 `/admin/me`로 `adminRole` 확인

**합의된 카카오 응답 계약 5개 반영** ([계약](../workflows/2026-06-10_admin-kakao-auth-contract.md) §7):
- [x] ① 응답 키 `admin` 블록 파싱 (`AdminLoginResponse.admin`)
- [x] ② `adminRole` 단일 역할 문자열 타입(`AdminRole`)
- [x] ③ `refreshToken` body 타입 수신 (자동 refresh 흐름은 후속 TODO)
- [x] ④ 403 등 `[error.code] message` 그대로 표시
- [x] ⑤ access 30분/refresh 14일 — 계약 주석 명시 (만료 시 401→재로그인)

- [x] 검증: `npm run typecheck` + `npm run build` **통과**
- [ ] (백엔드·키 준비 후) 카카오 로그인 → 대시보드 진입 수동 e2e 확인

---

## ▶ 2단계 — [계약] 화면 실제 API 연결 (계약 합의 후)

### qt-passages (AD-02 · 이지윤) — 계약 거의 합의, 선결 2건 대기
- [ ] **선결**: ① 요청 필드 04 명세 갱신(book+chapter+verse) · ② 상태값 3종 vs 결정② 5종 확정 ([계약 문서](../workflows/2026-06-10_admin-qt-passages-api-contract.md) §10)
- [ ] `src/api/qtPassages.ts`: generic `QtPassage` → 계약 §6 단건 응답으로 구체화 + `create/update` 함수
- [ ] `src/pages/QtPassagesPage.tsx`: 상태 Tag + 게시/숨김/등록/수정 버튼
- [ ] (페이징·에러는 변경 불필요 — 이미 일치)

### dashboard (AD-01 · 강상민) — 협의 불요(강상민 독립 구현)
> MSA 분리로 김지민이 짠 AD-01 백엔드는 미재사용 → 강상민이 admin-server에 독립 구현. **김지민 협의 불필요**(04 명세 기준). API 올라오면 연결만.
- [ ] (강상민 API 후) `src/api/dashboard.ts` + `DashboardPage` 실제 연결

### notices (AD-06 · 강상민) — 협의 불요(강상민 독립 구현)
> 위와 동일 — 강상민 독립 구현, 04 명세 기준, **협의 불필요**.
- [ ] (강상민 API 후) `src/api/notices.ts` + `NoticesPage` 실제 연결

---

## ▶ 3단계 — 정합·게이트

- [ ] admin-web 8090 정합 후 PR(base `dev`, F-06)
- [ ] (Lead 게이트) admin-web→dev 반영 → 모놀리식 원본 제거 후속

---

## 검증 게이트 (각 PR 공통)

- [ ] `cd admin-web && npm run typecheck` + `npm run build`
- [ ] 로컬 화면 확인(로그인→대시보드, 각 화면 동작), 에러 시 토스트/ErrorCode 표시
- [ ] 금지 규칙: 서버 `/oauth2` 미사용 · (AD-05) 가사·음원·URL 저장 금지

---

## 진행

- [x] 2026-06-10 MSA 8090 전환 반영, FE 워크플로우·체크리스트 신규 작성
- [x] 2026-06-10 **1단계 [지금] F3·F1·F2 코드 구현 완료**(env 8090 · 카카오 SDK 버튼 · `admin/auth/kakao` 연동 · 합의 5개), typecheck·build 통과
- [ ] 2단계 [계약] qt-passages(선결 2건 후) · dashboard·notices(강상민 API 후 연결, 협의 불요)
