# 2026-06-11 flutter 웹 카카오 로그인 사전 가드 (bugfix/mobile-web-kakao-login-guard)

## 목표·배경
코드리뷰 TODO 2 (P2): 카카오 dart SDK는 웹 미지원인데 로그인 화면에 `kIsWeb` 분기가 없어, 웹 사용자가 버튼을 누르면 SDK 예외가 그대로 발생.

## 작업 내용
- 판정을 순수 함수로 분리: `auth/services/kakao_login_guard.dart` — `isKakaoLoginUnsupported(isWeb, webDevBypassEnabled)`. `kIsWeb`은 테스트 분기 제어가 어려워 주입형으로 설계.
- 로그인 화면: 웹이면 카카오 버튼 비활성 + 안내 문구 표시. dev 웹 우회(`webDevNoLogin` 삼중 게이트) 경로는 기존 동작 유지.
- l10n 키 `loginWebNotSupported`(ko/en) 추가 — 하드코딩 없음.
- 웹 카카오 정식 지원은 서버측 OAuth 필요로 기존 결정(§5 `/oauth2/**` 미사용)과 충돌하는 미해결 설계 이슈 — 이번 작업은 안내 가드까지만(TODO 문서 합의 범위).

## 범위
- 브랜치: `bugfix/mobile-web-kakao-login-guard` — **`chore/mobile-lint-hardening`에서 스택 분기**(login_screen.dart 충돌 방지). 린트 PR 머지 후 본 PR 머지.

## 검증
- 가드 단위 테스트 3건(웹/웹+우회/모바일 조합 4케이스), `flutter analyze` 무이슈, `flutter test` 144건 전체 통과.
- 수동: 웹 dev 실행(우회 끔)으로 버튼 비활성+안내 확인 예정.

## 미해결 / 후속
- 웹 카카오 로그인 정식 지원 여부는 별도 설계 결정 필요(서버측 OAuth와 §5 충돌).

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
