# 2026-06-04 웜 파스텔 테마 적용 — 결과 보고

## 요약
디자인 시안(웜 파스텔 Calm)을 Flutter 앱에 적용. AppTheme 토큰 + 스플래시 + 로그인 디자인 교체 + 5탭 탭바.

## 산출물

| 파일 | 설명 |
|------|------|
| `app_theme.dart` | ThemeData 토큰 (색상/폰트/카드/버튼/탭바) |
| `main.dart` | AppTheme 적용 + 스플래시 디자인 |
| `login_screen.dart` | 웜 파스텔 디자인 전면 교체 |
| `home_screen.dart` | 5탭 탭바 (오늘/성경/나눔/노트/마이) |
| `pubspec.yaml` | google_fonts 의존성 추가 |

## 검증
- `flutter analyze` — No issues found
- `flutter test` — 99건 통과 (login_screen_test, app_router_test 텍스트 변경 반영 포함)

## 설계 근거
- **5탭 IA**: 06_화면정의서 §2 메인 탭 구조 (오늘의QT / 성경 / 나눔 / 노트 / 마이페이지)
- **폰트 로딩**: `google_fonts` 패키지 runtime fetch 방식. 오프라인 fallback은 기본 시스템 폰트 적용.

## 미해결
- PR 머지 대기
- dev 병합 시 성경 탭 충돌 해결 완료 (BibleBrowserScreen 채택)
