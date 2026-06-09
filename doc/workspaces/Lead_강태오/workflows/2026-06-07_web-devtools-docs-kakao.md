# 2026-06-07 · 웹 dev 도구 / 작업기록 문서 / 카카오 웹로그인(B안) — 워크플로우

작성: Claude (Lead T 지시) · 대상: `QT-AI-2nd-Team-Project` (구현)

## 0. 배경

음악 기능(#315) 머지 후, 작업트리에 남아 있던 미커밋들을 "순서대로 다 처리"하기로 함. 세 묶음:
1. T가 진행 중이던 **웹 실행용 임시 dev 도구**(미커밋 코드 + 스크립트)
2. 6/5~6/7 **작업 기록 문서**(미커밋)
3. **카카오 웹 로그인** — 설계안만 있던 것을 초안 구현

핵심 웹 실행 호환(dart:io 제거·호스트 분기·TTS 웹)은 이미 #313으로 dev에 머지된 상태였음.

## 1. Part 1 — 웹 개발용 dev 도구 (PR #316)

- 서버 `DevSecurityConfig`에 dev 프로파일 웹 CORS 허용(localhost:3000).
- Flutter 웹 로그인 우회(`core/dev/web_dev_access.dart` + `main.dart`), `X-Dev-User-Id` 헤더 주입(`api_client`/`auth_providers`). 3중 게이트(kIsWeb+dev+`--dart-define`)로 운영 무해.
- 실행 스크립트 `run-dev-web.ps1/.sh`.
- 커밋 2개로 분리(서버 chore / Flutter chore). `flutter-app/android/gradle.properties`(Flutter 마이그레이터 자동 플래그)는 환경별이라 **커밋 제외**.
- 브랜치 `chore/web-dev-tools`(dev 기준), PR base dev.

## 2. Part 2 — 작업 기록 문서 (PR #317, dev 머지 완료)

- `doc/workspaces/Lead_강태오/`의 reports/workflows/designs 27개(full-codebase-review, p0/p1/p2 fixes, fe-prep-i18n, web-run-support, admin-web 골격, 카카오 설계안 등) 커밋.
- 코드는 각 PR로 이미 반영됐고 문서만 미커밋이었음. 브랜치 `docs/work-records`, base dev → 머지됨.

## 3. Part 3 — 카카오 웹 로그인 서버 OAuth(B안) (DRAFT PR #319)

- **사전 조사(웹 검색):** 현행 카카오 JS SDK는 `Kakao.Auth.login`(직접 토큰) 폐지 → `Kakao.Auth.authorize()` 인가코드 리다이렉트 방식. 코드→토큰 교환은 서버에서만 가능(REST키/CORS). 즉 "서버 변경 없는 웹 로그인(A안)"은 불가, 서버 OAuth(B안)가 필요.
- **정책 충돌:** CLAUDE.md §1(서버사이드 OAuth 미사용)과 충돌 → **DRAFT**로 만들고 강사/Lead 검토 전제.
- **구현(기존 인증 미변경, 신규 추가):** `POST /api/v1/auth/kakao/web`(`KakaoWebAuthController`) → `KakaoWebAuthService`(코드→토큰 교환 후 기존 `LoginUseCase.login(토큰)` 재사용) → `KakaoTokenClient`(kauth.kakao.com 교환, redirect_uri는 서버 설정값=오픈리다이렉트 방지). `application.yml kakao.oauth.*`, `SecurityConfig` permitAll, 테스트 2건.
- **Flutter(웹 전용):** `web/index.html` 카카오 JS SDK, `kakao_web_login`(facade/web/stub, js_interop authorize+코드읽기), `AuthRepository.loginWithKakaoWebCode(code)`.

## 4. Git / PR 처리에서 부딪힌 것

- **브랜치명 CI 규칙:** 음악 PR이 `feat/`로 `Branch Name Convention` 실패(허용 `feature|bugfix|hotfix|chore|release|docs|test`). → `feature/`로 재생성(#314 닫고 #315).
- **stacked PR → dev 정리:** 음악/카카오 작업이 미머지 브랜치 위에 얹혀 있어, 격리 `git worktree` + `cherry-pick`으로 **해당 커밋만 dev 위로** 옮겨 클린 PR 생성(작업트리 비파괴).
- **auto-merge:** dev에 required status checks가 없어 `gh pr merge --auto`가 즉시 머지됨(이후 빌드/테스트 green 확인).
- **동시 작업 충돌:** T가 같은 repo에서 admin-web 골격(`c655d17`)을 동시 커밋 → 카카오 브랜치가 그 위에 얹혀 꼬임. clean dev 기준 `feature/web-kakao-login-draft`로 분리해 #319 생성, T 작업은 보존.

## 5. 결과 PR

| PR | 내용 | base | 상태 |
|---|---|---|---|
| #316 | 웹 dev 도구 | dev | open |
| #317 | 작업 기록 문서 | dev | merged |
| #319 | 카카오 웹로그인 B안 | dev | **DRAFT** |

## 6. 검증 / 후속

- #316: 검토·머지.
- #319: 강사/Lead 검토 + Kakao 콘솔(Web 플랫폼/Redirect/키) + 브라우저 테스트 + 로그인화면 `?code` wiring 필요. 그 전엔 머지 금지.
- 꼬인 로컬 `feature/web-kakao-login`(+stale 원격)은 정리 가능.
