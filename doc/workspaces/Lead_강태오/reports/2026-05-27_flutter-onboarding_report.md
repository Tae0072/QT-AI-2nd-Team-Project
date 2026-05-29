# 2026-05-27 Flutter 온보딩 화면 구현 — 결과 보고

## 요약
앱 최초 설치 시 표시되는 4페이지 온보딩 화면을 구현했다.
PageView 스와이프 + Skip 버튼 + dot indicator 네비게이션을 제공하며,
SharedPreferences로 완료 플래그를 저장해 1회만 표시된다.
차분한 남색/보라 톤 그라데이션 배경에 플레이스홀더 아이콘을 배치했다.

## 산출물

| 파일 | 설명 |
|------|------|
| `flutter-app/lib/features/onboarding/models/onboarding_page_data.dart` | 페이지별 데이터 모델 (4페이지 기본값 포함) |
| `flutter-app/lib/features/onboarding/screens/onboarding_screen.dart` | 온보딩 메인 화면 (PageView + Skip + 버튼) |
| `flutter-app/lib/features/onboarding/widgets/onboarding_page_widget.dart` | 개별 페이지 위젯 (아이콘 + 텍스트) |
| `flutter-app/lib/features/onboarding/widgets/page_indicator.dart` | 하단 dot indicator 위젯 |
| `flutter-app/lib/features/onboarding/providers/onboarding_providers.dart` | SharedPreferences 연동 Riverpod Provider |
| `flutter-app/lib/routes/app_router.dart` | `/onboarding` 경로 추가 |
| `flutter-app/test/features/onboarding/models/onboarding_page_data_test.dart` | 모델 데이터 검증 테스트 (4건) |
| `flutter-app/test/features/onboarding/screens/onboarding_screen_test.dart` | 화면 동작 테스트 (8건) |
| `flutter-app/test/features/onboarding/widgets/page_indicator_test.dart` | indicator 위젯 테스트 (3건) |

## 구현 상세

### 4페이지 구성
1. **매일 큐티와 함께** — 오늘의 말씀과 묵상 본문 소개
2. **AI가 도와주는 해설** — AI 해설 기능 안내
3. **나만의 묵상 노트** — 노트 기록과 공유 안내
4. **지금 시작해볼까요?** — 카카오 로그인으로 시작

### 네비게이션
스와이프와 '다음' 버튼 모두 지원한다. 마지막 페이지에서는 '시작하기' 버튼으로 변경되며,
Skip(건너뛰기) 버튼은 마지막 페이지에서 opacity 0으로 숨겨진다.

### SharedPreferences 연동
`OnboardingNotifier`가 `onboarding_complete` 키를 관리한다.
`sharedPreferencesProvider`는 앱 시작 시 override해야 한다.

### 테스트
15건 전부 통과 — 스와이프, 버튼 전환, Skip, 시작하기 콜백, dot indicator, 모델 데이터.

## 남은 리스크
- 플레이스홀더 아이콘을 디자이너가 실제 일러스트로 교체해야 함
- `main.dart`에서 SharedPreferences provider override 및 온보딩 → 로그인 라우팅 연결은 Auth PR에서 통합 예정
