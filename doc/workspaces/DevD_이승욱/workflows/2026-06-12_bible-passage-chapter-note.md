# 2026-06-12 성경 본문 장 전체 보기 + 범위 선택 → 설교 노트 연동 (feature/bible-passage-chapter-note)

## 목표·배경
T 요청: 성경 본문 보기 개편.
1. 목차에서 장+절을 고르면 본문은 **그 장 전체**를 보여주고 선택 절을 첫 화면에 포커스.
2. 본문에서 절을 **탭-탭으로 범위 지정**.
3. 범위 선택 후 '노트 작성하기' → **설교 노트 작성 화면에 선택 본문 동봉**(오늘의 QT 노트 흐름과 동일).
4. 해설 버튼이 안 눌리는 것도 확인.

## 작업 내용
### ① 본문 페이지 (`bible_passage_screen.dart`)
- 인자 변경: `range`(선택 범위만) → `chapter`(장 전체 `BibleVerseRange`) + `focusVerseNo`. `getChapterVerses`로 장 전체를 받아 모든 절 렌더.
- 진입 시 포커스 절을 `GlobalKey` + `Scrollable.ensureVisible(alignment: 0.0)`로 **첫 줄에 스크롤**.
- 절 **탭-탭 범위 선택**(`VerseRangeSelection` 재사용) + 선택 절 `accentSoft` 면 하이라이트.
- 하단 고정 액션바: 현재 선택 범위 라벨 + **'노트 작성하기'**.
- 해설 버튼은 현재 선택 범위 기준 `/qt/passage-study` 가용성으로 활성/비활성(기존 로직 유지).

### ② 목차 (`bible_browser_screen.dart`)
- `_search`: `getVerses`(선택 범위) → `getChapterVerses`(장 전체) 로 바꾸고 `BiblePassageScreen(chapter, focusVerseNo: _verseFrom)`로 진입.

### ③ 노트 연동 (`note_edit_screen.dart`)
- `NoteEditArgs`에 `referenceText`·`versePreview` 추가. 본문 '노트 작성하기' → `noteEdit(category: SERMON, verseIds + 참조라벨 + 본문 미리보기)`. 에디터 상단에 **선택 본문 읽기 전용 미리보기 박스** 표시(어떤 본문을 보고 쓰는지 유지 — QT 노트와 동일 맥락). 저장은 기존 설교 노트 경로(verseIds=note_verses).

## 범위/주의
- **bible 화면(목차·본문)은 이지윤(DevA) 담당 → PR 리뷰 필요.** note 에디터는 본인(DevD) 담당.
- 목차의 절 탭-탭 선택은 이제 "포커스 절(진입점)" 지정 용도이고, 노트용 범위 선택은 본문 화면에서 한다.
- **해설 버튼**: 코드 정상(승인 해설 없으면 비활성). 런타임 확인 결과 `verse_explanations` **0건**·시드 부재(로컬·origin/dev 모두)라 모든 범위에서 비활성. 클릭 가능하게 하려면 dev 해설 시드가 별도로 필요 — **분리 작업**(study/ai 백엔드).

## 검증
- `flutter analyze lib/features/bible lib/features/note` 무이슈.
- (권장) 에뮬레이터 수동 확인: 목차에서 장+절 선택 → 조회 → 본문 장 전체 + 포커스 스크롤, 절 탭-탭 범위, '노트 작성하기' → 설교 노트에 본문 동봉.

담당: DevD 이승욱
