# 2026-05-26 인증 백엔드 및 Flutter 연동 — 결과 보고

## 요약
카카오 OAuth 로그인 → JWT RS256 발급/갱신(rotation)/무효화 전체 흐름을 서버에 구현하고, Flutter 앱에서 카카오 SDK → 서버 인증 → 토큰 관리 → 자동 재인증까지 연동했다.
PR Guard Round 3 완료. Mockito doAnswer 패턴 전환으로 동시 가입 경합 테스트 NPE 해결.

## 산출물

| 파일 | 설명 |
|------|------|
| `AuthController.java` | POST /auth/kakao, /auth/token/refresh, /auth/logout |
| `AuthService.java` | 카카오 로그인(자동가입), 토큰 갱신(rotation), 로그아웃 |
| `KakaoOAuthClient.java` | 카카오 API 검증 (1회성 사용 후 폐기) |
| `RefreshTokenStore.java` | Redis 기반 refresh token 저장 (TTL 14일) |
| `MemberAuthProvider.java` | Entity + V6 DDL |
| `SecurityConfig.java` | Stateless + JWT 필터, permitAll 경로 설정 |
| `Member.java` | 프로필 수정/탈퇴/닉네임 잠금 구현 완료 |
| Flutter `auth/` | KakaoAuthService, AuthRepository, AuthState(sealed), AuthProvider(StateNotifier), SplashScreen, LoginScreen |
| Flutter `auth_interceptor.dart` | 401 자동 갱신 (single-flight) |

## 검증
- `gradlew compileJava` — BUILD SUCCESSFUL
- `gradlew test` — 41건 전체 통과 (AuthServiceTest 15, AuthControllerTest 8, MemberServiceTest 10, MemberControllerTest 8)
- `flutter analyze` — 에러 0
- 도메인 경계 위반 — 없음
- 금지 기술/데이터 — 위반 없음

## 미해결
- admin_role 세부 권한은 admin 도메인 구현 후 추가 예정
