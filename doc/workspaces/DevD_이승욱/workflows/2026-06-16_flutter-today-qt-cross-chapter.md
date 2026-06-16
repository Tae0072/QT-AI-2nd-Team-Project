# 2026-06-16 Flutter 장 교차 오늘 QT 본문 절 조회 (bugfix/flutter-today-qt-cross-chapter)

## 목표·배경
#659가 백엔드에 장 교차 QT(예: 고린도전서 10:14-11:1)를 추가했으나 Flutter 앱은 본문 절을 **단일 장**으로 조회한다.
오늘 QT가 장 교차로 들어오자(SU import), 앱이 `chapter=10, verseFrom=14, verseTo=1`(끝 장 11 무시)로 요청해
`verseFrom(14) > verseTo(1)` → 서버 400(C0002), 본문을 못 불렀다. (`/api/v1/qt/today` 자체는 200 정상)
백엔드·QT영상 변경과 무관한, "백엔드는 장 교차를 넣었는데 앱이 안 따라간" 갭이다.

## 작업 내용
### ① 모델에 endChapter
- `TodayQtRange`: `endChapter` 추가(fromJson, 구 백엔드 호환으로 없으면 시작 장 폴백) + `isCrossChapter`.
- `BibleReference`: `endChapter`(선택) 추가 + displayText 교차 표기(`10:14-11:1`). `TodayQtRange.toReference()`가 endChapter 전달.

### ② 장 교차 절 조회 (bible_repository.dart)
- `getTodayQtPassage`: `isCrossChapter`면 `_getCrossChapterVerses`로, 아니면 기존 단일 장 `getVerses`.
- `_getCrossChapterVerses`: 장별로 `getChapterVerses`(장 전체)를 받아 시작 장은 verseFrom부터, 종료 장은 verseTo까지,
  중간 장은 전체를 모은다(백엔드 `collectRangeVerses`와 동일). 권 메타는 시작 장 응답 사용.

## 범위/주의
- 미사용 메서드 `getPassageFromReferenceText`(+ `BibleReferenceParser`의 장 교차 throw)는 호출처가 없어 이번 범위에서 제외.
- `BibleReference.endChapter`는 선택(nullable)이라 기존 단일 장 생성자는 그대로 동작(하위호환).
- 백엔드 변경 없음(Flutter 단독). `/bible/verses` 단일 장 계약을 그대로 쓰되 클라이언트가 장별로 합친다.

## 검증
- `flutter analyze lib/features/bible test/features/bible` 무이슈.
- `flutter test test/features/bible` 45건 통과(신규 "장 교차 10:14-11:1 → 14,15,1만" 포함).

담당: DevD 이승욱
