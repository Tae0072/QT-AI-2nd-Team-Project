# 2026-06-11 성경 본문 목차 화면 overflow 수정 + Calm Paper 리디자인 (feature/flutter-bible-browser-redesign)

## 목표·배경
성경(본문) 탭의 목차검색 화면이 (1) 일부 디바이스/큰 글자 배율에서 권 목록 행이 세로로 넘쳐 `BOTTOM OVERFLOWED BY n PIXELS`가 뜨고, (2) 헤더·색·하단 바 구성이 타사 성경앱과 지나치게 유사하다는 지적. F-01(성경·QT 본문 화면) 범위의 UI 개선.

## 작업 내용
- overflow 근본 원인 제거: `_BibleBookRow`의 고정 `height: 45` + 2줄 Column(국문 14 + 영문 11) 조합이 큰 글자 배율에서 45px를 초과 → `minHeight 52` + 상하 패딩 기반으로 변경해 행이 늘어나도록. 숫자열(`_BibleNumberColumn`)도 고정 `itemExtent: 34` 대신 `minHeight 40`로 교체.
- 디자인 통일: 하드코딩 타사풍 팔레트(파랑-회색 `#506984` 헤더, 카키 `#B9BAA6` 선택색, `#E9EBEF` 등)를 앱 공통 Calm Paper 토큰(`context.appColors` + `AppGap`/`AppRadius`)으로 전면 교체. 위계는 색이 아니라 명도·여백·굵기로 표현(선택 = `accentSoft` 면 + 3px `accent` 좌측 바 + 본문색 굵게).
- 헤더 라벨 `목차검색 :: 성경본문` → `성경 본문`. 하단 키보드 아이콘 → 명시적 `조회` 버튼(ElevatedButton). 본문 결과 시트도 동일 타이포/여백으로 정리.
- 기능 없는 헤더 닫기(X) 버튼 제거: 본 화면은 홈 `IndexedStack` 탭이라 `Navigator.maybePop()`이 의미 있는 동작을 하지 않음(T 지시) → `onClose` 배선 일괄 제거.
- 위젯 key(`bible-book-list`/`bible-chapter-list`/`bible-verse-list`/`bible-selection-bar`/`bible-browser-english-toggle`)와 렌더 포맷(`2절`·`1:2`·섹션 라벨·영문)은 유지. 헤더 텍스트 변경에 맞춰 위젯 테스트 기대값만 갱신.

## 범위
- 브랜치: `feature/flutter-bible-browser-redesign` (base: `dev`).
- 변경 파일 2개: `flutter-app/lib/features/bible/screens/bible_browser_screen.dart`, 동 테스트.
- bible 도메인 화면이나 서버/데이터 변경 없음. 금지 번역본(개역개정·KJV 등) seed/fixture 미추가 — 저장소에 untracked로 존재하던 `data/bible-json/{KorRV,KJV}.json`은 스테이징하지 않음(§8).

## 검증
- `flutter analyze` 대상 파일 무이슈.
- `flutter test .../bible_browser_screen_test.dart` 2건 전체 통과(렌더·선택·조회·영문 토글, 검색 실패 안전 메시지).
- overflow는 고정 높이 제거로 구조적으로 해소(행이 콘텐츠에 맞춰 늘어남).

## 미해결 / 후속
- 실기기/웹에서 큰 글자 배율 수동 확인은 PR 리뷰 시 스크린샷으로 보강 권장.

담당: DevD 이승욱
