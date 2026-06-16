# 2026-06-11 Calm Paper 화면 색 정합 + 마이페이지 재구성 (feature/mobile-calm-paper-screens-redesign)

## 목표·배경
Calm Paper 테마(`app_theme.dart`)는 dev에 반영됐으나(#515), 개별 화면이 옛 컬러풀 테마의 파생/하드코딩 색을 그대로 써서 에뮬레이터에서 청록·파랑·보라가 노출됨(절번호·통계 아이콘·달력 하이라이트·아바타 등). 또 마이페이지는 흰 Material 카드 구조라 DESIGN_PROTOTYPE.md(Calm Paper)의 회색 그룹박스 톤과 어긋남. → 화면 색 정합 + 마이페이지 1차 재구성.

## 원인
- 화면들이 강조색으로 `theme.colorScheme.primary` / `primaryContainer`를 사용. Calm Paper가 시드를 잉크(#1F1F1F)로 주는데 **Material3가 near-neutral 시드에서 primary·container를 청록빛으로 파생** → 절번호·통계 아이콘·달력 헤더·아바타가 청록으로 표시됨.
- 노트 달력(`table_calendar`)은 `calendarStyle` 미지정 → 기본 보라(selected)·파랑(today).

## 작업 내용
- `app_theme.dart`: `colorSchemeSeed` → 명시 `colorScheme`(`fromSeed(...).copyWith(...)`)로 전환. primary·secondary·tertiary 및 각 container를 잉크/무채색 토큰으로 고정 → **화면 코드 변경 없이 colorScheme 기반 색을 일괄 무채색화**.
- `core/widgets/calm_paper.dart` 신규: DESIGN_PROTOTYPE.md 공통 컴포넌트 — `CpTitleBlock`/`CpSectionTitle`/`CpGroup`/`CpRow`/`CpBadge`/`CpSubBox`. 흰 카드 대신 sunken 그룹박스 + 헤어라인 구분선. 후속 화면 재구성에 재사용.
- `mypage_screen.dart`: ProfileCard/StatsCard/QuickMenuCard(흰 카드) → 프로필 행(아바타 sunken + 닉네임 + 묵상 통계 부제 접기) + `CpGroup` 메뉴(알림·찬양·설정). 이동·뱃지·곡수 기능 유지(DESIGN_PROTOTYPE s-my 대응).
- `meditation_calendar.dart`: `CalendarStyle` 추가 — today=회색 원, selected=잉크 원, 저장일 marker=`accentDot`(유일 유채색), 요일/외부일 무채색.
- `note_list_screen.dart`: DRAFT 라벨 `Colors.orange` → `colorScheme.outline`(카테고리 라벨 청록은 primary 고정으로 자동 해소).
- `profile_card.dart`: 아바타 배경/아이콘을 토큰(`bgSunken`/`text2`)으로 명시.

## 범위 제외(후속 PR)
- 오늘 QT 주제 제목·절별 영어/해설 sub-box, 노트·나눔 레이아웃 전면 재구성 — 백엔드 데이터(주제 제목·절별 해설) 의존 또는 화면 전면 재작성이라 별도 PR. (note/sharing/praise는 본인=DevD 담당 영역)
- 성경 본문 탭은 이번 범위 제외(담당: 이지윤).

## 검증
- `flutter analyze` 무이슈, `flutter test` 169건 전체 통과(색 단언 테스트 없음, 기존 위젯 테스트 영향 없음).
- 수동: 에뮬레이터(Android API 37)에서 오늘 QT(절번호 무채색)·노트(달력 회색 today/빨간 마커)·마이(그룹박스) 스크린샷 확인.

담당: DevD 이승욱
