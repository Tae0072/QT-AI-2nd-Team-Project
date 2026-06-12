# Report 2026-06-12 qt-note-editor-fixes

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/qt-note-editor-rendering` |
| 대상 브랜치 | `dev` |
| workflow | `doc/workspaces/DevE_김지민/workflows/2026-06-12_qt-note-editor-fixes.md` |
| 작업 범위 | Flutter QT 노트 작성/서식, QT 해설 표시, QT 노트 저장 실패 수정 |

## 변경 요약

- QT 노트 작성 화면에서 QT 본문 패널과 노트 작성 패널을 항상 동시에 렌더링하도록 유지했다.
- QT 본문 영역과 노트 작성 영역에 각각 독립 스크롤바 키를 두어, 노트 작성 중에도 본문을 확인할 수 있게 했다.
- 노트 입력 포커스 상태에 따라 화면 구조를 바꾸는 흐름을 제거해, 첫 키보드 입력 시 화면이 QT 첫 화면처럼 되돌아가는 재렌더링 문제를 방지했다.
- `MaterialApp` key가 `initialRoute`에 묶여 main app 내부 route 상태 변화 때 Navigator가 재생성될 수 있던 구조를 수정했다. splash와 main app key만 구분하고, main app 내부에서는 안정적인 key를 사용한다.
- main app이 한 번 시작된 뒤 auth/bootstrap 상태가 잠깐 `unknown`으로 갱신될 때 splash `MaterialApp`으로 교체되며 push된 노트 작성 route가 사라질 수 있던 원인을 차단했다.
- `NoteRichTextEditor`의 본문 `FocusNode`를 State 필드로 명시해 keyboard inset/rebuild 이후에도 focus가 유지되도록 했다.
- QT 노트 화면에서 글자서식바를 제목 입력 박스와 노트 작성 본문 입력창 사이의 일반 레이아웃 한 줄 영역으로 이동했다.
- `NoteRichTextEditor`에 화면별 header와 서식바 배치 옵션을 추가해 제목 입력 박스, 서식바, 본문 입력창 순서를 유지하고 overlay 없이 배치되게 했다.
- B 버튼이 최근 선택 범위를 잃지 않고 선택 텍스트를 `**강조**`로 감싸도록 유지했고, `QtNoteRichTextParser`가 bold TextSpan을 `FontWeight.w800`으로 렌더링하도록 수정했다.
- QT 해설 화면의 절 라벨은 절 번호만 표시하고, `검수해설` 같은 sourceLabel 문구는 노출하지 않도록 정리했다.
- QT 노트 저장 실패 원인을 확인했다. Flutter payload는 서버 계약과 일치했지만, service-note가 QT passage 검증 중 Docker 내부에서 `localhost:8082`를 호출해 service-bible 연결이 거부되며 `POST /api/v1/notes`가 502/C0006으로 실패했다.
- `docker-compose.yml`의 `service-note.environment`에 `QTAI_SERVICES_BIBLE_BASE_URL=http://service-bible:8082`를 추가해 service-note가 Docker 네트워크의 service-bible을 호출하도록 수정했다.
- PR BLOCK 예방을 위해 같은 `ServiceEndpointsProperties` 계약을 쓰는 기존 RestClient 사용처 전체를 검색했다. 누락돼 있던 service-user의 bible/admin URL, service-note의 user/ai URL, service-ai의 bible/admin URL도 Docker 내부 서비스명으로 명시했다.
- QT 노트 저장 실패 시 DioException의 HTTP status/path/code/message/traceId를 개발 로그로 남기도록 보강했다. Authorization/token은 로그에 남기지 않는다.

## 변경 파일

- `flutter-app/lib/features/note/screens/qt_note_editor_screen.dart`
- `flutter-app/lib/features/note/widgets/note_rich_text_editor.dart`
- `flutter-app/lib/features/note/models/qt_note_rich_text.dart`
- `flutter-app/lib/features/bible/screens/today_qt_screen.dart`
- `flutter-app/lib/features/bible/screens/bible_passage_screen.dart`
- `flutter-app/lib/features/study/screens/qt_study_content_screen.dart`
- `flutter-app/test/features/note/screens/qt_note_editor_screen_test.dart`
- `flutter-app/test/features/note/qt_note_rich_text_test.dart`
- `flutter-app/test/features/bible/screens/today_qt_screen_test.dart`
- `docker-compose.yml`

## 테스트 보강

