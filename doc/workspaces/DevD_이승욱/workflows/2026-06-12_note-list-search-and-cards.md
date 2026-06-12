# 2026-06-12 기록(노트) 목록 검색 + 카드 재디자인 + bodyPreview (feature/mobile-note-list-search-cards)

## 목표·배경
T 요청: 기록 탭의 노트 목록 카드를 참고 사진처럼 바꾸고(검색·날짜 유지), 카드에 본문 미리보기를 넣는다. 참고 사진과 여러 차례 목업으로 정합한 최종안: **카드 좌측 카테고리 색 세로선(아이콘 없음) + 날짜·시각 스택 + 배지 2개(오전/오후 + 카테고리) + 제목 + 본문 2줄 미리보기**, 상단에 검색바.

## 작업 내용
### ① 검색 (서버 검색 연동)
- 백엔드는 이미 `GET /api/v1/notes?q=`(제목·본문 LIKE)를 구현해 둠. Flutter만 미연동이었다.
- `note_repository.getNotes`에 `q` 추가, `noteSearchQueryProvider`(StateProvider) 신설, `notesProvider`가 q를 watch.
- `note_list_screen`을 ConsumerStatefulWidget으로 바꿔 상단 검색바(엔터 제출·X로 해제) 추가. 달력(날짜 선택)은 유지. 검색 결과 없으면 "검색 결과가 없습니다".
- l10n: `noteSearchHint`/`noteSearchEmpty`(app_en/app_ko + gen-l10n).

### ② 카드 재디자인 (승인 목업대로)
- `note_card`를 전면 재작성: 좌측 카테고리 색 세로선(IntrinsicHeight + stretch), 아이콘/썸네일 제거. 날짜·시각을 위아래 두 줄로, 우측에 배지 2개(오전/오후 + 카테고리). 제목 아래 본문 2줄 미리보기. 임시저장/나눔은 배지로 보존.
- `date_format_utils`: `dateDotLabel`/`dateDotFromIso`(yyyy.MM.dd), `clockLabel`(hh:mm AM/PM), `amPmKoLabel`(오전/오후) 추가.

### ③ 백엔드: 목록에 bodyPreview 추가 (note 도메인, T 담당)
- 목록 응답에 본문 미리보기가 없어, `NoteListItem` record에 `bodyPreview` 추가.
- `NoteService.toListItem`에서 `buildBodyPreview`로 생성: 자유노트는 body, 묵상노트는 4섹션 중 먼저 채워진 것 → 공백 정리 후 80자에서 truncate, 없으면 null.
- OpenAPI `NoteListItem` 스키마에 `bodyPreview`(nullable) 추가. 스키마 변경 없음(기존 body 컬럼 파생) → Flyway 불필요.

## 범위/주의
- 기록(노트)·검색은 T(이승욱) 담당 도메인. bodyPreview는 read-only 매핑이라 admin-server 복사본은 독립 컴파일되어 영향 없음(추후 필요 시 동기화).
- 검색은 서버 검색이라 페이지 전체 대상. 클라 임시 필터 아님.
- 작업 폴더의 `data/bible-json/KJV.json`·`KorRV.json`(§8 금지 번역본)은 커밋에서 제외.

## 검증
- `flutter analyze lib` 무이슈, 관련 테스트(date util·note_card·note_list_screen) 21건 통과. note_list 위젯 테스트는 검색바 추가로 세로 길이가 늘어 뷰포트를 실제 폰 크기로 키워 통과.
- 백엔드 `:service-note:compileJava` 성공, service-note docker 재빌드·healthy 재기동.
- 수동: 에뮬레이터에서 카드(세로선·날짜/시각·배지 2개·미리보기)·검색바 확인(T 확인).

담당: DevD 이승욱
