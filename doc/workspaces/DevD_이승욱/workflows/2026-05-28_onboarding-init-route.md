# 2026-05-28 온보딩 완료 후 로그인 화면 이동 수정

## 목표
온보딩 완료 후 홈 화면으로 직행하던 버그를 수정하여, 강제 로그인 정책에 맞게 온보딩 → 로그인 → 홈 흐름을 구현한다.

## 작업 내용
1. **main.dart ConsumerWidget 전환** — `StatelessWidget` → `ConsumerWidget`으로 변경, `onboardingCompleteProvider` 상태 기반 `initialRoute` 분기 추가
2. **initialRoute 수정** — `onboardingComplete` 시 `/home` → `/login`으로 변경 (강제 로그인 정책)
3. **onComplete async 전환** — `app_router.dart`에서 `onComplete` 콜백을 `async`로 변경, `await complete()` 후 `context.mounted` 확인 후 `pushReplacementNamed(login)` (fire-and-forget 레이스 방지)
4. **login placeholder 라우트 추가** — `AppRouter`에 `/login` 라우트 추가
5. **테스트 보강 (리뷰 2차 반영)**:
   - `OnboardingNotifier` 단위 테스트 신규 작성 (complete/reset/초기값 분기/Riverpod 통합, 7개 케이스)
   - `widget_test.dart`에 initialRoute 조건 분기 테스트 2개 추가 (미완료→`/onboarding`, 완료→`/login`)
   - `app_router_test.dart`에 login 라우트 렌더링 + onComplete→login 네비게이션 통합 테스트 추가
6. **CI 실패 수정** — `widget_test.dart`에 `SharedPreferences.setMockInitialValues({})` + `sharedPreferencesProvider.overrideWithValue(prefs)` 추가

## 리뷰 이력
- **1차 리뷰**: CI 실패 (Flutter Analyze & Test) — widget_test.dart SharedPreferences override 누락 → 수정 후 force push
- **2차 리뷰**: BLOCK(테스트 부재) + WARN(home 직행 → 강제 로그인 충돌) + WARN(complete() fire-and-forget 레이스) → 코드 수정 + 테스트 보강 후 force push

## 범위
- 브랜치: `bugfix/onboarding-init-route`
- PR: #133 (Merged)
- 커밋: `fix(onboarding): 온보딩 완료 후 로그인 화면으로 이동하도록 수정`
- 변경: 5파일 (`main.dart`, `app_router.dart`, `widget_test.dart`, `app_router_test.dart`, `onboarding_providers_test.dart`)
- 관련: F-01 카카오 소셜 로그인

## 미해결
- `/login` 라우트는 placeholder — `feature/kakao-login`에서 실제 카카오 로그인 화면으로 교체 완료

## 담당
- DevD 이승욱
