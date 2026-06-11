# 2026-06-11 뒤로가기 2번으로 앱 종료 (feature/mobile-double-back-exit)

## 목표·배경
홈(루트)에서 뒤로가기 한 번에 앱이 바로 종료되는 문제 — 실수 종료 방지를 위해 안드로이드 관례(첫 입력 안내 → 2초 내 재입력 시 종료)로 변경 요청.

## 작업 내용
- `DoubleBackExitPolicy`(home/services): 종료 판정 순수 함수 — 창 2초, 경계 포함. 시간 판정을 분리해 위젯 없이 단위 테스트.
- `HomeScreen`: `PopScope(canPop: false, onPopInvokedWithResult: ...)` — 첫 입력은 플로팅 스낵바 안내(노출 시간 = 판정 창 2초로 일치), 창 내 재입력은 `SystemNavigator.pop()`으로 태스크 종료. 홈 위에 push된 상세 화면들은 자기 라우트가 pop을 처리하므로 영향 없음.
- l10n `homeBackExitGuide`(ko/en) — 하드코딩 없음.

## 검증
- 정책 단위 테스트 3건(첫 입력/창 내·경계/창 초과), `flutter analyze` 무이슈, `flutter test` 159건 전체 통과.
- 수동: 에뮬레이터에서 홈 뒤로가기 1회(안내)·2회(종료), 상세 화면 뒤로가기는 기존대로 pop 확인 예정.

## 미해결 / 후속
- 없음. (탭 이동 시 뒤로가기로 첫 탭 복귀 같은 확장은 별도 결정 필요 — 이번 범위는 종료 동작만)

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
