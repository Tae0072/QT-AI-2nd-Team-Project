# 리포트 — 노트 펜 색상 + 내 계정 목업 노트

- 날짜: 2026-06-19 / 브랜치: `feature/note-pen-color-and-mock` → `dev`
- F-ID: F-03(기록/손그림), 목업 표시: F-03·F-10

## 변경 파일

| 파일 | 구분 | 내용 |
|---|---|---|
| `note/widgets/qt_note_format_toolbar.dart` | 수정 | `onTogglePenLongPress` 추가, 롱프레스 버튼은 InkWell(tap+longPress)로 구성 |
| `note/widgets/note_rich_text_editor.dart` | 수정 | 펜 전용색 `_penColor`(테마 기본 라이트검정/다크흰색), `_openPenColorSheet`, 손그림에 펜색 전달 |
| `note/providers/dev_mock_notes.dart` | 신규 | 디버그+내계정 게이트 가짜 노트 2개(운영 전 제거) |
| `note/providers/note_providers.dart` | 수정 | `notesProvider`에 목업 주입(두 경로) |
| `sharing/providers/dev_mock_sharing.dart` | 신규 | 디버그+내계정 게이트 가짜 내글 2개(운영 전 제거) |
| `sharing/providers/sharing_providers.dart` | 수정 | `mySharingPostsProvider`에 목업 주입 |
| `test/features/note/widgets/note_drawing_and_page_mode_test.dart` | 수정 | 펜 롱프레스→펜 색 시트 위젯 테스트 1건 추가 |

## 검증 결과
- `flutter analyze`(note·sharing·note test) → No issues found
- `flutter test`(note·sharing) → All tests passed (117, 신규 1 포함)
- 테마 파일 미수정(읽기 전용 준수)

## 동작 요약
- 펜 버튼 **짧게** = 펜 on/off(기존), **길게** = 펜 색 선택 시트.
- 펜 기본색: 라이트=검정, 다크=흰색. 선택 시 그 색 고정.
- 내 계정(이메일 일치) + 디버그 빌드에서만 기록·나눔에 `[목업]` 노트 2개씩 표시. DB 무변경.

## 운영 전 제거 체크(목업)
1. `dev_mock_notes.dart`, `dev_mock_sharing.dart` 삭제.
2. `note_providers.dart`/`sharing_providers.dart`의 `import ... dev_mock_*` 1줄 + `withDebugMock*` 호출 제거.
