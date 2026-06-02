# 2026-06-02 Flutter 성경/QT API 연동 리포트

## 요약

`feature/flutter-bible-api-integration` 브랜치에서 Flutter 홈 탭의 QT 화면을 구현했다.

서버 `GET /api/v1/qt/today`가 권/장/절 범위(`range`)를 내려주도록 보강하고, Flutter는 그 범위를 기준으로 실제 본문을 우리 서버 API에서 조회하도록 연결했다. 성경 본문 텍스트를 외부 사이트에서 가져오거나 문서에 저장하지 않았다.

## 변경 파일 적용내용

| 파일 | 내용 |
| --- | --- |
| [bible_reference.dart](../../../../flutter-app/lib/features/bible/models/bible_reference.dart) | 성서유니온 본문 범위 파서 추가 |
| [bible_models.dart](../../../../flutter-app/lib/features/bible/models/bible_models.dart) | 성경 권/절/오늘 QT 화면 모델 추가 |
| [bible_repository.dart](../../../../flutter-app/lib/features/bible/services/bible_repository.dart) | `/bible/books`, `/bible/verses` 호출 추가 |
| [bible_providers.dart](../../../../flutter-app/lib/features/bible/providers/bible_providers.dart) | Repository와 오늘 QT 본문 Provider 추가 |
| [today_qt_screen.dart](../../../../flutter-app/lib/features/bible/screens/today_qt_screen.dart) | 홈 탭 QT 화면 추가 |
| [home_screen.dart](../../../../flutter-app/lib/features/home/screens/home_screen.dart) | 홈 placeholder를 QT 화면으로 교체 |
| [TodayQtRangeResponse.java](../../../../qtai-server/src/main/java/com/qtai/domain/qt/api/dto/TodayQtRangeResponse.java) | 오늘 QT 권/장/절 범위 응답 DTO 추가 |
| [TodayQtResponse.java](../../../../qtai-server/src/main/java/com/qtai/domain/qt/api/dto/TodayQtResponse.java) | `range` 필드 추가 |
| [QtPassageRepository.java](../../../../qtai-server/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java) | `qt_passages`와 `bible_books` 조인 조회 추가 |
| [flutter bible 테스트](../../../../flutter-app/test/features/bible/) | parser, repository, screen 테스트 추가 |
| [workflow 문서](../workflows/2026-06-02_flutter-bible-api-integration.md) | 작업 workflow 작성 |
| [리포트 문서](./2026-06-02_flutter-bible-api-integration_report.md) | 작업 리포트 작성 |

## 동작 흐름

1. Flutter `TodayQtScreen` 진입
2. `BibleRepository.getTodayQt()`가 `/api/v1/qt/today` 호출
3. 서버 응답 `range.bookCode`, `range.chapter`, `range.verseFrom`, `range.verseTo` 확인
4. `BibleRepository.getVerses()`가 `/api/v1/bible/verses` 호출
5. 화면에 `고린도전서 1:10-17`, QT 제목, 절 목록 표시
6. `range`가 없으면 기존 성서유니온 표기 fallback 파싱 사용

## 검증

| 명령 | 결과 |
| --- | --- |
| `flutter test test/features/bible/models/bible_reference_parser_test.dart test/features/bible/services/bible_repository_test.dart` | 통과 |
| `flutter analyze` | 통과, issues 없음 |
| `flutter test test/features/bible` | 통과 |
| `flutter test` | 통과 |
| `./gradlew.bat test --tests com.qtai.domain.qt.internal.QtPassageRepositoryTest --tests com.qtai.ApplicationContextLoadTest --tests com.qtai.domain.qt.web.QtControllerTest --tests com.qtai.domain.qt.internal.QtPassageLookupTest --tests com.qtai.domain.qt.internal.QtServiceTest` | 통과 |

## 기준 준수

| 기준 | 결과 |
| --- | --- |
| 외부 사이트 본문 텍스트 저장 금지 | 준수 |
| 권/장/절만 파싱 | 준수 |
| 우리 DB/API에서 본문 조회 | 준수 |
| 금지 번역본 본문 데이터 미사용 | 준수 |
| Flutter 기존 Dio/Riverpod 패턴 사용 | 준수 |
| `OLD`/`NEW` 기준 | 서버 `range.testament`와 테스트에 반영 |

## 남은 작업

- 해설, 시뮬레이터, 노트 버튼은 `qtPassageId`와 각 화면 라우트가 확정된 뒤 연결한다.

## 2026-06-02 추가 구현: 성서유니온 오늘 본문 변경 자동 반영

서버에 성서유니온 오늘 본문 수집 흐름을 추가했다.

