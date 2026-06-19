# 앱 기본 테마 라이트 모드 고정 — 결과 정리

- 작성자: Lead 강태오 (with Claude)
- 날짜: 2026-06-19
- 관련 워크플로우: `workflows/2026-06-19_theme-default-light.md`
- 브랜치: `feature/theme-default-light` → PR to `dev`
- 관련 화면/F-ID: 설정 M-06(마이페이지 F-13 영역). 테마 모드 기본값은 별도 F-ID 미명세 UX 기본값.

## 1. 결과 요약

앱 최초 실행 시 기본 테마를 `system` → `light`로 고정했다. 사용자가 직접 고른 값(dark/system)은 그대로 유지된다.

| 상황 | 변경 전 | 변경 후 |
|---|---|---|
| 최초 실행(저장값 없음) | 기기 설정 따름(system) | 라이트 |
| 구버전 dark_mode=false | 라이트 | 라이트(동일) |
| 구버전 dark_mode=true | 다크 | 다크(동일) |
| 직접 고른 light/dark/system | 유지 | 유지(동일) |
| 알 수 없는 저장값 | 시스템 | 라이트 |

## 2. 변경 파일

| 파일 | 내용 |
|---|---|
| `flutter-app/lib/core/theme/theme_providers.dart` | `_initial` 기본값 light, `_parse`에 'system' 명시 매칭 + 알 수 없는 값 light fallback, 주석 보강 |
| `flutter-app/test/core/theme/theme_providers_test.dart` | 기본값 테스트 light로 수정 + 신규 3건(구버전 false, 직접 system 복원, 알 수 없는 값) |

## 3. 검증 (2회 이상 자가)

1. 단위 테스트: `flutter test test/core/theme/theme_providers_test.dart` → 7/7 통과.
2. 정적 분석: `flutter analyze`(변경 2파일) → No issues found.
3. 코드 재검토: `main.dart` 등 다른 곳에 `ThemeMode` 하드코딩 기본값 없음 확인(provider 단일 소비). 직접 고른 '시스템' 선택이 깨지지 않도록 `_parse`에 system 케이스를 추가.

## 4. 영향/후속

- 이 변경은 테마 파일 단독 수정이라, 노트/알림 등 후속 작업이 안전하게 이 위에서 진행 가능하다.
- 기존 사용자 중 테마 미선택자(system 기본)는 다음 실행부터 라이트로 시작한다(요구 사항과 일치).
- 작업 트리의 다른 WIP(note/sharing/admin-web)는 건드리지 않았다. 사용자 SelfTestPage WIP는 stash로 보관했다.
