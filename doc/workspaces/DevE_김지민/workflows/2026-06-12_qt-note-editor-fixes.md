# Workflow 2026-06-12 qt-note-editor-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | Codex |
| 브랜치 | `feature/qt-note-editor-rendering` |
| 대상 브랜치 | `dev` |
| 관련 범위 | Flutter QT 탭 노트 작성, 리치 텍스트 렌더링, QT 해설 표시 |
| 기준 | AGENTS.md, Flutter 기존 화면/테스트 관례 |

## 작업 목표

QT 탭의 노트 작성 화면에서 앱 최초 실행 후 첫 글자를 입력할 때 화면이 다시 QT 첫 화면처럼 보이는 문제를 막는다. 노트 작성 진입 후에도 오늘의 QT 본문 영역과 노트 작성 영역이 동시에 보이도록 유지하고, 두 영역은 각각 독립 스크롤을 갖는다.

키보드 표시나 `MediaQuery.viewInsets` 변화로 rebuild가 발생하는 것은 허용하지만, Navigator route가 오늘 QT 화면으로 pop/replacement 되거나 본문 controller/focus가 초기화되면 안 된다. 앱 route 상태가 갱신되어도 push된 `QtNoteEditorScreen`은 유지되어야 한다.

텍스트 범위 선택 시 OS 선택 메뉴는 제거하지 않고 유지한다. QT 노트 화면의 서식 수정바는 TextField 내부 overlay가 아니라 제목 입력 박스와 노트 작성 본문 입력창 사이의 일반 레이아웃 한 줄 영역에 둔다. 화면 순서는 오늘의 QT 본문 영역, 제목 입력 박스, 글자서식바, 노트 작성 본문 입력창, 임시저장/저장 버튼 순서여야 한다. B 버튼은 선택 범위를 잃은 뒤에도 최근 선택 범위에 `**강조**` 마커를 적용해야 하며, `QtNoteRichTextParser`는 해당 마커 구간을 `FontWeight.w800` 이상으로 렌더링해 실제 화면에서도 확실히 굵게 보이게 한다.

기존 요청 범위에 포함된 QT 해설 화면은 절 표시에서 장 번호와 `해설` 접미사를 제거하고, `검수해설` 같은 sourceLabel 문구를 노출하지 않는다.

## 구현 범위

- `QtNoteEditorScreen`
  - QT 본문 패널과 노트 작성 패널을 항상 같은 화면에 유지한다.
  - QT 본문 스크롤바(`qt-note-passage-scroll`)와 노트 작성 스크롤바(`qt-note-editor-scroll`)를 분리한다.
  - 노트 입력 포커스/키보드 진입으로 상위 화면 구조가 바뀌지 않게 한다.
- `QTAIApp`
  - splash에서 main app으로 넘어갈 때만 Navigator를 새로 만들고, main app 내부에서는 `initialRoute` 변화로 Navigator가 재생성되지 않도록 `MaterialApp` key를 안정화한다.
  - main app이 한 번 시작된 뒤에는 auth/bootstrap 상태가 잠깐 `unknown`으로 갱신되어도 splash `MaterialApp`으로 되돌아가지 않게 해 push된 `QtNoteEditorScreen` route를 유지한다.
- `NoteRichTextEditor`
  - 본문 `FocusNode`와 `ScrollController`를 State 필드에서 한 번만 생성하고 dispose한다.
  - 화면별 header와 서식바 배치 옵션을 지원한다.
  - QT 노트 화면에서는 제목 입력 박스와 본문 입력창을 유지하고, 서식 수정바를 제목 입력 박스와 본문 입력창 사이에 배치한다.
  - B 버튼이 현재 선택 범위 또는 최근 선택 범위에 `**` 마커를 적용하도록 유지한다.
- `QtNoteRichTextParser`
  - bold=true인 TextSpan을 `FontWeight.w800`으로 렌더링한다.
- QT 해설 표시
  - 절 라벨은 `25`처럼 절 번호만 표시한다.
  - 해설/용어 sourceLabel은 사용자 화면에 노출하지 않는다.

## 추가 작업 - QT 노트 저장 실패

QT 노트 작성 화면에서 `임시저장` 또는 `저장` 버튼을 눌렀을 때 Flutter는 실패 스낵바를 띄우고, 실제 `POST /api/v1/notes`는 gateway에서 502로 종료되는 문제를 함께 수정한다.

