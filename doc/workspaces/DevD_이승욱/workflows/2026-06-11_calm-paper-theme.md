# 2026-06-11 Calm Paper 디자인 토큰 적용 (feature/mobile-calm-paper-theme)

## 목표·배경
T가 확정한 "Calm Paper" 디자인(DESIGN_PROMPT.md — 웜 오프화이트 + 잉크 차콜, Apple풍 미니멀)을 앱 전역에 적용. 기존은 세이지 그린 웜 파스텔 테마.

## 작업 내용
- `app_theme.dart` 전면 교체(§6 매핑): bg #F7F5F2 / 카드·칩·시트 #FFFFFF / 보조박스 #F0EEEA / 텍스트 #1F1F1F·#8A8A8E·#B8B5B0 / 헤어라인 #E8E6E2. hero·page 토큰도 무채색으로 통일(어두운 블록 금지).
- **포인트 색 정책**: `accent`(버튼·선택·포커스)는 잉크 차콜로 — "8색 외 유채색 금지·포인트는 화면당 1곳 이하" 규칙상 일반 강조가 빨강이면 위반. 빨강 #E0492F는 신규 `accentDot` 토큰으로 분리해 **활성 탭 도트 전용**.
- 컴포넌트 테마(§3): 앱바 17 w600 중앙, 카드 흰 면+헤어라인+반경 14+그림자 제거, 칩 stadium(선택=흰/비선택=투명), 입력 반경 14, 주 버튼 잉크 필, FAB elevation 0, textTheme를 §2 타이포 스케일(26 w700 / 16·1.65 / 14·1.55 / 13 / 12)로 재구성.
- `app_dimens.dart`: `AppRadius.box=14`(보조박스)·`AppRadius.sheet=24`(플로팅 시트) 추가.
- `home_screen.dart` 탭바: filled 아이콘 전환 제거(§3 금지) — 라인 아이콘 유지 + 활성 탭 위 빨간 도트(`accentDot`), 상단 헤어라인 1px.
- `DESIGN_PROMPT.md`를 flutter-app/에 커밋(팀 공용 — 화면 작업 시 마스터 프롬프트 §5 사용).

## 추가: 다크 모드 (T 요청, 2026-06-11)
- 다크 토큰 세트 "잉크 위의 종이" — 웜 다크 차콜 배경(#1B1A18/#242220/#21201D) + 종이색 텍스트(#F0EEEA/#A5A29C/#6E6B66), 헤어라인 #383531. `accentDot`(#E0492F)은 양 모드 동일.
- 색 토큰을 `AppColors` **ThemeExtension**으로 승격 — 라이트/다크 인스턴스를 ThemeData에 등록하고, 화면은 `context.appColors`로 현재 모드의 색을 받는다(테마 미지정 컨텍스트는 라이트 fallback — 테스트 NPE 방지).
- ThemeData는 `_build(토큰, brightness)` 공용 빌더 하나로 생성 — 컴포넌트 규격은 두 모드 동일, 색만 교체.
- `main.dart`: 두 MaterialApp에 `darkTheme` 등록. ~~`themeMode: system`~~ → **설정 화면 토글만 따른다**(3차 커밋, T 결정): `themeModeProvider`(SharedPreferences `dark_mode` 로컬 저장, 기본 라이트)를 watch — 시스템 다크 설정 비추종. 마이페이지 설정 화면에 "다크 모드" SwitchListTile 추가(l10n ko/en). 스플래시도 Builder로 다크 대응.
- 정적 토큰 직접 참조 화면(login/home/main 스플래시)을 `context.appColors`로 전환. 로고 가운뎃점은 `accentDot`(포인트 토큰 일원화), 약관 링크는 무채색+밑줄로 변경(유채색 1곳 이하 규칙).
- 기존 `AppTheme.x` 정적 상수는 라이트 값으로 유지(타 화면 호환) — 신규 코드는 `context.appColors` 사용을 표준으로.

## 범위 제외(후속 협의)
- `bible_browser_screen.dart`의 화면 내 하드코딩 색(블루그레이 계열 다수) — 담당(이지윤) 화면이라 본 PR에서 미수정(관련 없는 리팩터링 금지, §9). 새 팔레트 정착 후 담당자와 토큰 치환 협의.
- 카카오 로그인 버튼 노란색(#FEE500)은 브랜드 가이드 예외로 유지.

## 검증
- `flutter analyze` 무이슈, `flutter test` 162건 전체 통과(테스트는 색 단언 없음 확인).
- 수동: 에뮬레이터에서 5탭 + 주요 하위 화면 시각 확인 예정(T 컨펌).

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
