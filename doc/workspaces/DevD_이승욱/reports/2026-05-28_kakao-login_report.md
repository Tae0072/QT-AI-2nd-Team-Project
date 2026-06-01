# 2026-05-28 카카오 OAuth 로그인 Flutter 구현 — 결과 보고

## 요약
F-01 카카오 소셜 로그인 Flutter 구현. 카카오 SDK로 토큰 발급 → `POST /api/v1/auth/kakao`로 서버 JWT 교환 흐름 완성. LoginScreen(카카오 로그인 버튼 + 게스트 모드), NicknameSetupScreen(2~10자 닉네임 유효성 검증) 구현. 의존 PR 2건(#131 spring-dotenv, #133 onboarding-init-route) Merged 후 dev merge + 충돌 해결 완료. PR Open (리뷰 대기).

## 산출물

### 구현 파일

| 파일 | 설명 |
|------|------|
| `flutter-app/pubspec.yaml` | `kakao_flutter_sdk_user` 의존성 추가 |
| `flutter-app/android/app/src/main/AndroidManifest.xml` | 카카오 로그인 스킴 + `CustomTabsActivity` 설정 |
| `flutter-app/lib/main.dart` | `KakaoSdk.init(nativeAppKey: AppConfig.instance.kakaoNativeAppKey)` 추가 |
| `flutter-app/lib/features/auth/services/auth_repository.dart` | 카카오 토큰 발급 → 서버 JWT 교환 + SecureStorage 저장 |
| `flutter-app/lib/features/auth/providers/auth_providers.dart` | `authRepositoryProvider`, `authStatusProvider` 구성 |
| `flutter-app/lib/features/auth/screens/login_screen.dart` | 카카오 로그인 버튼(#FEE500) + 에러 메시지 + 로딩 상태 |
| `flutter-app/lib/features/auth/screens/nickname_setup_screen.dart` | 2~10자 닉네임 유효성 검증 + 서버 등록 |
| `flutter-app/lib/routes/app_router.dart` | `/login`, `/nickname-setup` 라우트 추가 |

### 테스트 파일

| 파일 | 케이스 수 | 설명 |
|------|-----------|------|
| `test/features/auth/providers/auth_providers_test.dart` | 3 | AuthStatus enum, LoginResult 모델 검증 |
| `test/features/auth/screens/login_screen_test.dart` | 6 | LoginScreen 위젯 렌더링, 카카오 버튼, 초기 상태 검증 |
| `test/features/auth/screens/nickname_setup_screen_test.dart` | 9 | NicknameSetupScreen 위젯 렌더링, 닉네임 유효성 검증 (빈값/1자/특수문자/maxLength) |
| `test/routes/app_router_test.dart` | 수정 | 기존 `'로그인'` → `'카카오 로그인'` 텍스트 변경 반영 |

## 검증
- `flutter analyze` — No issues found
- `flutter test` — 94건 전체 통과 (신규 auth 테스트 18개 포함)
- 금지 기술/기능 — 위반 없음 (javax, RAG, Kafka, SSE, 개역개정/ESV/NIV 등)
- 하드코딩 점검 — `main.dart` 카카오 앱 키 `AppConfig`에서 `--dart-define` 기반 로딩 확인
- PR Guard 점검 — 커밋 메시지 OK, 브랜치명 scope 누락 [WARN]
- dev merge 충돌 2건 해결: `main.dart`(카카오 SDK + SharedPreferences 양쪽 유지), `app_router.dart`(async/await + context.mounted + 실제 화면 라우트 병합)

## 미해결
- PR 리뷰 대기
- AndroidManifest.xml 카카오 스킴 하드코딩 — 빌드 타임 요건으로 불가피, 향후 Gradle `manifestPlaceholders` 전환 검토
- 카카오 SDK `loginWithKakao*` 호출 Mock 기반 통합 테스트 — 후속 PR 예정
- 서버 측 `POST /api/v1/auth/kakao` 엔드포인트는 별도 브랜치에서 구현
- iOS 설정 (Info.plist 카카오 스킴) 미적용 — Android 우선
