# 강태오 (Lead) — 관리자 웹(admin-web) 작업 일정

- 기준일: 2026-06-08
- 기준: admin-web 골격(#318) 머지 완료 · `origin/dev`
- 담당(제안 적용본): 환경 준비 · dev 관리자 인증 경로 · 권한 정책 확정 · 백엔드 미비 화면 협의 · 카카오 웹 로그인. (화면 구현은 김지민, 강태오는 리뷰)

> **권장 순서:** A1 → A2~A4 → B1 → B2 → B3 → D1 → E1~E3 → F1

## A. 작업 준비

- [ ] **A1.** 미커밋 변경 정리 — `pubspec.lock` + 잡파일(dartv/ftool/fver/run_app) 버리거나 분리 (빌드·임시파일 커밋 금지)
- [ ] **A2.** 로컬 dev 최신화 — 원격보다 7개 뒤처짐 · `git switch dev && git pull` _(공통)_
- [ ] **A3.** admin-web 작업 브랜치 생성 — dev에서 `feature/admin-web-...` _(공통)_
- [ ] **A4.** 로컬 실행 확인 — `cd admin-web` → `npm install` → `npm run dev` → localhost:5173 _(공통)_

## B. dev 관리자 로그인·인증 (주도)

- [ ] **B1.** dev 관리자 계정 시드 — `admin_users` + `members(role=ADMIN)` (현재 dev에 없음)
- [ ] **B2.** admin-web ↔ dev 인증 연결 — 식별 헤더 전송 또는 dev 전용 관리자 토큰 발급 경로
- [ ] **B3.** 로그인 흐름 점검 — 보호 라우트(ProtectedRoute) + 실제 admin API 200 확인

## C. 화면 리뷰·정책

- [ ] **C(리뷰).** 김지민 화면 PR 리뷰 — 도메인 경계·권한·프로젝트 규칙 준수
- [ ] **C3.** 정책 확인 — AD-03 승인 전 AI 원문 비노출 보장

## D. 권한 정책

- [ ] **D1.** 권한표 확정 — `admin-web/src/constants/menu.ts` 추정 권한 → `04_API_명세서` §AD-01~08 기준 확정
- [ ] **D3.** 금지 규칙 점검 — AD-05 가사·음원·URL 저장 금지 등 (CLAUDE.md §8) _(공통)_

## E. 백엔드 미비 화면 협의 (주도)

- [ ] **E1.** AD-01 대시보드 — `/api/v1/admin/dashboard` 존재 확인·요청
- [ ] **E2.** AD-02 오늘 QT 관리 — `/api/v1/admin/qt-passages` 백엔드 없음 → 설계·요청
- [ ] **E3.** AD-06 시스템 공지 — `/api/v1/admin/notices` 백엔드 없음 → 설계·요청

## F. 인증 정식화·마무리

- [ ] **F1.** 카카오 웹 로그인 정식화 — `web-kakao-login-draft`(B안) · `/oauth2` 미사용 규칙 충돌 여부 Lead 결정
- [ ] **F3.** 워크플로우·리포트 정리 — 작업 저장소 작업은 항상 기록 _(공통)_

---

- 작업 워크플로우: `doc/workspaces/Lead_강태오/workflows/2026-06-08_admin-web-task-schedule.md`
- 참고: `admin-web/README.md`, `doc/workspaces/Lead_강태오/designs/2026-06-07_web-kakao-login-server-oauth_design.md`
