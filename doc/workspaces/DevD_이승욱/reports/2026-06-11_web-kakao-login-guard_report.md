# 2026-06-11 flutter 웹 카카오 로그인 사전 가드 — 결과 보고

## 요약
코드리뷰 TODO 2(P2) 완료. 웹에서 카카오 로그인 버튼을 누르면 SDK 예외가 그대로 터지던 문제를, 판정 순수 함수 + 버튼 비활성 + l10n 안내로 가드했다. dev 웹 우회 경로는 영향 없음.

## 산출물
| 파일 | 설명 |
|------|------|
| `auth/services/kakao_login_guard.dart` | 판정 순수 함수(kIsWeb 주입형 — 단위 테스트 가능) |
| `auth/screens/login_screen.dart` | 웹이면 버튼 비활성 + 안내 표시 |
| `l10n/app_{ko,en}.arb` + 생성 파일 3종 | `loginWebNotSupported` 키 |
| `test/features/auth/kakao_login_guard_test.dart` | 4케이스 판정 고정 |

## 검증
- `flutter analyze` 무이슈 / `flutter test` 144건 통과(신규 3건 포함)

## 미해결 / 후속
- 웹 카카오 정식 지원은 서버측 OAuth 필요 — §5 결정과 충돌하는 미해결 설계 이슈(안내 가드까지만)
- 머지 순서: `chore/mobile-lint-hardening`(스택 베이스) 머지 후 본 PR 머지

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
