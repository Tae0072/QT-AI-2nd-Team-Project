# 2026-06-02 Flutter 성경/QT API 연동 workflow

## 1. 목적

이지윤 담당 Flutter 작업으로 홈 탭의 QT 화면을 구현한다.

성서유니온 오늘 본문 표기에서 권, 장, 절만 파싱하고, 앱은 본문 텍스트를 외부 사이트에서 가져오지 않는다. 파싱한 범위는 우리 `qtai-server`의 `/api/v1/bible/**` API에 전달해 DB 본문을 조회한다.

## 2. 담당 범위

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/flutter-bible-api-integration` |
| Flutter 영역 | `flutter-app/lib/features/bible/**`, 홈 탭 |
| 관련 서버 API | `GET /api/v1/qt/today`, `GET /api/v1/bible/books`, `GET /api/v1/bible/verses` |
| 기준 출처 | `https://sum.su.or.kr:8888/bible/today` |
| 오늘 본문 표기 | `고린도전서(1 Corinthians)1:10 - 1:17` |
| DB bookCode | `1CO` |

## 3. 고정 기준

- 팀 명세서 기준으로 성경 구분은 `OLD`/`NEW`를 사용한다.
- Flutter 화면은 외부 사이트 본문 텍스트를 저장하거나 노출하지 않는다.
- 성서유니온에서는 본문 텍스트가 아니라 권/장/절 범위만 참고한다.
- 금지 번역본(개역개정, ESV, NIV) 본문 텍스트를 seed, fixture, 문서, 테스트에 넣지 않는다.
- v1 범위는 단일 장 범위 파싱이다. 다중 장/드래그 선택은 v2로 둔다.

## 4. 구현 절차

1. 서버 `TodayQtResponse.range`에 `testament`, `bookCode`, `chapter`, `verseFrom`, `verseTo`를 추가한다.
2. Flutter는 `/qt/today`의 `range`를 우선 사용한다.
3. `range`가 있으면 `/bible/verses?bookCode=1CO&chapter=1&verseFrom=10&verseTo=17`를 바로 호출한다.
4. `range`가 없을 때만 `BibleReferenceParser`로 `고린도전서(1 Corinthians)1:10 - 1:17` fallback 표기를 파싱한다.
5. 홈 탭 placeholder를 `TodayQtScreen`으로 교체한다.
6. 로딩, 에러, 본문 목록 표시 상태를 구현한다.
7. parser, repository, screen widget 테스트를 추가한다.

## 5. 완료 기준

| 기준 | 완료 조건 |
| --- | --- |
| 파싱 | 권, 영어 권명, 장, 시작 절, 끝 절 추출 |
| API 연결 | `/qt/today` range 또는 fallback 파싱값으로 `bookCode=1CO`, `chapter=1`, `verseFrom=10`, `verseTo=17` 호출 |
| 화면 | 홈 탭에서 오늘 QT 범위와 절 목록 표시 |
| 테스트 | Flutter bible 테스트와 전체 Flutter 테스트 통과 |
| 문서 | workflow/report 작성 |

## 6. 후속 작업

- 해설, 시뮬레이터, 노트 버튼은 `qtPassageId` 연동이 확정된 뒤 실제 라우팅을 붙인다.

## 7. 2026-06-02 추가: 성서유니온 오늘 본문 자동 반영

- 서버가 매일 00:05 KST에 `https://sum.su.or.kr:8888/bible/today` HTML을 조회한다.
- 성서유니온 HTML에서는 제목과 `권(영문) 장:절-절` 범위만 파싱한다.
- 성서유니온 본문 텍스트는 저장하지 않는다.
- 파싱한 영문 권명은 DB `bible_books.english_name`으로 찾아 `book_id`로 변환한다.
- 오늘 날짜의 `qt_passages`가 없으면 생성하고, 이미 있으면 제목/권/장/절 범위를 갱신한다.
- 저장 성공 후에도 04:00 KST 전 사용자 노출 정책을 지키기 위해 `todayQt` 캐시는 즉시 비우지 않는다.
- `/api/v1/qt/today` 조회는 00:00~04:00 사이 오늘 데이터가 DB에 있어도 전일 본문을 `STALE_FALLBACK`으로 반환한다.
- 같은 장 안의 범위만 v1에서 지원한다. 장이 넘어가는 범위는 저장하지 않고 실패 로그를 남긴다.
