# 2026-05-28 온보딩 완료 후 로그인 화면 이동 수정 — 결과 보고

## 요약
온보딩 완료 후 홈 화면으로 직행하던 버그를 수정. `main.dart`를 `ConsumerWidget`으로 변경하고 `onboardingCompleteProvider` 상태 기반 `initialRoute` 분기를 추가했다. 온보딩 완료 시 `/login`으로 이동하도록 수정(강제 로그인 정책). PR 리뷰 2차 BLOCK(테스트 부재) + WARN(home 직행, fire-and-forget 레이스) 반영 완료 후 Merged (#133).

## 산출물

| 파일 | 설명 |
|------|------|
| `flutter-app/lib/main.dart` | `ConsumerWidget` 전환, `initialRoute` 분기 (`/login` or `/onboarding`) |
| `flutter-app/lib/routes/app_router.dart` | `onComplete` async 전환, `await complete()` + `context.mounted` 확인, `/login` 라우트 추가 |
| `flutter-app/test/widget_test.dart` | `SharedPreferences` override 추가, `initialRoute` 조건 분기 테스트 2개 |
| `flutter-app/test/routes/app_router_test.dart` | login 라우트 렌더링 + onComplete→login 네비게이션 통합 테스트 추가 |
| `flutter-app/test/features/onboarding/providers/onboarding_providers_test.dart` | `OnboardingNotifier` 단위 테스트 7개 케이스 (초기값, complete, reset, 왕복, Riverpod 통합) |

## 검증
- `flutter analyze` — No issues found
- `flutter test` — 전체 통과 (기존 + 신규 테스트 11개 추가)
- 금지 기술/기능 — 위반 없음
- PR Guard 점검 — 브랜치명·커밋 메시지·코드 컨벤션 통과

## 리뷰 이력
- **1차 리뷰**: CI 실패 (widget_test.dart `SharedPreferences` override 누락) → 수정 후 force push
- **2차 리뷰**: BLOCK(테스트 부재) + WARN(home 직행 → 강제 로그인 충돌) + WARN(complete() fire-and-forget 레이스) → 코드 수정 + 테스트 보강 후 force push
- Merged (#133)

## 미해결
- `/login` 라우트는 placeholder → `feature/kakao-login`에서 실제 카카오 로그인 화면으로 교체 완료
