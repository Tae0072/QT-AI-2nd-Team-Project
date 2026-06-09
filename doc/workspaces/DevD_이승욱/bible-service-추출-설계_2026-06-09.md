# bible-service 추출 설계 (MSA Phase 1) — 2026-06-09

> 첫 **사용자 서비스 추출**. today-qt·note·sharing·mypage 4개가 따를 **템플릿**이므로 경계·DB·인증 패턴을 여기서 확정한다.
> 작성: DevD 이승욱 (Lead 강태오 계정) · 협의 대상: bible 오너 그룹(김지민·이승욱·이지윤) + Lead
> 근거: MSA 거버넌스 결정(2026-06-08), `MSA분리계획_2026-06-08.md`, CLAUDE.md §3~§8

## 1. 대상 도메인 현황 (모놀리식 `com.qtai.domain.bible`)

- **성격**: **읽기 전용 참조 데이터**(books/verses). 쓰기 API·도메인 이벤트 없음 → **outbox/Kafka 불필요**.
- **공개 계약(api/UseCase)** = 추출 시 HTTP 경계가 될 4종:
  - `GetBibleVerseUseCase`, `ListBibleBooksUseCase`, `ListChaptersUseCase`, `SearchBibleUseCase`
  - DTO: `BibleBookResponse`, `BibleVerse(Book/Range)Response`, `BibleSearchRequest`
- **HTTP 라우트**: `GET /api/v1/bible/books`, `GET /api/v1/bible/verses` (`/api/v1/bible/**`)
- **internal**: `BibleBook`/`BibleVerse`(Entity), `BibleBookRepository`/`BibleRepository`, `BibleService`
- **테이블**: `bible_books`, `bible_verses` (+ source 메타). 시드 `V21__seed_bible_verses.sql`
- **소비자(타 도메인 → bible.api/UseCase 호출)**: `qt`(BibleBookLookup·QtTodayPassageImportService·TodayQtRangeMapper, 최다), `note`(NoteService), `study`(StudyService), `ai`(ExplanationGenerationJobHandler)

## 2. 핵심 설계 결정

1. **동기 경계 = 기존 api/UseCase 4종을 그대로 HTTP화**. 소비자 도메인은 `client/bible/{UseCase}HttpAdapter`(같은 인터페이스 구현)가 bible-service를 호출 → **소비자 코드의 인터페이스 의존 불변**(CLAUDE.md §4).
2. **이벤트 없음**: 참조 데이터라 발행 이벤트 없음. 시드는 bible-service 배치/마이그레이션 소유로 이전.
3. **DB-per-service**: `bible_books`·`bible_verses`를 bible-service 전용 스키마로. **주의 — 크로스 도메인 FK**: `study.glossary_terms.bible_verse_id → bible_verses(id)` 존재. DB 분리 시 이 **FK 제약을 제거**하고 `bible_verse_id`는 제약 없는 참조 값으로 유지(study가 무결성 보장 책임).
4. **인증**: 게이트웨이가 JWT 검증 후 `X-Member-Id`/`X-Member-Role` 주입(#364). bible-service는 헤더를 신뢰하고 인증된 역할만 허용(앱 콘텐츠 API 보호, CLAUDE.md §5). 게이트웨이 미경유 직접 호출은 차단 전제.
5. **금지 데이터 유지**: 본문은 KJV/KRV(허용)만. **개역개정·ESV·NIV·성서유니온·두란노 본문 저장 금지**(CLAUDE.md §8). 시드 이전 시 동일 정책. (현재 작업트리 untracked `data/bible-json/KorRV.json` 금지 여부 별도 확인 필요.)
6. **라우트 컷오버**: 게이트웨이 `/api/v1/bible/**` → `bible-service` URI(나머지는 모놀리식 유지). bible는 전용 prefix라 분기 깔끔.

## 3. 증분 계획 (PR 단위, ≤500 lines/10 files 지향)

| Inc | 범위 | 트래픽/리스크 | 비고 |
|-----|------|---------------|------|
| **1** | bible-service **스캐폴드** — 앱 + Persistence/Inbound Configuration + bible `api/UseCase`·DTO·web 이전, H2 테스트 | 트래픽 없음(skeleton) | ai-service 패턴 동일. DB 소유권·inbound 계약 확립 |
| **2** | 게이트웨이 **라우트 분기** `/api/v1/bible/**` → bible-service + 스모크/라우트 테스트 | 실트래픽 컷오버 | 모놀리식 bible-web과 병존(롤백 용이) |
| **3** | 모놀리식 소비자(qt·note·study·ai)를 **bible HTTP 어댑터**로 전환 + 계약 테스트(provider/consumer) | 내부 호출 경로 변경 | 인터페이스 불변, Mock→HTTP |
| **4** | **DB-per-service** — bible 전용 스키마/데이터소스, 시드 이전, **glossary_terms FK 제거** | 데이터 경계 분리 | 가장 신중. 마이그레이션 순서 주의 |
| **5** | 모놀리식에서 **bible 도메인 제거**(Strangler 완료) + ArchUnit 경계 갱신 | 모놀리식 축소 | 회귀 테스트 필수 |

## 4. 위험 / 협의 필요

- **소비자 4개 도메인 동시 영향**(특히 qt) — Inc3에서 한 번에 바꾸지 말고 도메인별로 쪼갤지 검토.
- **glossary_terms FK 제거**는 study 오너(김태혁) 협의 필요.
- **시드 데이터 이전·번역본 정책** — bible 오너(이지윤) 협의.
- bible-service 인증·헤더 신뢰 모델은 이후 4개 서비스 공통 → 여기서 확정.
- **Inc2 전제(보안)**: bible-service는 게이트웨이 미경유 직접 호출을 deny-by-default로 차단(`X-Member-Id`·`X-Member-Role` 필수 + 선택적 `X-Gateway-Token` 공유 토큰). 라우트 컷오버(Inc2) 시 **게이트웨이가 `X-Gateway-Token`을 주입**하도록 설정해 2차 방어선을 활성화한다(공유 토큰 = env 주입). 이 토큰 정책도 이후 서비스 공통.

## 5. 첫 PR(Inc1) 산출물 예정

- `qtai-server/bible-service/`(build.gradle.kts, `BibleServiceApplication`, Persistence/Inbound Configuration, `api/UseCase`·DTO·`web/BibleController` 이전), `settings.gradle.kts`에 `:bible-service` 추가
- 테스트: contextLoads 스모크 + (가능 시) BibleController slice. 테스트 키 런타임 주입 패턴 재사용.
- 모놀리식 bible 도메인은 **이 단계에서 제거하지 않음**(Inc5까지 병존).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업) · bible 오너 그룹·Lead 리뷰 요청
