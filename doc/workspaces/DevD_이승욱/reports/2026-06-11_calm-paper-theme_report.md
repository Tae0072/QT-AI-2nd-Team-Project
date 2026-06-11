# 2026-06-11 Calm Paper 디자인 토큰 적용 — 결과 보고

## 요약
앱 전역 테마를 세이지 그린 웜 파스텔 → **Calm Paper**(웜 오프화이트+잉크 차콜, DESIGN_PROMPT.md)로 전환했다. 색 변경은 `app_theme.dart` 한 파일 원칙(§6)을 지켰고, 유일한 유채색 포인트(#E0492F)는 `accentDot` 신규 토큰으로 분리해 활성 탭 도트에만 쓴다. 화면 코드 변경은 홈 탭바(라인 아이콘+도트)뿐.

## 산출물
| 파일 | 설명 |
|------|------|
| `core/theme/app_theme.dart` | 8토큰 교체 + 컴포넌트 테마(앱바/카드/칩/입력/버튼/탭바/타이포) 재구성 |
| `core/theme/app_dimens.dart` | `AppRadius.box=14`, `AppRadius.sheet=24` 추가 |
| `features/home/screens/home_screen.dart` | 탭바 — filled 전환 제거, 활성 도트(`accentDot`) + 상단 헤어라인 |
| `flutter-app/DESIGN_PROMPT.md` | 디자인 프롬프트 원문 커밋(팀 공용, 마스터 프롬프트 §5) |

## 검증
- `flutter analyze` 무이슈 / `flutter test` 162건 통과
- 시각 확인: 에뮬레이터 5탭+하위 화면 T 컨펌 예정

## 미해결 / 후속
- `bible_browser_screen.dart` 하드코딩 색 토큰 치환 — 담당(이지윤)과 협의 후 별도 PR
- 다크 모드는 미정(현 라이트 전용 유지)

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