- `SuTodayBibleClient`: `https://sum.su.or.kr:8888/bible/today` HTML 조회
- `SuTodayPassageParser`: 제목과 `권(영문) 장:절-절` 범위만 파싱
- `QtTodayPassageImportService`: `bible_books.english_name`으로 `book_id` 조회 후 `qt_passages` 생성/갱신
- `SuTodayPassageImportScheduler`: 매일 00:05 KST 자동 실행
- 저장 성공 시 `todayQt` 캐시 삭제

주의 기준:

- 성서유니온 본문 텍스트는 저장하지 않는다.
- 성서유니온에서 본문 제목/범위가 바뀌면 서버 배치가 다음 실행 시 `qt_passages`를 같은 날짜 기준으로 갱신한다.
- Flutter는 기존처럼 `/api/v1/qt/today`의 `range`를 받아 `/api/v1/bible/verses`를 호출하면 된다.
- v1은 같은 장 안의 범위만 지원한다.

## 2026-06-02 추가 확인: 에뮬레이터 더미 본문 표시

퇴근 전 Flutter 에뮬레이터에서 파싱/API/더미 본문 표시까지 확인했다.

### 목적

- 실제 성경 본문 seed가 이승욱님 작업으로 들어오기 전, 이지윤 Flutter QT 화면의 API 연결 흐름을 먼저 확인한다.
- 확인 범위는 오늘 성서유니온 본문 범위인 `고린도전서(1 Corinthians) 1:10-17`로 제한한다.
- 더미 본문은 실제 번역본 본문이 아니며, 금지 번역본 본문을 포함하지 않는다.

### 추가한 임시 데이터

- [V23__seed_dummy_today_qt_1co_verses.sql](../../../../qtai-server/src/main/resources/db/migration/V23__seed_dummy_today_qt_1co_verses.sql)
- `qt_passages`에 `2026-06-02`, `고린도전서 1:10-17` 임시 QT 1건 추가
- `bible_verses`에 고린도전서 1장 10-17절 더미 8건 추가
- 파일 상단에 다음 삭제 안내 주석을 추가했다.

```sql
-- 이승욱님 seed 실제데이터입력시 해당 더미데이터 삭제바람.
```

### 추가 수정

- [V7__seed_bible_books.sql](../../../../qtai-server/src/main/resources/db/migration/V7__seed_bible_books.sql)의 `testament` 값을 팀 명세 기준인 `OLD`/`NEW`로 맞췄다.
- 기존 `OT`/`NT` 값은 서버 `BibleBook.Testament` enum과 맞지 않아 `/api/v1/bible/verses` 조회 시 500 오류가 발생했다.
- Flutter 개발 확인용으로 `--dart-define=DEV_FORCE_HOME=true` 실행 시 홈/QT 화면에 바로 진입하는 dev 전용 우회 플래그를 추가했다.

### 확인한 API 응답

- `GET /api/v1/qt/today`
  - `range.testament=NEW`
  - `range.bookCode=1CO`
  - `range.chapter=1`
  - `range.verseFrom=10`
  - `range.verseTo=17`
- `GET /api/v1/bible/verses?bookCode=1CO&chapter=1&verseFrom=10&verseTo=17`
  - `한글 테스트 본문 10`부터 `한글 테스트 본문 17`까지 반환
  - `English dummy verse 10`부터 `English dummy verse 17`까지 반환

### 에뮬레이터 확인

- Android emulator: `emulator-5554`
- Flutter 실행 옵션: `--dart-define=DEV_FORCE_HOME=true`
- 화면 확인 결과:
  - 상단 제목 `고린도전서 1:10-17` 표시
  - QT 제목 `성서유니온 파싱 확인용 더미 QT` 표시
  - 한글/영어 더미 본문 8절 리스트 표시
- 스크린샷: [qtai_today.png](../../../../flutter-app/build/codex-run/qtai_today.png)

### 실행 검증

| 명령 | 결과 |
| --- | --- |
| `./gradlew.bat test --tests com.qtai.common.BibleBookSeedContractTest --tests com.qtai.common.DummyBibleSeedContractTest --tests com.qtai.MysqlMigrationValidationTest` | 통과 |
| `flutter analyze` | 통과 |
| `flutter test test/app_route_resolver_test.dart test/features/bible` | 통과 |

### 내일 정리 필요

- 이승욱님이 실제 `bible_verses` seed/import를 넣으면 [V23__seed_dummy_today_qt_1co_verses.sql](../../../../qtai-server/src/main/resources/db/migration/V23__seed_dummy_today_qt_1co_verses.sql)은 삭제한다.
- 실제 seed가 들어온 뒤에는 같은 API와 에뮬레이터 화면에서 실제 본문이 표시되는지 다시 확인한다.