- 노트 첫 입력 후 QT 본문 스크롤과 노트 작성 스크롤이 모두 유지되는지 검증했다.
- `TodayQtScreen`에서 노트 버튼으로 진입한 뒤 앱 route 상태가 `unknown`으로 갱신되어도 `QT 노트`, 본문 focus, controller text가 유지되는지 검증했다.
- keyboard inset 변화 후에도 본문 TextField의 controller/focusNode가 유지되는지 검증했다.
- 서식 수정바가 제목 입력 박스 아래, 본문 입력창 위에 있고 keyboard inset 변화 후에도 B/텍스트 색상/배경 색상 버튼을 누를 수 있는지 검증했다.
- B 버튼 클릭 후 `controller.text`가 `**강조**`가 되는지 검증했다.
- `QtNoteRichTextParser.parse` 결과에서 강조 TextSpan의 `fontWeight.index`가 `FontWeight.w800.index` 이상인지 검증했다.
- QT 해설 화면에서 `2:1 해설`과 `검수해설`이 보이지 않고 `1`만 표시되는지 검증했다.
- 임시저장 버튼 클릭 시 `createQtNote`가 `status=DRAFT`, `qtPassageId`, `title`, `body`, `verseIds`를 포함해 호출되는지 검증했다.
- 저장 버튼 클릭 시 `createQtNote`가 `status=SAVED`, `qtPassageId`, `title`, `body`, `verseIds`를 포함해 호출되는지 검증했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `flutter test test/features/note/screens/qt_note_editor_screen_test.dart test/features/note/qt_note_rich_text_test.dart` | 통과 |
| `flutter test test/features/note/screens/note_edit_screen_test.dart` | 통과 |
| `flutter test test/features/bible/screens/today_qt_screen_test.dart` | 통과 |
| `flutter test test/features/bible/screens/bible_passage_screen_test.dart` | 통과 |
| `flutter analyze` | 통과, `No issues found!` |
| `flutter test` | 통과, `184`개 테스트 통과 |
| `git diff --check` | 통과 |

### 추가 검증

| 명령 | 결과 |
| --- | --- |
| `flutter test test/features/note/screens/qt_note_editor_screen_test.dart --plain-name "서식 수정바는 제목 입력 박스와 본문 입력창 사이에 배치된다"` | 통과 |
| `flutter test test/features/bible/screens/today_qt_screen_test.dart --plain-name "QT 노트 첫 입력 중 앱 route 상태가 unknown으로 갱신되어도 노트 화면과 본문 입력을 유지한다"` | 통과 |
| `flutter test test/features/bible/screens/today_qt_screen_test.dart` | 통과, `7`개 테스트 통과 |
| `flutter test test/features/note/screens/qt_note_editor_screen_test.dart` | 통과, `17`개 테스트 통과 |
| `flutter test test/features/note` | 통과, `37`개 테스트 통과 |
| `.\gradlew.bat :service-note:test --tests "com.qtai.domain.note.client.qt.NoteQtRestClientAdapterTest"` | 통과 |
| `docker compose up -d --no-deps service-note service-user service-bible` | 통과, service-note 재생성 |
| service-note 컨테이너 `QTAI_SERVICES_BIBLE_BASE_URL` 확인 | `http://service-bible:8082` |
| service-note 컨테이너에서 `service-bible:8082` 연결 확인 | 통과 |
| `rg -n "endpoints\\.get[A-Za-z]+BaseUrl\\(\\)" qtai-server -g "*.java"` | service-user/service-note/service-ai RestClient endpoint 계약 전체 확인 |
| `docker compose config`의 `QTAI_SERVICES_*_BASE_URL` 확인 | service-user(note/bible/admin), service-note(bible/user/ai), service-ai(bible/admin) 설정 확인 |
| 수정 전 Docker gateway 경유 `POST /api/v1/notes` | 502, `C0006`, `외부 API 호출에 실패했습니다.` 재현 |
| 수정 후 Docker gateway 경유 `POST /api/v1/notes` DRAFT | 201, 생성 noteId=2, status=`DRAFT` |
| 수정 후 Docker gateway 경유 `POST /api/v1/notes` SAVED | 201, 생성 noteId=3, status=`SAVED` |
| gateway 로그 확인 | 수정 전 502, 수정 후 201 두 건 확인 |
| `flutter analyze` | 통과, `No issues found!` |
| `git diff --check` | 통과, 공백 오류 없음 |

## 미실행/제외 검증

- `gitleaks detect --source . --redact --exit-code 1`: 로컬 환경에 `gitleaks` 명령이 설치되어 있지 않아 실행하지 못했다.
- 전체 서버 `build`, Spectral lint는 서버 Java/OpenAPI 계약 변경이 아니라 Docker 런타임 환경 변수 변경이라 실행 범위에서 제외했다. 대신 service-note QT RestClient 대상 테스트와 실제 Docker gateway 경유 저장 요청 201로 검증했다.
