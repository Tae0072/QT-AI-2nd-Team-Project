# 2026-06-09 bible-service verse 배치 조회 엔드포인트(Inc3c-1) — 결과 보고

## 요약
소비자 HTTP 어댑터 전환의 엔드포인트 커버리지 갭을 메웠다. 커버리지 조사 결과 note가 쓰는 `getVerses(List<Long> verseIds)`가 HTTP 미노출이라, `GET /api/v1/bible/verses/by-ids` 엔드포인트를 bible-service에 추가했다. 컨트롤러 1 + 슬라이스 테스트 2.

## 엔드포인트 커버리지 조사
- 커버됨: `listBibleBooks()`(/books, qt), `getVerses(범위)`(/verses, qt)
- **갭 → 추가**: `getVerses(List<Long>)`(note) → `/verses/by-ids`
- cross-domain 미소비(어댑터 불요): `getVerse(Long)`, `ListChapters`, `SearchBible`

## 산출물

| 파일 | 설명 |
|------|------|
| `bible-service/.../web/BibleController.java` | `GET /verses/by-ids?ids=1,2,3` 추가 → `getVerses(List<Long>)` 노출 |
| `bible-service/.../BibleVersesByIdsSliceTest.java` | (신규) ids 배치 200+data / 무헤더 401 |

## 변경 성격
- **provider 갭 해소**: 기존 UseCase/구현(BibleService)을 HTTP로 노출만(로직 무변경). 읽기 전용·멱등 → GET + ids 쿼리.
- bible-service 컨트롤러만 변경(모놀리식 copy는 in-process라 미변경 — 병존 중 의도된 divergence).
- 인증 필터 뒤에서 동작(신원 헤더 200 / 무헤더 401) 확인.

## 입력 안전성 보강(리뷰 후속)
- **개수 상한**: `MAX_VERSE_IDS=200` 초과 시 400(C0002).
- **빈/누락 입력**: `ids` 없음/빈 → 400(C0002).
- **매치 정책 명시 + 500 버그 수정**: 도메인 `getVerses(List<Long>)`는 **all-or-nothing**(요청 ID 중 하나라도 없으면 `BIBLE_VERSE_NOT_FOUND` throw)인데, bible-service엔 GlobalExceptionHandler가 없어 **500이 누출되던 버그**를 발견 → 컨트롤러에서 `BusinessException`을 잡아 ErrorCode 상태(404 B0002)·표준 envelope로 변환. (부분 매치 아님을 javadoc·테스트로 명시.)

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures** (BibleVersesByIdsSliceTest **5**: 배치 200 / 무헤더 401 / 누락 ID 404 / 빈 입력 400 / 초과 400)

## 미해결
- API 명세(`04_API_명세서.md`) 동기화 — Lead 소유 문서, 별도 협의.
- Inc3c-2(external/bible HTTP 클라이언트+어댑터, 기본 inprocess) → Inc3d(소비자 전환) → Inc4(DB 분리) → Inc5(모놀리식 제거).
