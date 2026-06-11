# 2026-06-09 MSA Phase 1 — 모놀리식 external/bible HTTP 클라이언트·어댑터 (Inc3c-2)

## 목표
모놀리식 소비자(qt/note/study)가 bible를 in-process 대신 bible-service HTTP로 호출할 수 있게 한다. **기본 inprocess=동작 무변경**, `mode=http`로 전환 가능한 Strangler 어댑터를 추가한다.

## 배경
- 설계: `bible-service-Inc3-소비자HTTP어댑터-설계_2026-06-09.md`(결정 A 빈 선택). Inc3c-1에서 provider 엔드포인트(`/books`·`/verses`·`/verses/by-ids`)가 갖춰짐.
- ai-service의 `client.mode=mock|http` + service-token 패턴과 동일 사상(모놀리식은 modern RestClient 사용).

## 작업 내용 (`com.qtai.external.bible`)
1. **BibleServiceClient** — RestClient로 bible-service 호출. **SYSTEM 호출**이라 사용자 헤더 없이 `X-Gateway-Token`만 주입(SYSTEM 주체 인증). 성공 응답은 `ApiResponse` envelope 언랩, **오류(4xx/5xx)는 error 코드를 `ErrorCode`로 역매핑한 `BusinessException`으로 변환**(in-process 예외 계약 보존 — 소비자 동작 무변경).
2. **어댑터** — `ListBibleBooksHttpAdapter`/`GetBibleVerseHttpAdapter`가 bible `api/UseCase`를 구현. 소비자 인터페이스 의존 불변. 단건 `getVerse(Long)`는 전용 엔드포인트가 없어 배치(`/verses/by-ids`)로 위임.
3. **구성** — `BibleHttpClientConfiguration`(`@ConditionalOnProperty mode=http`)이 클라이언트 + 어댑터를 **`@Primary`** 로 등록 → in-process `BibleService`를 오버라이드. `BibleClientProperties`(base-url·gateway-token·timeout). `mode!=http`면 미등록 → BibleService 사용(무변경).
4. **설정** — 모놀리식 `application.yml`에 `qtai.bible.client`(기본 `inprocess`, http 시 base-url·gateway-token·timeout env 주입).

## 범위
- 브랜치: `feature/msa-bible-http-client-adapter` (base: `dev`)
- 변경: `external/bible` 6파일 + application.yml + 테스트 2. 소비자(qt/note/study)·bible 도메인·코어 무변경.
- 관련: bible 추출 Inc3c-2

## 검증
- `gradlew :compileJava :test --tests "com.qtai.external.bible.*"` — **BUILD SUCCESSFUL / 0 failures (5건)**
  - `BibleServiceClientTest` 3(토큰 주입+books 언랩 / by-ids 쿼리+언랩 / 404 B0002→BusinessException(BIBLE_VERSE_NOT_FOUND) 역매핑)
  - `GetBibleVerseHttpAdapterTest` 2(단건→배치 위임 / 미발견→404)
- 기본 inprocess라 기존 동작 무변경(전체 회귀는 CI).

## 미해결 / 후속
- **Inc3d**: `mode=http` 전환 + 소비자(qt → note → study) 경로 계약/통합 테스트(오너 협의). 컷오버 시 bible-service `gateway.shared-token`과 `qtai.bible.client.gateway-token` 동일 값.
- `getVerse(Long)`·`ListChapters`·`SearchBible`은 cross-domain 미소비(어댑터 단건은 배치 위임으로 커버).
- Inc4(DB 분리) → Inc5(모놀리식 bible 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
