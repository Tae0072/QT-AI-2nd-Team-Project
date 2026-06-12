# 2026-06-12 폰트 크기 설정 적용 + 성경 본문 영어 영역 정합 (feature/mobile-font-size-and-bible-english)

## 목표·배경
T 요청 2건. ① 마이페이지 설정의 "폰트 크기"(SMALL/MEDIUM/LARGE)를 바꿔도 화면 글자 크기가 안 바뀐다. ② 성경 본문 보기 페이지의 영어 본문 영역이 오늘의 QT 영어 영역(sunken sub-box)처럼 보이게 한다.

## 작업 내용
### ① 폰트 크기 적용 (실제로 안 먹던 버그)
- 원인: 설정 드롭다운이 값을 백엔드(`PATCH /me/settings`)에 저장만 하고, 그 값을 화면 글자 크기에 적용하는 코드가 전혀 없었다(다크 모드는 `themeModeProvider`로 로컬 적용되지만 폰트는 적용 provider 부재).
- `core/theme/font_scale_provider.dart` 신설: `fontSizeProvider`(StateNotifier<String>, SharedPreferences `font_size`, 다크 모드와 동일한 로컬 저장 패턴) + `fontScaleOf(코드)` 매핑(SMALL 0.9 / MEDIUM 1.0 / LARGE 1.2, 레이아웃 깨짐 방지용 보수 범위). 허용값 외에는 MEDIUM으로 보정.
- `main.dart`: 메인 `MaterialApp`에 `builder`를 추가해 `MediaQuery.textScaler = TextScaler.linear(fontScale)`로 앱 전역에 적용. OS 글자 크기는 추종하지 않고(다크 모드와 같은 철학) 앱 설정이 단일 진실.
- `settings_screen.dart`: 드롭다운 렌더 값을 로컬 `fontSizeProvider`로(즉시 반영), onChanged는 ①로컬 set ②백엔드 저장 순. `ref.listen(settingsProvider)`로 서버 값 도착 시 로컬에 동기화(다른 기기 변경 반영, set()이 동일값 무시라 루프 없음).

### ② 성경 본문 영어 영역 → QT처럼 sub-box
- `bible_passage_screen.dart` `_PassageVerseTile`: 영어 본문을 평범한 muted `Text` → 오늘의 QT `_VerseTile`과 동일한 `CpSubBox`(margin top 10, GowunDodum 14, height 1.55, color text2)로 변경. `calm_paper.dart` import 추가.

## 범위/주의
- 성경 화면은 이지윤님 담당 도메인. T 지시로 본인이 구현, PR에서 이지윤님 리뷰 필요(도메인 경계 존중).
- 폰트 크기는 서버 설정(`settings.fontSize`)과 로컬을 양방향 동기화 — 서버 계약 변경 없음.
- 작업 폴더에 `data/bible-json/KJV.json`·`KorRV.json`(§8 금지 번역본) 등 untracked 파일이 있으나 **커밋 대상에서 제외**함. 본 PR엔 포함되지 않음.

## 검증
- `flutter analyze lib` + 테스트 파일 무이슈.
- 신규/영향 테스트: `font_scale_provider_test.dart`(매핑·초기값·보정·저장값 6건), `bible_passage_screen_test.dart`(영어 토글 전 미노출 / 토글 시 영어가 `CpSubBox` 안에 노출) 추가 — 관련 11건 통과.
- 수동: 에뮬레이터(Android)에서 설정 폰트 크기 변경 시 본문 글자 배율 반영, 성경 본문 영어 토글 시 QT와 동일한 sub-box 확인(T 확인).

담당: DevD 이승욱
