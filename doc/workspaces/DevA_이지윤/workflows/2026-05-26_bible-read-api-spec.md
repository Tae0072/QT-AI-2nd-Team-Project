# Workflow - 2026-05-26 bible-read-api-spec

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/bible-read-api` |
| PR 대상 | `dev` |
| 관련 F-ID | F-01, F-03, F-08, F-16 |
| 트리거 | 성경 데이터 모델과 사용자 조회 API 구현 전에 `books`, `verses`, `verse_explanations`의 소유 경계, DTO, 검증 기준을 고정한다. |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `qtai-server/02_ERD_문서.md`, `CODE_CONVENTION.md`, `data/bible-sources/README.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/bible/**`, `qtai-server/src/main/java/com/qtai/domain/study/**`, `qtai-server/src/main/resources/db/migration/**`, `qtai-server/src/test/java/com/qtai/domain/bible/**`, `qtai-server/src/test/java/com/qtai/domain/study/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

성경 권 목록과 절 범위 조회가 `07_요구사항_정의서.md` F-01, `04_API_명세서.md` §4.2.1~§4.2.2, `25_기능_명세서.md` F-03/F-16의 `@`멘션 본문 자동 삽입 흐름을 만족하도록 데이터 모델과 API 계약을 정리한다. 구현 결과는 로그인 사용자가 `/api/v1/bible/books`, `/api/v1/bible/verses`를 안정적으로 호출할 수 있고, 다른 도메인이 성경 절을 직접 DB 조회하지 않고 `domain.bible.api` UseCase로 조회할 수 있어야 한다.

`verse_explanations`는 `bible_verses`에 연결된 절별 해설 테이블이지만 사용자 노출 API는 `GET /api/v1/qt/{qtPassageId}/study-content` 경계에 둔다. 이번 명세는 `verse_explanations` 데이터 모델의 승인본 노출 기준을 고정하되, `/api/v1/bible/**`가 해설 본문을 직접 반환하지 않도록 한다.

## 범위

- `bible_books`, `bible_verses` 모델을 ERD/API 기준 컬럼명으로 정합화한다.
- `GET /api/v1/bible/books`를 구현하고 `displayOrder` 기준으로 성경 권 목록을 반환한다.
- `GET /api/v1/bible/verses?bookCode=&chapter=&verseFrom=&verseTo=`를 구현하고 장 전체, 단일 절, 절 범위 조회를 지원한다.
- `verseFrom` 단독 지정은 단일 절 조회로 처리한다.
- `verseFrom > verseTo`, 1 미만 값, 상한 초과 범위는 `400 VALIDATION_ERROR`로 거절한다.
- 존재하지 않는 책, 장, 절은 `BIBLE_BOOK_NOT_FOUND` 또는 `BIBLE_VERSE_NOT_FOUND`로 거절한다.
- HTTP 응답은 공통 `ApiResponse.success(data)` envelope를 사용한다.
- `domain.bible.api`에 다른 도메인이 사용할 공개 UseCase와 DTO를 둔다.
- Controller는 UseCase만 호출하고 Repository를 직접 호출하지 않는다.
- `verse_explanations`는 `domain.study` 소유 read model로 정리하고, 사용자 응답은 `APPROVED`와 `active_unique_key='ACTIVE'` 기준만 허용한다.
- OpenAPI에는 `/api/v1/bible/books`, `/api/v1/bible/verses`의 query parameter, 성공 응답, 실패 코드를 반영한다.
- 테스트 데이터에는 실제 성경 본문 문장을 넣지 않고, 의미 없는 더미 문자열만 사용한다.

## 제외 범위

- 성경 원문 JSON, 가공 JSON, SQLite DB, seed SQL, fixture 파일 추가는 제외한다.
- 승인된 성경 출처가 없으므로 한글 번들 다운로드 API의 본문 제공 구현은 제외한다.
- `/api/v1/bible/bundle` 구현은 `data/bible-sources/README.md`에 승인 후보가 생긴 뒤 별도 workflow로 진행한다.
- 성경 검색 API, FULLTEXT 인덱스, 키워드 검색은 제외한다.
- 관리자 성경 데이터 등록/수정 API는 제외한다.
- QT Today API, 노트 저장 API, `note_verses` 동기화 구현은 제외한다.
- 프로젝트 금지 번역본과 외부 큐레이션 본문 텍스트는 seed, fixture, 응답 예시, 테스트 데이터 어디에도 추가하지 않는다.
- `verse_explanations`를 `domain.bible.web` 응답에 직접 포함하는 API 신설은 제외한다. 필요하면 Lead 검토 후 API 명세를 먼저 변경한다.

## 모델/API 계약 명세

### 데이터 모델

| 테이블 | 소유 도메인 | 핵심 컬럼 | 구현 기준 |
| --- | --- | --- | --- |
| `bible_books` | `domain.bible` | `id`, `testament`, `code`, `korean_name`, `english_name`, `display_order` | `code`와 `display_order`는 unique로 유지한다. |
| `bible_verses` | `domain.bible` | `id`, `book_id`, `chapter_no`, `verse_no`, `korean_text`, `english_text` | `(book_id, chapter_no, verse_no)` unique 제약과 `(book_id, chapter_no)` 조회 인덱스를 둔다. |
| `verse_explanations` | `domain.study` | `id`, `bible_verse_id`, `summary`, `explanation`, `source_label`, `status`, `active_unique_key`, `ai_asset_id`, `approved_at` | 사용자 노출은 `status=APPROVED`와 `active_unique_key='ACTIVE'`인 현재 승인본만 허용한다. |

현재 `V2__create_bible.sql`과 `BibleVerse`는 `chapter`, `verse`, `krv_text`, `kjv_text`를 사용한다. 구현 PR에서는 ERD와 API 명세에 맞춰 `chapter_no`, `verse_no`, `korean_text`, `english_text`로 정합화한다. 이미 적용된 DB를 대상으로 하는 환경이면 기존 V2 수정 대신 신규 보정 마이그레이션을 사용한다.

### 공개 UseCase

| UseCase | 패키지 | 책임 |
| --- | --- | --- |
| `ListBibleBooksUseCase` | `com.qtai.domain.bible.api` | 성경 권 목록을 `displayOrder` 순서로 반환한다. |
| `GetBibleVerseUseCase` | `com.qtai.domain.bible.api` | `verseId` 단건 조회와 `bookCode + chapter + verseFrom + verseTo` 범위 조회를 제공한다. |
| `ListApprovedVerseExplanationUseCase` | `com.qtai.domain.study.api` | QT 학습 콘텐츠 조립 시 승인된 절별 해설만 조회한다. 이번 PR에서 study API를 구현하지 않으면 인터페이스 추가 없이 study 작업 범위로 남긴다. |

`domain.qt`, `domain.note`, `domain.sharing`, `domain.ai`는 성경 절이 필요할 때 `domain.bible.api`만 import한다. `domain.bible.internal`, `domain.bible.web` 직접 import는 금지한다.

### HTTP API

| Method | Path | 인증 | 응답 data 타입 | 실패 코드 |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/bible/books` | USER | `List<BibleBookResponse>` | `401 UNAUTHORIZED`, `403 FORBIDDEN` |
| GET | `/api/v1/bible/verses` | USER | `BibleVerseRangeResponse` | `400 VALIDATION_ERROR`, `404 BIBLE_BOOK_NOT_FOUND`, `404 BIBLE_VERSE_NOT_FOUND`, `429 RATE_LIMIT_EXCEEDED` |

`GET /api/v1/bible/verses` query parameter는 아래 기준으로 고정한다.

| 파라미터 | 필수 | 기준 |
| --- | --- | --- |
| `bookCode` | 필수 | `bible_books.code`와 대소문자까지 일치해야 한다. |
| `chapter` | 필수 | 1 이상의 정수만 허용한다. |
| `verseFrom` | 선택 | 생략하면 장 전체 조회다. 지정하면 1 이상의 정수만 허용한다. |
| `verseTo` | 선택 | 생략하고 `verseFrom`만 있으면 단일 절 조회다. 지정하면 `verseFrom <= verseTo`를 강제한다. |

단일 요청의 최대 절 범위는 50절로 둔다. 장 전체 조회가 50절을 초과하는 경우에는 장 전체 조회를 허용하되, `verseFrom`/`verseTo`로 지정한 사용자 인용 범위가 50절을 초과하면 `VALIDATION_ERROR`로 거절한다.

### DTO

| DTO | 위치 | 필드 |
| --- | --- | --- |
| `BibleBookResponse` | `domain.bible.api.dto` | `id`, `testament`, `code`, `koreanName`, `englishName`, `displayOrder` |
| `BibleVerseRangeResponse` | `domain.bible.api.dto` | `book`, `verses` |
| `BibleVerseBookResponse` | `domain.bible.api.dto` | `code`, `koreanName`, `englishName`, `chapter` |
| `BibleVerseResponse` | `domain.bible.api.dto` | `id`, `bookCode`, `chapterNo`, `verseNo`, `koreanText`, `englishText` |
| `ApprovedVerseExplanationResponse` | `domain.study.api.dto` | `verseId`, `summary`, `explanation`, `sourceLabel`, `aiAssetId` |

테스트와 OpenAPI 예시의 `koreanText`, `englishText`, `summary`, `explanation`은 실제 성경 구절이나 실제 주석 문장이 아닌 더미 문자열만 사용한다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/resources/db/migration/V2__create_bible.sql` 또는 신규 보정 migration | `bible_verses` 컬럼명과 제약을 ERD/API 기준으로 정합화한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/bible/internal/BibleBook.java` | `bible_books` JPA 매핑을 유지하고 생성자/팩토리는 테스트 필요 범위만 추가한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/bible/internal/BibleVerse.java` | `chapterNo`, `verseNo`, `koreanText`, `englishText` 매핑과 `BibleBook` 연관을 정리한다. |
| Create | `qtai-server/src/main/java/com/qtai/domain/bible/internal/BibleBookRepository.java` | 성경 권 목록 조회를 담당한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/bible/internal/BibleRepository.java` | 성경 절 단건/범위 조회 쿼리를 담당한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/bible/internal/BibleService.java` | read-only 트랜잭션, 입력 검증, DTO 매핑, 예외 변환을 담당한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/bible/web/BibleController.java` | `/api/v1/bible/books`, `/api/v1/bible/verses` HTTP 엔드포인트를 구현한다. |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/bible/api/**` | 공개 UseCase와 DTO를 구현 계약에 맞게 정리한다. |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/study/**` | `verse_explanations` 승인본 read model은 study 경계에서만 다룬다. |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 성경 권 목록/절 조회 API 계약과 실패 코드를 반영한다. |
| Create | `qtai-server/src/test/java/com/qtai/domain/bible/**` | Controller, Service, Repository, JPA 매핑 테스트를 추가한다. |
| Create/Modify | `qtai-server/src/test/java/com/qtai/domain/study/**` | 승인 해설 노출 기준을 구현 범위에 맞춰 검증한다. |

## 구현 순서

1. `data/bible-sources/README.md`에 승인된 성경 원문 후보가 없는 상태임을 확인하고, 본문 데이터 추가 없이 구조/API 구현만 진행한다.
2. `V2__create_bible.sql`, `BibleVerse`, `qtai-server/02_ERD_문서.md`의 컬럼명 차이를 확인한다.
3. 적용 대상 DB가 초기 개발 DB인지 확인한다. 초기 개발 DB이면 V2를 ERD 기준으로 수정하고, 적용 이력이 있는 DB이면 신규 보정 migration을 작성한다.
4. `BibleBook`과 `BibleVerse` JPA 매핑을 `bible_books`, `bible_verses` 기준으로 맞춘다.
5. `(book_id, chapter_no, verse_no)` unique 제약과 `(book_id, chapter_no)` 인덱스를 migration에 반영한다.
6. `BibleBookRepository`와 `BibleRepository`를 Spring Data JPA repository로 구현한다.
7. `ListBibleBooksUseCase`, `GetBibleVerseUseCase`, `BibleBookResponse`, `BibleVerseRangeResponse`, `BibleVerseBookResponse`, `BibleVerseResponse`를 정리한다.
8. `BibleService`에 `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`를 적용한다.
9. `BibleService`에서 `bookCode`, `chapter`, `verseFrom`, `verseTo`를 검증하고 `BusinessException`으로 공통 오류를 반환한다.
10. `BibleController`에 `@RestController`, `@RequestMapping("/api/v1/bible")`, `@RequiredArgsConstructor`를 적용한다.
11. `GET /books`, `GET /verses` 핸들러가 `ResponseEntity<ApiResponse<...>>`를 반환하도록 구현한다.
12. `SecurityConfig`에서 `/api/v1/bible/**`가 로그인 사용자만 접근 가능한지 확인하고, 별도 permitAll을 추가하지 않는다.
13. `verse_explanations` 구현이 같은 PR에 포함되면 `domain.study`에만 read model을 두고 `APPROVED` 현재 승인본 필터를 적용한다.
14. `qtai-server/apis/api-v1/openapi.yaml`에 공통 envelope, query parameter, 실패 코드를 반영한다.
15. Controller 테스트에서 인증 사용자, 정상 목록, 정상 범위, 단일 절, `verseFrom > verseTo`, 미존재 책/절 케이스를 검증한다.
16. Repository 또는 JPA 테스트에서 unique 좌표, book/chapter 조회 순서, 컬럼 매핑을 검증한다.
17. 다른 도메인이 `domain.bible.internal` 또는 `domain.bible.web`를 import하지 않는지 ArchUnit 또는 `rg`로 확인한다.
18. 테스트 데이터와 API 예시에 실제 성경 본문 문장이 들어가지 않았는지 `rg`와 수동 확인을 함께 수행한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/bible/web/BibleControllerTest.java` | `/books`, `/verses` 성공 응답 envelope, query parameter 검증, 400/404 오류 응답 |
| `qtai-server/src/test/java/com/qtai/domain/bible/internal/BibleServiceTest.java` | 범위 기본값, 단일 절 처리, 범위 상한, 미존재 책/절 예외 |
| `qtai-server/src/test/java/com/qtai/domain/bible/internal/BibleRepositoryTest.java` | `(book_id, chapter_no, verse_no)` 좌표 조회, displayOrder 정렬 |
| `qtai-server/src/test/java/com/qtai/common/JpaEntityDdlTest.java` | 변경된 Bible entity와 migration 컬럼 정합성 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/VerseExplanationReadModelTest.java` | 같은 PR에서 study read model을 구현할 경우 `APPROVED` 현재 승인본만 반환 |
| `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | 다른 도메인의 `domain.bible.internal`/`web` import 금지 |

## 수용 기준

- [ ] `GET /api/v1/bible/books`가 로그인 사용자에게 공통 envelope로 성경 권 목록을 반환한다.
- [ ] `GET /api/v1/bible/verses`가 장 전체, 단일 절, 절 범위 조회를 지원한다.
- [ ] `verseFrom`/`verseTo` 입력 오류가 `400 VALIDATION_ERROR`로 반환된다.
- [ ] 존재하지 않는 책과 절이 각각 성경 도메인 404 오류로 반환된다.
- [ ] `BibleController`는 Repository를 직접 호출하지 않는다.
- [ ] 다른 도메인은 `domain.bible.api`만 import한다.
- [ ] `bible_verses` 모델 컬럼명이 ERD/API의 `chapter_no`, `verse_no`, `korean_text`, `english_text`와 일치한다.
- [ ] `(book_id, chapter_no, verse_no)` unique 제약이 존재한다.
- [ ] `verse_explanations`는 study 경계에서만 사용자 노출 기준을 갖고, 미승인 해설을 반환하지 않는다.
- [ ] 테스트/fixture/OpenAPI 예시에 실제 성경 본문이나 금지 번역본 데이터가 들어가지 않는다.
- [ ] 원본 JSON, seed SQL, SQLite DB, 가공 성경 데이터 파일을 커밋하지 않는다.
- [ ] OpenAPI와 실제 Controller 경로가 일치한다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- Bible API 구현, JPA/migration 정합화, OpenAPI 문서, 테스트 보강은 편집 경로를 분리하면 병렬 작업이 가능하다.
- 단, DTO 필드명과 오류 코드는 한 번 어긋나면 여러 파일에 파급되므로 메인 에이전트가 계약을 먼저 고정해야 한다.
- `verse_explanations`는 study 경계와 연결되므로 병렬 작업자가 맡더라도 `domain.bible` 내부를 직접 참조하지 않는지 최종 통합 검증이 필요하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | Bible Controller/Service/Repository 구현 | `qtai-server/src/main/java/com/qtai/domain/bible/**` |
| Worker 2 | Bible 테스트 보강 | `qtai-server/src/test/java/com/qtai/domain/bible/**`, `qtai-server/src/test/java/com/qtai/common/**` |
| Worker 3 | OpenAPI 계약 반영 | `qtai-server/apis/api-v1/openapi.yaml` |
| Worker 4 | Study 승인 해설 read model 검증 | `qtai-server/src/main/java/com/qtai/domain/study/**`, `qtai-server/src/test/java/com/qtai/domain/study/**` |

### 직접 실행 판단

메인 에이전트는 DTO/오류/경계 계약, migration 방향, 최종 테스트 실행, 금지 데이터 검사를 직접 통합 확인한다.

## 검증 계획

- `git diff --check`
- `./gradlew -p qtai-server test --tests "*Bible*"`
- `./gradlew -p qtai-server test --tests "*JpaEntityDdlTest"`
- `./gradlew -p qtai-server test --tests "*Architecture*"`
- `./gradlew -p qtai-server build`
- `./gradlew -p qtai-server test jacocoTestReport`
- `./gradlew -p qtai-server jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "com\\.qtai\\.domain\\.bible\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain --glob "*.java"`
- `rg -n "([[:alpha:]]+|[가-힣]+)[[:space:]]+[0-9]+:[0-9]+|^[[:space:]]*[0-9]+:[0-9]+" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml`
- `23_도메인_용어사전.md`의 금지 번역본 기준에 해당하는 이름이나 본문 데이터가 새 코드, 테스트, OpenAPI 예시에 추가되지 않았는지 수동 확인한다.

`spectral` 또는 `gitleaks` 실행 파일이 없는 환경에서는 설치 여부와 실패 사유를 PR 본문에 남기고, 실행 가능한 CI에서 같은 명령을 다시 수행한다.

## 후속 작업으로 남길 항목

- 승인된 성경 원문 후보 확정 후 `/api/v1/bible/bundle?language=ko&version=` 구현
- 성경 검색 API와 FULLTEXT 인덱스 설계
- 관리자 성경 데이터 등록/검수 API
- QT Today API에서 성경 절과 학습 콘텐츠를 조립하는 흐름 구현
- 노트 `@`멘션 파싱 후 `note_verses` 저장 동기화 구현
