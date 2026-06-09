# 2026-06-07 · 웹 dev 도구 / 작업기록 / 카카오 웹로그인(B안) — 리포트

대상: `QT-AI-2nd-Team-Project` · 워크플로우: workflows/2026-06-07_web-devtools-docs-kakao.md

## 1. 요약

음악 기능 머지 후 남아 있던 작업들을 3개의 PR로 정리했다. 핵심 웹 실행 호환은 이미 #313으로 dev에 있었고, 이번엔 ① 웹 dev 도구 ② 작업 기록 문서 ③ 카카오 웹 로그인 서버 OAuth 초안을 처리했다.

## 2. 산출물 (PR)

| PR | 제목 | base | 상태 | 비고 |
|---|---|---|---|---|
| #316 | chore: 웹 개발용 dev 도구 | dev | open | 서버 dev CORS + Flutter 웹 우회 + 실행 스크립트 |
| #317 | docs: Lead 작업 기록 정리(6/5~6/7) | dev | **merged** | 문서 27개 |
| #319 | feat(auth): 카카오 웹로그인 서버 OAuth(B안) | dev | **DRAFT** | §1 충돌·미검증, 머지 보류 |

## 3. 변경 요약

### #316 웹 dev 도구
- `DevSecurityConfig` 웹 CORS, `web_dev_access.dart`+`main.dart` 로그인 우회, `api_client`/`auth_providers` `X-Dev-User-Id` 주입, `run-dev-web.ps1/.sh`.
- 3중 게이트로 운영/모바일 무영향. `gradle.properties`는 환경별이라 제외.

### #317 작업 기록 문서
- reports/workflows/designs 27개. 코드 변경 없음(문서 전용).

### #319 카카오 웹 로그인 B안 (DRAFT)
- 신규: `KakaoWebAuthController`(`POST /api/v1/auth/kakao/web`), `KakaoWebAuthService`(코드→토큰 후 기존 `LoginUseCase` 재사용), `KakaoTokenClient`, `KakaoTokenResponse`, `KakaoCodeLoginRequest`, `LoginWithKakaoCodeUseCase`.
- 설정/보안: `application.yml kakao.oauth.*`, `SecurityConfig` permitAll.
- 테스트: `KakaoWebAuthServiceTest`, `KakaoWebAuthControllerTest`.
- Flutter(웹 전용): `web/index.html` JS SDK, `kakao_web_login`(facade/web/stub), `AuthRepository.loginWithKakaoWebCode`.
- 기존 `AuthController/AuthService`는 미변경(테스트 회귀 0).

## 4. 미해결 / 후속

- **#319는 머지 금지(DRAFT).** 필요: 강사/Lead 검토(§1 정책 변경), Kakao 콘솔(Web 플랫폼·Redirect URI·키), 환경변수(`KAKAO_REST_API_KEY`/`KAKAO_WEB_REDIRECT_URI`/JS키), 로그인 화면 `?code` wiring, 브라우저 실테스트.
- 꼬인 `feature/web-kakao-login` 로컬·stale 원격 정리.
- admin-web 골격(`c655d17`, T의 `feature/admin-web-scaffold`)은 별도 PR 대상(T 진행).

## 5. 리스크 / 주의

- 서버 OAuth는 보안 민감 + 정책 충돌 → 임의 머지 금지(draft 유지).
- 카카오 JS SDK 버전/콘솔 설정에 따라 동작 차이 → 브라우저 실검증 필수(샌드박스 미검증).
- dev에 required status checks 미설정 → auto-merge가 빌드 전 머지 가능. CI green 강제하려면 브랜치 보호에 추가 권장.
