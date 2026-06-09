# 2026-06-09 모놀리식 external/bible HTTP 클라이언트·어댑터(Inc3c-2) — 결과 보고

## 요약
모놀리식이 bible를 HTTP로 호출할 수 있는 Strangler 어댑터를 추가했다. 기본 inprocess=동작 무변경, `mode=http`로 전환 시 `external/bible` 어댑터가 `@Primary`로 in-process `BibleService`를 오버라이드한다. SYSTEM 토큰 인증·envelope 언랩·오류 역매핑까지 포함. 6 코드 + yml + 테스트 2.

## 산출물

| 파일 | 설명 |
|------|------|
| `external/bible/BibleServiceClient.java` | RestClient — X-Gateway-Token 주입, envelope 언랩, 오류 ErrorCode 역매핑→BusinessException |
| `external/bible/ListBibleBooksHttpAdapter.java` | `ListBibleBooksUseCase` HTTP 구현 |
| `external/bible/GetBibleVerseHttpAdapter.java` | `GetBibleVerseUseCase` HTTP 구현(단건은 배치 위임) |
| `external/bible/BibleHttpClientConfiguration.java` | `@ConditionalOnProperty mode=http` → 어댑터 `@Primary` 등록 |
| `external/bible/BibleClientProperties.java` | `qtai.bible.client`(mode·base-url·gateway-token·timeout) |
| `src/main/resources/application.yml` | `qtai.bible.client` 토글(기본 inprocess) |
| `external/bible/*Test.java` | 클라이언트 3 + 어댑터 2 |

## 변경 성격
- **Strangler 소비자 어댑터(기본 off)**: `mode=http`에서만 어댑터 `@Primary` 등록 → 소비자(qt/note/study) 코드(인터페이스 의존) 무변경으로 호출만 HTTP 우회. 기본 inprocess라 동작 무변경.
- **예외 계약 보존**: 오류 응답을 `ErrorCode` 역매핑한 `BusinessException`으로 변환 → in-process와 동일한 예외(404 BIBLE_VERSE_NOT_FOUND 등). 소비자 에러 처리 무변경.
- **SYSTEM 인증**: 사용자 컨텍스트 없는 배치/캐시 호출이라 X-Gateway-Token만 주입(bible-service SYSTEM 주체).
- 토큰·base-url은 env 주입(평문 키 금지). bible/코어/소비자 무변경.

## 리뷰 후속 보강(컷오버 전)
- **빈 입력 방어**: `getVerses(빈/null)`은 네트워크 호출 없이 빈 결과 — in-process 계약과 동일(불필요 400 회피).
- **커버리지 보강**: 범위 조회(`getVerses(range)`) 쿼리·언랩, 빈 입력, `ListBibleBooksHttpAdapter` 위임 테스트 추가.
- (추적) **재시도/Circuit Breaker**: HTTP 호출 회복탄력성은 Inc3d 컷오버 전 도입 — 운영 진입 체크리스트에 추적(설계 사안). `BibleHttpClientConfiguration` 활성화 컨텍스트 테스트도 컷오버 시 통합으로.

## 검증
- `gradlew :test --tests "com.qtai.external.bible.*"` — **0 failures (8건)**: 클라이언트 5(토큰·books 언랩·범위·빈입력·오류 역매핑) + 어댑터 3(단건 위임·미발견·목록 위임)
- 전체 회귀·통합은 CI(기본 inprocess라 영향 없음).

## 미해결
- Inc3d(`mode=http` 컷오버 + 소비자 계약 테스트 + 재시도/CB, 토큰 동기화) → Inc4(DB 분리) → Inc5(모놀리식 제거).
