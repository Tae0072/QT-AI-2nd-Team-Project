# 2026-06-11 뒤로가기 2번으로 앱 종료 — 결과 보고

## 요약
홈(루트) 뒤로가기를 1회 → 안내, 2초 내 2회 → 종료로 변경했다(실수 종료 방지, 안드로이드 관례). 판정은 순수 함수로 분리해 단위 테스트로 고정했고, 상세 화면들의 뒤로가기는 영향 없다.

## 산출물
| 파일 | 설명 |
|------|------|
| `home/services/double_back_exit_policy.dart` | 종료 판정 순수 함수(창 2초, 경계 포함) |
| `home/screens/home_screen.dart` | `PopScope` + 안내 스낵바 + `SystemNavigator.pop()` |
| `l10n` 5파일 | `homeBackExitGuide` ko/en |
| `test/features/home/double_back_exit_policy_test.dart` | 3건(첫 입력/창 내·경계/창 초과) |

## 검증
- `flutter analyze` 무이슈 / `flutter test` 159건 통과

## 미해결 / 후속
- 없음

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
