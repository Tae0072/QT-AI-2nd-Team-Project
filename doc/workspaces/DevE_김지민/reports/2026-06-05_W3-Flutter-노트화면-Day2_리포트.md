# 2026-06-05 · W3 Flutter 노트화면 Day2 리포트

> 브랜치: `feature/note-flutter-screens`
> 모드: 학습(SKILL 🟢 소크라테스 강) · 검증: `flutter analyze` 전체 무경고
> 범위: N-04 / 달력 / 외부공유 / 서식툴바 / 나눔 댓글·신고 보완 + 명세대조 + 학습문서

---

## 한 일 (Day2 전 항목 완료)

### N-04 상세/수정/삭제
- 모델 `NoteDetail`·`NoteVerseRef`(전 카테고리 1모델), repo `getDetail/update/delete`, `noteDetailProvider`(family).
- `note_detail_screen.dart`: 보기(자유=body / 묵상=4섹션) + 수정(자유노트만) + 삭제(확인창).
- 수정 = N-03 편집모드 재사용(설계 A). 전달 방식 = **i(noteId만 넘기고 재조회)**. 전용 인자 `NoteEditArgs`.
- 묵상 [수정]은 편집화면 부재로 숨김(→ 회의 안건 #2).

### N-01 달력 탭
- 모델 `MeditationCalendar`/`CalendarDay`/`CalendarSummary`, repo `getMeditationCalendar(month)`, `meditationCalendarProvider`(월별 family) + `noteCalendarViewProvider`(토글).
- `table_calendar`로 달력 위젯(`MeditationCalendarView`): 점 표시(eventLoader ①) + 날짜탭 이동 + 월이동 재조회 + 요약줄.
- N-01 AppBar 토글(목록↔달력).

### 외부 공유 (§19.1)
- `note_share_sheet.dart`: 바텀시트 + 미리보기 카드(RepaintBoundary) + 텍스트(`SharePlus`) / 카드이미지(`toImage`→임시파일→공유).
- 상세 AppBar 공유 버튼(전 카테고리).

### 서식 툴바 (Day2-9)
- `note_format_toolbar.dart`: 마크다운 마커 삽입(굵게/기울임/제목/목록/인용/체크박스/구분선). 백엔드 body=평문 유지(설계 A).
- 색/크기/하이라이트는 마크다운 불가 → 회의 안건 #3.

### 나눔 보완 (Day2-10)
- `sharing_repository`: `getComments/createComment/deleteComment/report` 추가, `CommentItem` 모델.
- `sharing_detail_screen`: "준비 중" placeholder 제거 → 댓글 목록/입력/삭제 + 신고 바텀시트(POST/사유선택) 연결.

### 명세 대조 점검 — 버그 1건 잡음
- N-04 [수정]을 `isFreeNote`로 열면 **설교노트 PATCH 시 verseIds 미전송 → note_verses 삭제**(데이터 손실, 04 §4.3.6).
- → [수정] 게이트를 `writableNoteCategories`(기도/회개/감사)로 좁힘. 설교노트 편집은 B-03(이지윤)로(회의 안건 #4).

---

## 패키지 추가 (pubspec 공용 변경)
- `table_calendar ^3.2.0`, `share_plus ^12.0.2`, `path_provider`(+의존성 `intl`·`simple_gesture_detector` 등).
- 팀원: pull 후 `flutter pub get` + (네이티브 플러그인이라) 앱 cold restart. → 회의 안건 #5 / PR 설명에 명시 예정.

## 산출 문서
- 학습: `study-notes/2026-06-05_W3-Day2-Flutter개념정리_학습노트.md`, `..._table_calendar-초보자-완전가이드.md`, `..._UseCase아키텍처-내파트-폴더구조_학습노트.md`
- 회의: `workflows/2026-06-05_회의-안건-목록.md`, `..._shares-죽은코드-정리-플랜.md`

## 검증
- `flutter analyze` (전체) **No issues found**. 실데이터 왕복(레벨2: 백엔드+카카오)은 추후.

## 다음 (W4)
- 묵상/설교 편집 화면(회의 결정 후), 리치텍스트 여부 결정, `shares` 죽은코드 정리(회의 결정 후).

## commit / push
- 오늘 작업 commit 완료. **push·PR은 김지민 직접.** PR 설명에 "패키지 3종 추가 → pull 후 pub get" + 회의 안건 링크.
