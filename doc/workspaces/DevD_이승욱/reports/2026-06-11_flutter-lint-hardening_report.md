# 2026-06-11 flutter 린트 강화 — 결과 보고

## 요약
코드리뷰 TODO 3(P2) 완료. 예방 린트 3종(`avoid_print`/`use_build_context_synchronously`/`unawaited_futures`)을 추가하고, 새로 드러난 경고 9건(전부 unawaited_futures)을 의도 명시(`unawaited()`)로 정리했다. 동작 변경 없음.

## 산출물
| 파일 | 설명 |
|------|------|
| `flutter-app/analysis_options.yaml` | 규칙 3종 + 사유 주석 |
| `music_providers.dart` | `_persist` 4곳 `unawaited()` — 서버 저장 fire-and-forget 의도 명시 |
| `login_screen.dart` / `profile_edit_screen.dart` / `app_router.dart` | Navigator push류 4곳 `unawaited()` |
| `test/routes/app_router_test.dart` | 테스트 내 pushNamed 1곳 `unawaited()` |

## 검증
- `flutter analyze` 무이슈 / `flutter test` 141건 전체 통과 (동작 변경 없는 의도 명시만)

## 미해결 / 후속
- 없음

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
