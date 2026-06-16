# 2026-06-16 Flutter 장 교차 오늘 QT 본문 절 조회 — 결과 보고

## 요약
#659가 백엔드에 장 교차 QT를 추가했지만 Flutter 앱이 본문 절을 단일 장으로 조회해, 장 교차 오늘 QT
(고린도전서 10:14-11:1)에서 `verseFrom(14) > verseTo(1)` → 400(C0002)로 본문을 못 불렀다. 앱이 `endChapter`를
인식하고, 장 교차면 장별로 조회해 경계를 필터링해 합치도록 고쳤다(백엔드 collectRangeVerses와 동일).

## 산출물
| 파일 | 설명 |
|------|------|
| `flutter-app/.../bible/models/bible_models.dart` | `TodayQtRange.endChapter`(폴백)·`isCrossChapter`·toReference 전달 |
| `flutter-app/.../bible/models/bible_reference.dart` | `BibleReference.endChapter`(선택)·교차 displayText |
| `flutter-app/.../bible/services/bible_repository.dart` | `getTodayQtPassage` 장 교차 분기 + `_getCrossChapterVerses` |
| `flutter-app/test/.../bible_repository_test.dart` | 장 교차(10:14-11:1) 조회·경계 필터·displayText 테스트 |

## 검증
- `flutter analyze lib/features/bible test/features/bible` 무이슈.
- `flutter test test/features/bible` 45건 통과(장 교차 신규 포함). 기존 단일 장 테스트는 endChapter 폴백으로 그대로 통과.

## 리뷰 보강(머지 전)
- `endChapter`는 fromJson에서 없으면 시작 장 폴백 → 구 백엔드/단일 장 응답 하위호환.
- `BibleReference.endChapter`는 nullable 선택 필드 → 기존 생성자/화면 표기 영향 없음(단일 장 동일 표기).
- 클라이언트 합치기는 백엔드 `collectRangeVerses`와 동일 경계 규칙(시작장 verseFrom~, 종료장 ~verseTo, 중간장 전체).

## 미해결 / 후속
- 미사용 `getPassageFromReferenceText`/`BibleReferenceParser`의 장 교차 미지원은 호출처가 없어 보류(필요 시 별도 정리).
- 노트 에디터·기타 화면의 reference 표기는 displayText 공통 사용이라 자동 반영됨.

담당: DevD 이승욱
