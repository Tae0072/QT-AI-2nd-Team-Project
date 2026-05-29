# 2026-05-27 온보딩 Flutter 화면 구현

## 목표
앱 최초 설치 시 1회만 표시되는 온보딩 화면을 구현한다. 4페이지 스와이프 구성으로 QT 묵상, AI 해설, 노트/공유, 시작하기를 안내하며, 완료 후 로그인 화면으로 이동한다. SharedPreferences에 완료 플래그를 저장하여 재표시를 방지한다.

## 작업 내용
1. **모델 1파일** — OnboardingPageData(title/description/icon/backgroundColor/iconColor, const defaults 4페이지)
2. **Provider 1파일** — sharedPreferencesProvider(main.dart override 필수), onboardingCompleteProvider(StateNotifier, complete/reset)
3. **화면 1파일** — OnboardingScreen(PageView.builder + PageController, 건너뛰기/다음/시작하기 버튼, 300ms easeInOut 전환)
4. **위젯 2파일** — OnboardingPageWidget(아이콘 영역 + 제목/설명, 남색 그라데이션), PageIndicator(AnimatedContainer dot, 활성 dot 확장 8→24px)
5. **라우터** — /onboarding 추가, 완료 시 /login 교체 이동
6. **의존성** — shared_preferences ^2.5.3 추가
7. **수정** — `withOpacity` deprecated → `withValues(alpha:)` 패턴 전환

## 범위
- 브랜치: `feature/flutter-onboarding`
- 변경 규모: 프로덕션 8파일 + 테스트 3파일 + 문서 2파일 (13건, +749줄)
- PR: #109 (Merged)
- `flutter analyze` No issues found
- 테스트: 3파일 15건 전체 통과 (onboarding_page_data 4, onboarding_screen 8, page_indicator 3)

## 미해결
- 온보딩 일러스트 교체 — 디자이너 합류 후 Material 아이콘 → 커스텀 이미지
- main.dart SharedPreferences override 연결
- 앱 버전 기반 재표시 정책

## 담당
- DevD 이승욱
