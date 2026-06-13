# 2026-06-14 설정 화면 테마 3택(라이트/다크/시스템)

## 배경(사용자 요청)
- 설정의 다크 모드를 라이트/다크/시스템 3가지 중 선택하게 해달라.
  - 라이트=지금처럼 다크 끔, 다크=다크, 시스템=기기 설정 따름(기기가 라이트면 라이트, 다크면 다크).

## 구현
- `core/theme/theme_providers.dart`: 저장값을 bool(`dark_mode`)에서 문자열(`theme_mode`: light/dark/system)로 변경. `setMode(ThemeMode)` 추가. 구버전 bool 값은 1회 마이그레이션(true→다크, false→라이트). 저장값 없으면 기본 **시스템 따름**.
- `MaterialApp.themeMode`는 이미 `themeModeProvider`를 그대로 사용 → `ThemeMode.system`이면 기기 설정을 자동 추종(main.dart 변경 불필요).
- `features/mypage/screens/settings_screen.dart`: 스위치(on/off)를 제거하고, 현재 모드를 보여주는 ListTile + 바텀시트(라이트/다크/시스템 라디오 3택)로 변경.
- l10n: `settingsThemeMode`/`settingsThemeLight`/`settingsThemeDark`/`settingsThemeSystem`/`settingsThemeSystemDesc` 추가(ko/en) + 생성물 재생성.

## 검증
- `flutter test` → **301개 전부 통과**(테마 provider 테스트를 3모드·마이그레이션 기준으로 갱신).

## 비고
- 기존 정책 주석("시스템 설정 비추종, 토글이 단일 진실")은 시스템 모드 지원으로 대체(Lead 결정).
- 신규 설치 기본값을 라이트 대신 **시스템**으로 두어 기기 설정을 존중. 기존 사용자(저장된 dark_mode)는 그대로 유지.

## Git/PR
- 브랜치 `feature/settings-theme-mode-three-options` → PR 대상 `dev`.