- Flutter 요청 payload는 `category=MEDITATION`, `qtPassageId`, `title`, `body`, `verseIds`, `status(DRAFT/SAVED)`, `visibility=PRIVATE`가 서버 `CreateNoteRequest`와 일치하는지 테스트로 고정한다.
- 원인은 service-note가 QT passage 읽기 검증을 위해 service-bible을 호출할 때 Docker 내부에서 기본값 `http://localhost:8082`를 사용한 점으로 본다. 컨테이너 내부 `localhost`는 자기 자신이므로 service-bible로 연결되지 않는다.
- `docker-compose.yml`의 `service-note.environment`에 `QTAI_SERVICES_BIBLE_BASE_URL=http://service-bible:8082`를 추가한다.
- 같은 `ServiceEndpointsProperties` 계약을 쓰는 기존 RestClient 사용처 전체를 검색해 Docker 내부 DNS 설정 누락을 함께 보정한다. service-user에는 note/bible/admin URL, service-note에는 bible/user/ai URL, service-ai에는 bible/admin URL을 명시한다.
- 저장 실패 시 Flutter catch 블록에서 에러를 성공 처리하지 않고, DioException의 HTTP status/path/code/message/traceId만 개발 로그에 남긴다. Authorization/token은 로그에 남기지 않는다.
- 저장 성공 후 기존 성공 스낵바와 `Navigator.pop` 흐름은 유지한다.

## 제외 범위

- 서버 API 코드, DB 스키마, MSA 도메인 경계 변경
- 노트 저장 API 계약 변경
- 새로운 AI 생성/검수 로직 추가
- 관리자 웹 화면 변경

## 테스트 계획

- 노트 작성 첫 입력 후 `QT 노트`, 입력값, QT 본문 스크롤, 노트 작성 스크롤이 모두 유지되는지 확인한다.
- `TodayQtScreen`에서 노트 버튼으로 진입한 뒤 앱 route 상태가 `unknown`으로 갱신되어도 `QtNoteEditorScreen`, 본문 focus, controller text가 유지되는지 확인한다.
- keyboard inset 변화 후에도 본문 TextField, focusNode, controller text가 유지되는지 확인한다.
- B 버튼 클릭 후 controller text가 `**강조**` 형태로 바뀌는지 확인한다.
- `QtNoteRichTextParser.parse` 결과의 강조 TextSpan `fontWeight`가 `FontWeight.w800` 이상인지 확인한다.
- 서식 수정바가 제목 입력 박스 아래, 본문 입력창 위에 배치되고 keyboard inset 변화 후에도 B/색상/배경색 버튼이 hitTest 가능한지 확인한다.
- QT 해설 화면에서 절 번호만 표시되고 `검수해설` 문구가 보이지 않는지 확인한다.
- QtNoteEditorScreen에서 임시저장 클릭 시 `createQtNote`가 `status=DRAFT`와 필수 payload로 호출되는지 확인한다.
- QtNoteEditorScreen에서 저장 클릭 시 `createQtNote`가 `status=SAVED`와 필수 payload로 호출되는지 확인한다.
- Docker MSA 환경에서 수정 전 `POST /api/v1/notes`가 `502/C0006`으로 실패하는지 재현하고, 수정 후 DRAFT/SAVED 요청이 각각 201을 반환하는지 확인한다.
- service-note 컨테이너에서 `QTAI_SERVICES_BIBLE_BASE_URL`과 `service-bible:8082` 연결성을 확인한다.
- `ServiceEndpointsProperties` getter 사용처 전체와 `QTAI_SERVICES_*_BASE_URL` compose 설정을 대조해 누락된 Docker 내부 URL이 없는지 확인한다.

## 검증 명령

- `flutter test test/features/note/screens/qt_note_editor_screen_test.dart test/features/note/qt_note_rich_text_test.dart`
- `flutter test test/features/note/screens/note_edit_screen_test.dart`
- `flutter test test/features/bible/screens/today_qt_screen_test.dart`
- `flutter test test/features/bible/screens/bible_passage_screen_test.dart`
- `flutter analyze`
- `flutter test`
- `flutter test test/features/note`
- `.\gradlew.bat :service-note:test --tests "com.qtai.domain.note.client.qt.NoteQtRestClientAdapterTest"`
- `docker compose up -d --no-deps service-note service-user service-bible`
- 실제 Docker gateway 경유 `POST /api/v1/notes` DRAFT/SAVED 201 확인
- `git diff --check`
