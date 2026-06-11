# 2026-06-11 Flutter QT/성경/리뷰 대응 통합 리포트

## 작업 브랜치

- `feature/flutter-qt-note-rich-editor`

## 통합 정리

오늘 이 채팅방에서 작성했던 개별 리포트는 삭제하고, 아래 구현 및 검증 내용을 이 리포트 하나로 통합했다.

- 삭제: `2026-06-11_flutter-qt-note-rich-editor-review-fixes_report.md`
- 삭제: `2026-06-11_flutter-bible-toc-ui_report.md`
- 신규 통합: `2026-06-11_flutter-qt-note-rich-editor-combined_report.md`

## 구현 내용

### 1. QT 노트 rich editor 서식 개선

- 하단 탭바의 `오늘` 문구를 `QT`로 변경했다.
- QT 노트 툴바에서 하이라이트 전용 버튼을 제거했다.
- 글씨 크기 입력을 숫자 직접 입력 방식에서 슬라이더 방식으로 변경했다.
- 글씨 크기, 텍스트 색상, 배경 색상이 선택 영역에 적용되도록 했다.
- 선택 영역이 없을 때는 이후 입력부터 새 서식이 적용되도록 했다.
- 굵게, 글씨 크기, 텍스트 색상, 배경 색상이 중복 적용되도록 rich text marker 파서와 렌더러를 보강했다.
- `(1)`, `1)` 자동 목록은 빈 항목에서 스페이스를 누르면 제거되도록 처리했다.

### 2. QT 노트 성경 구절 @ 멘션 개선

- 구절 삽입 버튼을 눌렀을 때 `@` 입력과 성경 권 추천이 바로 보이도록 했다.
- `@창`, `@Ge`처럼 한글/영어 검색어 모두 성경 권 추천에 매칭되도록 했다.
- 성경 권 선택 후 작은 장/절 피커를 통해 구절을 삽입할 수 있도록 했다.
- `@창 1:1` 같은 직접 입력 흐름도 기존대로 구절 삽입이 가능하도록 유지했다.

### 3. 오늘 QT 해설/AI 연결 확인 및 리뷰 대응

- Docker MySQL `qtai` DB의 오늘 QT row/매핑 누락 문제를 확인하고, 고린도전서 6:12-20 매핑 및 승인 해설 연결을 검증했다.
- `service-bible`의 QT import/backfill 로직은 이미 `qt_passage_verses`까지 저장하도록 반영되어 있어 코드 수정은 없음을 확인했다.
- `QtVisibility`처럼 더 이상 사용하지 않는 enum 및 TODO 참조를 `service-bible`, `admin-server` 복사본에서 제거했다.
- 성서유니온 수집 parser가 임의 제목을 저장하지 않도록 정리하고, 관련 parser 테스트 기대값을 갱신했다.
- 관리자 웹 프록시에서 `/api/v1/admin/auth/**` 요청이 generic `/api` proxy보다 먼저 `service-user:8081`로 전달되도록 분리했다.
- `admin-web/.env.example`에 `VITE_ADMIN_AUTH_PROXY_TARGET` 예시를 추가했다.
- 죽은 중복 migration인 `service-bible/src/main/resources/db/migration/V30__create_qt_video_clips.sql`을 제거했다.

### 4. Flutter 영상 플레이어 l10n 대응

- `qt_video_player.dart`의 하드코딩 tooltip 문구를 l10n 키로 분리했다.
- `videoTooltipBack`, `videoTooltipPlay`, `videoTooltipPause`, `videoTooltipSpeed`, `videoTooltipFullscreen`를 `ko/en` ARB 및 생성 파일에 반영했다.

### 5. 성경 탭 목차 UI 개편

- 두 번째 성경 탭 화면을 스크린샷 기준의 3열 목차 선택 UI로 변경했다.
- 상단 헤더를 `목차검색 :: 성경본문` 형태로 변경했다.
- 성경 권 목록, 장 목록, 절 목록을 각각 독립적인 세로 리스트로 배치했다.
- 성경 권 목록에는 `율법서`, `역사서`, `시가서`, `예언서`, `복음서`, `서신서` 구분 헤더를 추가했다.
- 선택된 권/장/절은 올리브 회색 배경으로 표시되도록 했다.
- 성경 권 행은 한글 권명과 영어 권명을 함께 보여준다.
- 하단 선택 바 또는 키보드 아이콘을 누르면 선택 구절을 조회하고, 기존 결과/영어 토글 기능은 바텀시트로 유지했다.

