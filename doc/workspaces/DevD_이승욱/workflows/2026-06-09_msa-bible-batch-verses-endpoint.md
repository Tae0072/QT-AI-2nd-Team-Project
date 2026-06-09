# 2026-06-09 MSA Phase 1 — bible-service verse 배치 조회 엔드포인트 (Inc3c-1)

## 목표
소비자 HTTP 어댑터 전환(Inc3c)의 **엔드포인트 커버리지 갭**을 메운다. note 등이 쓰는 `getVerses(List<Long> verseIds)`(절 ID 배치 조회)가 bible-service HTTP에 노출돼 있지 않아, 해당 엔드포인트를 추가한다.

## 배경 — 엔드포인트 커버리지 조사 결과
모놀리식 소비자(ai 제외)의 bible api 사용 현황:
| 메서드 | HTTP | 소비자 |
|---|---|---|
| `listBibleBooks()` | ✅ `/books` | qt |
| `getVerses(bookCode,chapter,from,to)` 범위 | ✅ `/verses` | qt |
| **`getVerses(List<Long> ids)` 배치** | ❌ 부재 → **본 작업으로 추가** | note |
| `getVerse(Long)`·`ListChapters`·`SearchBible` | ❌ | cross-domain 미소비(어댑터 불요) |
→ qt는 즉시 HTTP 전환 가능, note는 배치 엔드포인트가 선결.

## 작업 내용
- `BibleController`에 `GET /api/v1/bible/verses/by-ids?ids=1,2,3` 추가 → `getBibleVerseUseCase.getVerses(ids)`. 읽기 전용·멱등이라 GET + ids 쿼리(`List<Long>` 바인딩). UseCase/구현(BibleService)은 이미 존재 → 컨트롤러 노출만.
- bible-service의 BibleController만 변경(모놀리식 copy는 in-process 호출이라 HTTP 엔드포인트 불요 — 병존 중 의도된 divergence).

## 범위
- 브랜치: `feature/msa-bible-batch-verses-endpoint` (base: `dev`)
- 변경: BibleController(+엔드포인트 1) + 슬라이스 테스트(신규 2). 코어·게이트웨이·소비자 무변경.
- 관련: bible 추출 Inc3c-1 (note 소비자 HTTP 전환의 provider 선결)

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures**: `BibleVersesByIdsSliceTest` 2(신규 — ids 배치 200+data 2건 / 무헤더 401) + 기존 전부
- GET `/verses/by-ids?ids=10,11` + 게이트웨이 신원 헤더 → 200 + 절 2건. 헤더 없음 → 401(필터).

## 미해결 / 후속
- **API 명세 동기화**: `04_API_명세서.md`에 `/api/v1/bible/verses/by-ids` 반영 필요(Lead 소유 문서 — 별도 갱신/협의).
- **Inc3c-2**: `external/bible` HTTP 클라이언트 + 어댑터(`ListBibleBooksUseCase`+`GetBibleVerseUseCase`, `@Primary` gated `qtai.bible.client.mode=http`, 기본 inprocess=무변경). `getVerse(Long)`은 미노출이라 어댑터에서 `UnsupportedOperationException`(cross-domain 미소비).
- Inc3d(qt/note 소비자 전환, 오너 협의) → Inc4(DB 분리) → Inc5(모놀리식 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
