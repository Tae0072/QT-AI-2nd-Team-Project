# 2026-06-11 카카오 네이티브 키 기본값 제거 (chore/mobile-kakao-key-default)

## 목표·배경
코드리뷰 TODO 4 (P3): `app_config.dart`에 dev 네이티브 앱 키가 기본값으로 커밋돼 있음. APK에 포함되는 공개 식별자라 실위험은 낮지만, CLAUDE.md §8 "plain secret 예시 금지" 준수를 위해 빈 기본값 + 주입 강제로 전환.

## 작업 내용
- `app_config.dart`: `defaultValue: '53e5…'` → `''` (주석에 주입 방법·빈 키 동작 명시)
- 빈 키 동작은 **기존 구현 그대로 활용**: dev는 SDK 초기화 스킵 + 경고 로그(카카오 로그인만 비활성), prod/staging은 기동 실패(빠른 실패)
- `flutter-app/README.md` 전면 갱신: 키 주입 실행법, 자주 쓰는 dart-define 표, 검증 명령. 키 값 자체는 문서에 남기지 않음(팀 채널로 공유)
- `run-dev-web.ps1`은 수정 안 함 — 웹은 카카오 dart SDK 미지원이라 키 불필요(README에 명시)
- 저장소 전체 grep으로 키 리터럴 잔존 없음 확인
- **팀 공지 발송됨**(2026-06-11): 머지 후 로컬 실행 시 `--dart-define=KAKAO_NATIVE_APP_KEY=...` 필요

## 검증
- `flutter analyze` 무이슈, `flutter test` 156건 전체 통과
- 수동: 키 없이 dev 실행 → 경고 로그 + 앱 정상 동작(로그인만 비활성) / 키 주입 시 로그인 동작 — 머지 전 확인 예정

## 미해결 / 후속
- 없음 (코드리뷰 TODO 1~4 전체 완료, TODO 5는 proxy 반영 확인만 잔여)

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