### 6. QT 탭 애니메이션 버튼 및 영상 이동

- QT 탭의 `시뮬레이터` 버튼 라벨을 `애니메이션`으로 변경했다.
- 영어 라벨은 `Simulator`에서 `Animation`으로 변경했다.
- `simulatorStatus == READY`일 때만 버튼이 활성화되는 기존 계약은 유지했다.
- 애니메이션 버튼을 누르면 오늘 QT 화면 하단의 영상 섹션으로 자동 스크롤되도록 했다.
- 영상 섹션에는 테스트용 안정 key를 추가했다.

## PR BLOCK 사전 점검

사용자 요청에 따라 현재 diff만 보지 않고, 같은 계약을 쓰는 기존 코드 전체를 검색해 유사 BLOCK 가능성을 확인했다.

### 점검한 계약

- `bibleSimulator` l10n 사용자 노출 라벨
- `simulatorStatus` enum 계약: `READY`, `MISSING`, `FAILED`, `DISABLED`
- `QtVideoClip.isReady`와 `QtVideoSection` 노출 조건
- `qtVideoClipProvider` 및 `/qt/{id}/video` 조회 경로
- QT 노트 `@` 성경 구절 멘션 추천/피커 흐름
- 성경 탭 기존 picker key 및 테스트 계약
- 관리자 웹 `/api/v1/admin/auth/**` 프록시 우선순위
- `QtVisibility`, `TITLE_PATTERN`, `extractTitle`, 죽은 V30 migration 잔여 참조

### 확인 결과

- Flutter 사용자 노출 라벨에는 `시뮬레이터/Simulator`가 남아 있지 않고, `애니메이션/Animation`으로 생성 파일까지 반영되어 있다.
- `bibleSimulator` 키 이름은 기존 API/테스트 계약과 호환을 위해 유지하되, 표시 값만 변경했다.
- `simulatorStatus`는 기존 모델 방어 로직대로 미지 값을 `MISSING`으로 처리하며, 버튼은 `READY`일 때만 활성화된다.
- 문서와 서버 코드에 남아 있는 `시뮬레이터` 용어는 도메인/API 계약명으로, 이번 사용자 노출 라벨 변경과 충돌하지 않는다.
- `QtVideoSection`은 클립 status가 `READY`이고 URL이 있을 때만 player를 렌더링하는 기존 방어 조건을 유지한다.
- 애니메이션 버튼 클릭은 실시간 생성/AI 호출 없이 기존 영상 조회 섹션으로 스크롤만 수행하므로, “사용자 요청 경로에서 시뮬레이터 생성 금지” 품질 게이트와 충돌하지 않는다.
- QT 노트 `@Ge` 영어 검색, 장/절 피커, 직접 입력 삽입 흐름에 대한 테스트가 유지되어 있다.
- 성경 탭 UI 변경 후 기존 휠 picker key 의존 테스트는 새 3열 목차 UI 기준으로 갱신했다.
- `QtVisibility`, 죽은 V30 migration 등 이전 리뷰 BLOCK 후보의 잔여 참조가 없음을 확인했다.

## 검증 명령

```bash
flutter gen-l10n
```

- 성공

```bash
dart format lib/features/bible/screens/today_qt_screen.dart test/features/bible/screens/today_qt_screen_test.dart
dart format lib/features/bible/screens/bible_browser_screen.dart test/features/bible/screens/bible_browser_screen_test.dart
```

- 성공

```bash
flutter test test/features/bible/screens/today_qt_screen_test.dart
```

- 성공: `All tests passed`

```bash
flutter test test/features/bible/screens/bible_browser_screen_test.dart
```

- 성공: `All tests passed`

```bash
flutter test test/features/bible
```

- 성공: `All tests passed`

```bash
flutter analyze
```

- 성공: `No issues found`

```bash
git diff --check
```

- 성공

이전 리뷰 대응 시점 검증:

```bash
flutter test test/features/note/qt_note_rich_text_test.dart test/features/note/screens/qt_note_editor_screen_test.dart test/features/bible/screens/today_qt_screen_test.dart
qtai-server/gradlew.bat :service-bible:test
qtai-server/gradlew.bat :admin-server:compileJava :service-bible:compileJava :service-ai:compileJava
admin-web/npm.cmd ci
admin-web/npm.cmd run build
```

- 모두 성공 확인

## 남은 상태

- 현재 요청에 따라 push는 하지 않았다.
- 사용자가 push를 요청하면 현재 브랜치 `feature/flutter-qt-note-rich-editor`에 남은 변경을 커밋 후 push하면 된다.
