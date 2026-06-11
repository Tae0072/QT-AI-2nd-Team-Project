# 2026-06-11 성경 본문 전체 페이지 전환 + 해설 진입점 + 노트 굵게 버그 (feature/flutter-bible-passage-page-explanation)

## 목표·배경
성경 탭 관련 3건: ① 저장된 노트 상세에서 굵게(B) 등 마크업이 적용되지 않던 버그, ② 본문 조회 결과를 작은 바텀시트가 아니라 전체 페이지로 표시, ③ 성경 본문에도 QT와 동일한 해설 진입점 노출(정식 서버 확장). dev pull 후 신규 브랜치.

## 작업 내용
### ① 노트 굵게 버그 (F-03)
- 원인: 편집기는 `QtNoteRichTextParser`로 라이브 렌더하지만 노트 상세(`note_detail_screen.dart`)는 평문 `Text`라 `**굵게**`·`==하이라이트==`·`[fg|bg|fs=..]` 마커가 그대로 노출.
- 수정: 상세 보기의 자유노트 본문 + 묵상 4섹션을 편집기와 동일 파서로 `Text.rich` 렌더(`_RichNoteText`).

### ② 본문 전체 페이지 (F-01)
- 조회 시 `showModalBottomSheet` 제거 → `BiblePassageScreen` 전체 페이지로 `Navigator.push`. Calm Paper 스타일, 영어 토글 유지.
- 죽은 코드(`_BibleResultPane`/`_BibleVerseTile`/시트 배선) 제거.

### ③ 해설 진입점 — 정식 서버 확장 (F-01/F-08)
- **모듈 순환 제약**: `qt`가 이미 `bible.api`에 의존 → `bible`이 `qt`를 호출하면 순환. 따라서 조합은 **qt 도메인**에서 수행(qt→bible, qt→study 의존은 허용). `BibleService`는 손대지 않음.
- 신규 `GET /api/v1/qt/passage-study?bookCode&chapter&verseFrom&verseTo` → 선택 범위를 포함하는 QT 본문을 찾아 그 절들에 승인(APPROVED·ACTIVE) 해설이 있으면 `{qtPassageId, hasExplanation}` 반환(없으면 NONE).
- 서버: `GetBiblePassageStudyUseCase`(api)·`BiblePassageStudy`(dto) + `QtService` 구현 + `QtPassageRepository.findContainingRange` + `BibleBookLookup.findBookIdByCode` + `QtController` 엔드포인트 + OpenAPI(api-v1) 경로/스키마.
- 프런트: `bible_repository.getBiblePassageStudy` + `biblePassageStudyProvider` + 전체 페이지가 이 가용성으로 해설 버튼 게이팅(조회 실패/미배포 시 버튼 숨김 → 안전).

## 범위
- 브랜치: `feature/flutter-bible-passage-page-explanation` (base: origin/dev 3c6705d9, 재설계 #526 포함).
- 커밋 3: fix(note) / feat(bible 프런트) / feat(qt 서버). 16 files (다영역 단일 요청이라 묶음; PR 본문에 사유 명시).
- ⚠️ qt/study/bible 백엔드는 타 담당(DevA 이지윤·AI)의 도메인 — 컨벤션 준수했으나 머지 전 도메인 오너 리뷰 권장.

## 검증
- `flutter analyze` (bible/note) 무이슈. `flutter test`(bible+note) 39건 + BiblePassageScreen 3건 통과.
- 서버: `./gradlew :service-bible:test` 전체 통과(QtServiceTest 3 신규 포함). OpenAPI YAML 파싱·$ref 검증 OK.

## 미해결 / 후속
- `data/bible-json/{KorRV,KJV}.json`(저장소 untracked, §8 금지 데이터로 보임) 별도 정리 — 본 작업 미포함.
- 실기기 수동 확인(해설 버튼 노출/네비게이션) 스크린샷 보강 권장.

담당: DevD 이승욱
