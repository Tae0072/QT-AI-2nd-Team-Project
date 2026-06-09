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

## 검증
- `gradlew :test --tests "com.qtai.external.bible.*"` — **0 failures (5건)**: 클라이언트 3(토큰·언랩·오류 역매핑) + 어댑터 2(단건 위임·미발견)
- 전체 회귀·통합은 CI(기본 inprocess라 영향 없음).

## 미해결
- Inc3d(`mode=http` 컷오버 + 소비자 계약 테스트, 토큰 동기화) → Inc4(DB 분리) → Inc5(모놀리식 제거).
