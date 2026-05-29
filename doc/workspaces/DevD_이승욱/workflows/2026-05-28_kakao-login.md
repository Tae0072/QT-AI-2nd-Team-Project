# 2026-05-28 카카오 OAuth 로그인 Flutter 구현

## 목표
F-01 카카오 소셜 로그인을 Flutter 앱에 구현한다. 카카오 SDK로 토큰을 발급받고 서버 `POST /api/v1/auth/kakao`에 전달하여 JWT를 교환하는 흐름을 완성한다.

## 작업 내용
1. **카카오 SDK 의존성 추가** — `pubspec.yaml`에 `kakao_flutter_sdk_user` 추가
2. **Android 설정** — `AndroidManifest.xml`에 카카오 로그인 스킴(`kakao{NATIVE_APP_KEY}://oauth`) + `CustomTabsActivity` 설정
3. **카카오 SDK 초기화** — `main.dart`에 `KakaoSdk.init()` 추가, 네이티브 앱 키는 `AppConfig`에서 환경별 로딩 (`.env` 관리)
4. **AuthRepository 구현** — 카카오 토큰 발급(`UserApi.loginWithKakaoTalk/Account`) → `POST /api/v1/auth/kakao`로 서버 JWT 교환
5. **LoginScreen 구현** — 카카오 로그인 버튼 + 게스트 모드(둘러보기) UI
6. **NicknameSetupScreen 구현** — 신규 사용자 닉네임 설정 (2~10자 유효성 검증)
7. **라우터 연결** — `AppRouter`에 `/login`, `/nickname-setup` 라우트 추가, `auth_providers.dart`에 `authRepositoryProvider` + `authStatusProvider` 구성

## 범위
- 브랜치: `feature/kakao-login`
- PR: Open (리뷰 대기)
- 커밋: `feat(auth): 카카오 OAuth 로그인 Flutter 구현`
- 변경: 9파일 (`pubspec.yaml`, `pubspec.lock`, `AndroidManifest.xml`, `main.dart`, `app_router.dart`, `auth_repository.dart`, `auth_providers.dart`, `login_screen.dart`, `nickname_setup_screen.dart`)
- 관련: F-01 카카오 소셜 로그인

## 의존
- `chore/spring-dotenv-setup` (Merged) — `.env`로 카카오 앱 키 관리
- `bugfix/onboarding-init-route` (#133, Merged) — 온보딩→로그인 흐름 전제
- 위 2개 PR 머지 후 dev rebase하여 충돌 해결 후 머지 예정

## 미해결
- PR 리뷰 대기
- 서버 측 `POST /api/v1/auth/kakao` 엔드포인트는 `feature/member-kakao-auth` 브랜치에서 별도 구현
- iOS 설정 (Info.plist 카카오 스킴) 미적용 — Android 우선

## 담당
- DevD 이승욱
