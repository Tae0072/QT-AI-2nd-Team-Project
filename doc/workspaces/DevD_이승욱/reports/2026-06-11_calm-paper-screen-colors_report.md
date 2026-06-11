# 2026-06-11 Calm Paper 화면 색 정합 + 마이페이지 재구성 — 결과 보고

## 요약
Calm Paper 테마는 dev에 있었으나 화면들이 청록·파랑·보라를 그대로 노출하던 문제를 잡았다. 핵심은 하드코딩 hex가 아니라 **Material3가 near-neutral 시드(#1F1F1F)에서 `colorScheme.primary`·`primaryContainer`를 청록빛으로 파생**시킨 것 — 테마에서 해당 role을 잉크/무채색 토큰으로 고정해 절번호·통계 아이콘·달력·아바타를 일괄 무채색화했다. 더불어 마이페이지를 흰 카드 → DESIGN_PROTOTYPE.md식 회색 그룹박스 + 프로필 행으로 재구성하고, 후속 화면 재사용을 위한 공통 컴포넌트(`calm_paper.dart`)를 추가했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `core/theme/app_theme.dart` | `colorSchemeSeed` → 명시 `colorScheme`. primary·secondary·tertiary·container를 잉크/무채색 토큰으로 고정 |
| `core/widgets/calm_paper.dart` (신규) | 공통 컴포넌트: TitleBlock/SectionTitle/Group/Row/Badge/SubBox (sunken 그룹박스 + 헤어라인) |
| `features/mypage/screens/mypage_screen.dart` | 흰 카드 → 프로필 행(통계 부제 접기) + 그룹박스 메뉴 (s-my 대응) |
| `features/note/widgets/meditation_calendar.dart` | CalendarStyle 무채색화 — 저장일 마커만 `accentDot` |
| `features/note/screens/note_list_screen.dart` | DRAFT 라벨 `Colors.orange` → `outline` |
| `features/mypage/widgets/profile_card.dart` | 아바타 배경/아이콘 토큰화 |

## 검증
- `flutter analyze` 무이슈 / `flutter test` 169건 전체 통과
- 시각 확인: 에뮬레이터(Android API 37) — 오늘 QT 절번호 무채색, 노트 달력 회색 today·빨간 마커, 마이 그룹박스 (스크린샷 확인 완료)

## 미해결 / 후속
- 오늘 QT(주제 제목·절별 해설 sub-box)는 백엔드 데이터 필요, 노트·나눔 레이아웃 전면 재구성은 별도 PR(본인 담당 영역, 순차 진행).
- 성경 본문 탭은 범위 제외(담당: 이지윤).
- 마이페이지에서 빠진 ProfileCard/StatsCard/QuickMenuCard 위젯은 위젯 테스트가 남아 있어 파일은 유지(미사용). 후속 정리 가능.

담당: DevD 이승욱
