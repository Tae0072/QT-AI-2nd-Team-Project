# 2026-06-11 Flutter QT Note Rich Editor Review Fixes Report

## 기준 문서

- `doc/2026-06-10_관리자웹_코드리뷰.md`
- `doc/2026-06-10_서버_코드리뷰.md`
- `doc/2026-06-10_플러터_코드리뷰.md`
- `doc/workspaces/DevA_이지윤/2026-06-10_코드리뷰_TODO_이지윤.md`

## 반영 내용

### Flutter QT 노트

- `feature/flutter-qt-note-rich-editor` 브랜치에 이전 stash 작업을 재적용했다.
- 하단 탭바의 `오늘` 문구를 `QT`로 갱신했다.
- QT 노트 툴바에서 하이라이트 단독 버튼을 제거하고, 글씨 크기/텍스트 색상/배경 색상을 선택 영역 또는 이후 입력에 적용하도록 정리했다.
- 굵게, 글씨 크기, 텍스트 색상, 배경 색상 마커가 중첩 적용되도록 rich text marker 파서/렌더러를 추가했다.
- `(1)`, `1)` 자동 목록은 빈 항목에서 스페이스 입력 시 제거되도록 처리했다.
- `@` 버튼을 작은 화면에서도 보이는 위치로 이동하고, `@Ge`처럼 영어 성경 권명 입력도 추천에 매칭되도록 테스트를 추가했다.
- `@창` 이후 성경 권 선택, 장/절 피커 선택, 구절 삽입 흐름을 유지했다.

### Flutter 리뷰 P2

- `qt_video_player.dart`의 하드코딩 툴팁을 l10n 키로 분리했다.
- `videoTooltipBack`, `videoTooltipPlay`, `videoTooltipPause`, `videoTooltipSpeed`, `videoTooltipFullscreen` 키를 `ko/en` ARB와 생성 파일에 반영했다.

### DevA 서버 TODO

- 사용처 없는 빈 `QtVisibility` enum을 `service-bible`, `admin-server` 복사본에서 제거했다.
- 남아 있던 `QtResponse` TODO의 `QtVisibility` 참조도 제거했다.
- 성서유니온 수집 parser가 외부 제목을 저장하지 않도록 바꿨다.
  - `title`은 외부 제목 대신 자체 생성한 `referenceText`와 동일하게 저장한다.
  - `service-bible`과 `admin-server` 복사본을 동일하게 맞췄다.
  - `SuTodayPassageParserTest` 기대값을 갱신했다.

## 검증

- `flutter gen-l10n`: 통과
- `dart format ...`: 통과
- `flutter test test/features/note/qt_note_rich_text_test.dart test/features/note/screens/qt_note_editor_screen_test.dart test/features/bible/screens/today_qt_screen_test.dart`: 통과, 15 tests
- `flutter analyze`: 통과, No issues found
- `qtai-server/gradlew.bat :service-bible:test --tests com.qtai.domain.qt.client.sum.SuTodayPassageParserTest`: 통과
- `qtai-server/gradlew.bat :admin-server:compileJava :service-bible:compileJava :service-ai:compileJava`: 통과
- `git diff --check`: 통과

## 참고

- 금지 키워드 스캔은 기존 문서/테스트/seed의 정책 문구와 기존 seed 데이터에서 매칭이 다수 발생했다. 이번 변경으로 새 본문 seed, 금지 번역본 fixture, plain secret은 추가하지 않았다.
- 관리자웹 리뷰의 refresh token, admin-web DTO 구체화, praise 버튼 연결 등은 DevE/admin-web 담당 범위라 이번 Flutter QT 노트 브랜치에는 포함하지 않았다.
