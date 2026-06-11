# 2026-06-11 Flutter 성경 탭 목차 UI 수정 리포트

## 작업 브랜치

- `feature/flutter-qt-note-rich-editor`

## 작업 범위

- Flutter 앱 두 번째 탭인 성경 본문 화면을 목차 검색형 3열 선택 UI로 개편했다.
- 기존 본문 조회 기능은 제거하지 않고, 하단 선택 바를 눌렀을 때 선택한 구절을 바텀시트로 조회하도록 유지했다.

## 변경 내용

### 성경 탭 UI

- 상단 헤더를 `목차검색 :: 성경본문` 형태의 파란 헤더로 변경했다.
- 성경 권 목록, 장 목록, 절 목록을 각각 독립적인 세로 리스트로 배치했다.
- 성경 권 목록에 구분 헤더를 추가했다.
  - 율법서
  - 역사서
  - 시가서
  - 예언서
  - 복음서
  - 서신서
- 선택된 권/장/절은 스크린샷 기준의 올리브 회색 배경으로 표시되도록 했다.
- 성경 권 행은 한글 권명과 영어 권명을 함께 표시한다.
- 하단 바에는 현재 선택된 권/장을 `창세기 1장` 같은 형태로 표시하고, 선택 바 또는 키보드 아이콘을 누르면 본문을 조회한다.

### 기존 기능 유지

- 성경 권/장 변경 시 서버의 장별 절 목록을 다시 조회하는 흐름을 유지했다.
- 절 선택 시 단일 절 조회로 연결했다.
- 조회 결과의 영어 본문 토글 기능을 바텀시트 안에서 유지했다.
- 장/절 목록 조회 실패 시 사용자에게 상태 메시지를 보여준다.

### 테스트

- 기존 휠 피커 기준 테스트를 새 3열 목차 UI 기준으로 갱신했다.
- 선택한 절이 실제 조회 요청의 `verseFrom`, `verseTo`에 반영되는지 검증했다.
- 조회 결과 바텀시트와 영어 본문 토글 동작을 검증했다.

## 변경 파일

- `flutter-app/lib/features/bible/screens/bible_browser_screen.dart`
- `flutter-app/test/features/bible/screens/bible_browser_screen_test.dart`
- `doc/workspaces/DevA_이지윤/reports/2026-06-11_flutter-bible-toc-ui_report.md`

## 검증 결과

```bash
dart format lib/features/bible/screens/bible_browser_screen.dart test/features/bible/screens/bible_browser_screen_test.dart
```

- 성공

```bash
flutter test test/features/bible/screens/bible_browser_screen_test.dart
```

- 성공: `All tests passed`

```bash
flutter analyze
```

- 성공: `No issues found`

```bash
flutter test test/features/bible
```

- 성공: `All tests passed`

## 비고

- 이번 변경은 Flutter 성경 탭 화면과 해당 테스트에 한정했다.
- 서버/API 계약 변경은 없다.
