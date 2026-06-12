# 2026-06-12 성경 본문 장 전체 보기 + 범위 선택 → 설교 노트 연동 — 결과 보고

## 요약
성경 본문 페이지를 "선택 범위만 표시"에서 **장 전체 표시 + 선택 절 포커스**로 바꾸고, 본문에서 절을 탭-탭으로 **범위 지정** 후 '노트 작성하기'로 **설교 노트 작성 화면에 선택 본문을 동봉**(verseIds + 참조·미리보기)하도록 연동했다. 오늘의 QT 노트 흐름과 동일한 맥락이다. 해설 버튼은 코드 정상이며, 승인 해설 데이터가 DB에 없어 비활성임을 런타임으로 확인했다(분리 후속).

## 산출물
| 파일 | 설명 |
|------|------|
| `features/bible/screens/bible_passage_screen.dart` | 장 전체 렌더 + 포커스 스크롤 + 절 탭-탭 범위 선택/하이라이트 + 하단 '노트 작성하기' 액션바 |
| `features/bible/screens/bible_browser_screen.dart` | `_search`를 장 전체(`getChapterVerses`) 조회 + 포커스 절 전달로 변경 |
| `features/note/screens/note_edit_screen.dart` | `NoteEditArgs`에 referenceText·versePreview 추가 + 작성 화면 상단 인용 미리보기 박스 |

## 검증
- `flutter analyze` 무이슈, `flutter test test/features/bible`(37건)·`test/features/note`(61건) 전부 통과
- (리뷰 보강) 신규 순수 로직 단위 테스트 추가 `passage_view_logic_test.dart`(포커스 보정·범위 라벨), 본문/목차 위젯 테스트는 새 생성자(chapter+focus)·`getChapterVerses` 흐름에 맞게 갱신
- (권장) 에뮬레이터 수동 워크스루: 목차 장+절 선택 → 본문 장 전체+포커스, 절 탭-탭 범위, 노트 작성하기 → 설교 노트 본문 동봉

## 리뷰 보강(머지 전, 동일 브랜치)
- **포커스 스크롤 신뢰성**: lazy 빌드로 먼 절에서 `ensureVisible` 실패 가능 → ListView `cacheExtent`를 키워 장 전체를 미리 빌드.
- **해설 가용성 1회 조회**: 절 선택마다 `/qt/passage-study` 재호출하던 것을 진입 포커스 기준 고정 ref로 **1회만** 조회하도록 변경.
- **신규 로직 단위 테스트**: 포커스 보정·범위 라벨을 순수 함수(`passage_view_logic.dart`)로 분리해 테스트 추가.

## 미해결 / 후속
- **bible 화면은 이지윤(DevA) 담당 → PR 리뷰 필요.**
- 해설 버튼 활성화는 승인 해설 데이터 의존 — 해설 DB import는 별도 완료(2026-06-12).
- 에뮬레이터 실기 확인은 사용자 환경에서 권장(빌드/설치는 미수행 — 현재 에뮬레이터의 다른 브랜치 빌드 보존).

담당: DevD 이승욱
