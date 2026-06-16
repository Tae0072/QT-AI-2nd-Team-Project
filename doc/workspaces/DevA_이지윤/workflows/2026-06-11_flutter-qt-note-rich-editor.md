# 2026-06-11 flutter-qt-note-rich-editor workflow

## 작업 목적

`feature/flutter-qt-note-rich-editor` PR의 QT 노트 rich editor, 성경 목차 UI, QT 애니메이션 이동 기능을 `dev` 기준으로 정리하고, Claude 리뷰에서 차단될 수 있는 유사 계약 문제를 사전에 해소한다.

## 작업 브랜치

- base: `dev`
- head: `feature/flutter-qt-note-rich-editor`
- PR: `#492`

## 수행 범위

- QT 노트 서식 툴바 및 rich text marker 적용 정책 점검
- `(1)`, `1)` 자동 목록 제거 경로 테스트 보강
- `@` 성경 구절 멘션의 한글/영어 검색, 장/절 picker, 실패 경로 테스트 보강
- 성경 탭 3열 목차 UI의 장 수 계약 중복 제거
- 성경 검색 실패 시 사용자에게 raw exception이 노출되지 않도록 메시지 정리
- QT 탭의 시뮬레이션 표시명을 애니메이션으로 변경하고 영상 섹션 스크롤 이동 유지
- 통합 리포트 갱신 및 PR 필수 workflow/report 경로 정리

## 제외 범위

- 서버/관리자 웹 신규 기능 추가
- AI 해설 또는 애니메이션 즉시 생성 경로 추가
- Bible 본문 데이터, seed, fixture 변경
- `dev` 또는 `master` 직접 push

## 검증 계획

- `flutter gen-l10n`
- `dart format` 대상 파일
- `flutter analyze`
- `flutter test test/features/bible`
- `flutter test test/features/note/qt_note_rich_text_test.dart test/features/note/screens/qt_note_editor_screen_test.dart`
- `git diff --check`

## 리뷰 대응 계획

- Claude BLOCK 항목은 테스트로 재현 가능하게 보강한다.
- WARN 항목 중 같은 계약을 쓰는 기존 코드 중복과 raw exception 노출은 함께 수정한다.
- PR 본문 수정 권한이 없을 수 있으므로, 저장소 내 workflow/report 파일은 반드시 커밋에 포함한다.
