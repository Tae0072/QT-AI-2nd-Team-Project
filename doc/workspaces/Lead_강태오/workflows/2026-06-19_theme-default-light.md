# 앱 기본 테마 라이트 모드 고정

- 날짜: 2026-06-19
- 작성: Lead 강태오 (with Claude)
- 브랜치: `feature/theme-default-light` → PR to `dev`
- 관련 화면/F-ID: 설정 화면 M-06(마이페이지 F-13 영역). 테마 모드 기본값은 별도 기능 F-ID로 명세되지 않은 앱 셸 UX 기본값이며, 선행 테마 커밋(#602 `feat(settings)`, #622 `fix(theme)`)도 F-ID를 표기하지 않았다.

## 1. 배경 — 왜

- 기존 기본 `ThemeMode`가 `system`이라, 테마를 한 번도 고르지 않은 최초 실행 사용자는 기기 설정을 따라가 기기가 다크면 다크로 시작됐다.
- 요구: 최초 실행 기본값을 라이트로 고정한다. 단, 사용자가 직접 고른 값(다크/시스템)은 그대로 존중한다.
- 이 테마 파일은 노트/알림 등 다른 작업이 참조하므로 가장 먼저 끝내 dev에 반영한다.

## 2. 작업

대상 파일(2개):
- `flutter-app/lib/core/theme/theme_providers.dart` — 기본값 로직
- `flutter-app/test/core/theme/theme_providers_test.dart` — 테스트

### theme_providers.dart 변경
- `_initial()`: 저장값(`theme_mode`)·구버전(`dark_mode`)이 모두 없을 때 반환을 `ThemeMode.system` → `ThemeMode.light`로 변경.
- `_parse()`: 기존 `_`(기본 케이스)가 저장된 `'system'`까지 처리하던 것을, `'system'`을 명시 매칭으로 분리하고 알 수 없는 값만 `ThemeMode.light`로 떨어지게 변경.
  - 이유: 이렇게 하지 않으면 사용자가 직접 고른 '시스템'이 라이트로 깨진다.
- 문서 주석에 "최초 실행 기본값 = 라이트"를 명시.

### 저장값 해석 우선순위(변경 후)
1. `theme_mode` 저장값이 있으면 그대로(light/dark/system) — 사용자 선택 존중
2. 없고 구버전 `dark_mode`가 있으면 true→dark / false→light
3. 둘 다 없으면(최초 실행) → **light (신규 기본값)**

`main.dart`는 `themeModeProvider` 값을 그대로 소비하므로 변경 불필요(별도 하드코딩 기본값 없음 확인).

## 3. 검증
- `flutter test test/core/theme/theme_providers_test.dart` → 7개 전부 통과(기본 light, 명시 system 복원, 구버전 true/false, 알 수 없는 값 fallback 포함).
- `flutter analyze`(변경 2파일) → No issues found.

## 4. 작업 트리 주의
- 작업 시작 시 메인 워크트리에 무관한 다른 WIP(note/sharing/admin-web)가 떠 있어, 테마 2개 파일만 명시적으로 `git add`/commit 한다. 다른 변경은 건드리지 않는다.
- 사용자 `admin-web/src/pages/SelfTestPage.tsx` 미커밋 변경은 작업 전 stash로 보관(stash 목록 최상단).

## 5. 커밋/PR
- 커밋: `fix(theme): 앱 기본 테마를 라이트로 고정 (최초 실행 system→light)`
- PR 대상: `dev`
