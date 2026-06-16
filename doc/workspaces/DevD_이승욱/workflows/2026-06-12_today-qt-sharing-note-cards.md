# 2026-06-12 오늘 QT·나눔·기록 화면 프로토타입 정합 (feature/mobile-tabs-layout-and-bible-range)

## 목표·배경
T 요청: 오늘 QT·나눔(·기록) 화면을 DESIGN_PROTOTYPE.html처럼. 특히 ① QT는 절의 영어를 별도 영역(박스)으로 분리, ② 나눔은 피드 카드 형태, ③ 기록 목록은 사진처럼 카테고리 배지가 보이는 카드.

## 작업 내용
### ① 오늘 QT 절 (`today_qt_screen.dart` `_VerseTile`)
- 프로토타입 `.verse` 패턴으로 변경: 좌측 절 번호(회색 text2) + 본문(Expanded). 영어는 인라인 텍스트 → **sunken `CpSubBox`로 분리**(영어 토글 ON일 때만). 절번호는 `colorScheme.primary` → `text2`.

### ② 나눔 피드 (`sharing_feed_screen.dart`)
- 목록 `ListTile` → `_PostCard`(프로토타입 `.post`): 작성자(잉크 굵게) + 카테고리 배지(`CpBadge`) + 상대시간 / 제목(16 w600) / 본문 미리보기(2줄, text2) / 좋아요·댓글(liked=accentDot). 카테고리 라벨·상대시간 헬퍼 추가.
- 검색바 하드코딩(`Colors.grey[100]`) 제거 → 테마 입력 데코 사용. 빈 상태 `Colors.grey` → `outline`. 구분선 hairline 토큰.

### ③ 기록 목록 (`note_list_screen.dart` `_NoteListBody`)
- 목록 `ListTile` → `_NoteCard`(사진처럼): sunken 카드 + 제목(16 w600) + 카테고리 배지(QT/설교/기도/…) + 날짜·범위 메타 + (임시저장)·(나눔 accentDot 배지). dev #533의 달력·상태 필터·카테고리 칩은 유지.

## 데이터 메모
- 절 영어는 `BibleVerse.englishText` 사용(절별 "해설" 필드는 응답에 없어 미표시).
- 나눔 피드 item에는 인용(snap) 필드가 없어 카드에서 생략. 카테고리/시간/좋아요/댓글만.
- 노트 목록 item에는 본문 미리보기 필드가 없어 메타로 날짜·`rangeLabel` 사용.

## 범위/주의
- 오늘 QT(`features/bible`)는 이지윤님 담당 — PR 리뷰 필요. 나눔·기록은 본인(DevD) 담당.
- (참고) 마이·노트·성경 Calm Paper 재설계는 dev #531/#533/#530으로 이미 반영됨.
- 검증 중 발견한 **찬양(praise) 화면 JSON 파싱 오류**(`_Map is not List`)는 본 작업과 무관한 기존 버그 — 후속 별도 처리 필요.

## 검증
- `flutter analyze lib` 무이슈, `flutter test` 181건 전체 통과.
- 에뮬레이터(Android API 37): QT 좌측 번호+본문 / 나눔 검색바·칩 정리(피드 비어 카드 미표시) / 기록 "dsfsd" 카드(기도 배지 + 6월 10일) 확인.

담당: DevD 이승욱
