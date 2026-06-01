# 2026-05-29 Flutter 개발 환경 설정 + 카카오 로그인 연동 수정 — 결과 보고

## 요약
Flutter 개발 환경에서 카카오 앱 키 미주입으로 KOE101 에러 발생하던 문제와, 카카오 로그인 → 서버 JWT 교환 시 API 경로 중복·필드명 불일치·응답 파싱 오류 3건을 해결했다.

## 산출물

| 파일 | 설명 |
|------|------|
| `flutter-app/.vscode/launch.json` | `--dart-define-from-file=.env` 자동 적용 (팀원 공유) |
| `flutter-app/lib/core/config/app_config.dart` | KAKAO_NATIVE_APP_KEY 기본값 추가 (dev 환경) |
| `flutter-app/lib/features/auth/services/auth_repository.dart` | API 경로 `/auth/kakao`, 필드명 `kakaoAccessToken`, 응답 `member.onboardingRequired` 수정 |
| `flutter-app/lib/features/auth/screens/nickname_setup_screen.dart` | 닉네임 설정 경로 `/me/nickname` 수정 |
| `.gitignore` | `.vscode/launch.json` 예외 추가 |

## 검증
- 에뮬레이터에서 카카오 로그인 → 서버 JWT 교환 → 닉네임 설정 화면 진입 확인
- `flutter analyze` — No issues found
- 금지 기술/데이터 — 위반 없음

## 미해결
- PR 머지 대기
