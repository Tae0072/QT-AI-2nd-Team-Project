# 스터디노트 — 배치(SYSTEM_BATCH) 서비스 간 호출과 시스템 토큰 RestClient 어댑터

작성일: 2026-06-10 / 작성: Claude (Lead 강태오)

## 배경: 사용자 호출 vs 배치 호출의 인증 차이

MSA에서 서비스 A가 서비스 B를 호출할 때 B는 JWT를 공유키로 검증한다. JWT를 어디서 얻느냐가 두 갈래로 갈린다.

- **사용자 맥락 호출**(예: 노트 화면 요청 → note→bible): 들어온 사용자 요청에 이미 `Authorization` 헤더가 있다. 그대로 전달하면 된다 → `ServiceCallAuthForwarder.forward()`.
- **배치/스케줄러 호출**(예: 해설 생성 job → ai→bible): 사람이 부른 요청이 아니라 **전달할 사용자 JWT가 없다.** 그래서 모든 서비스가 공유하는 시크릿으로 단명 토큰을 스스로 발급한다 → `SystemTokenProvider.issueSystemToken()`(HS256, sub=0, role=SYSTEM_BATCH).

수신 서비스의 `JwtAuthenticationFilter`는 먼저 사용자(RS256) 검증을 시도하고, 실패하면 시스템 토큰(HS256)으로 폴백 검증한다(PR #440). 둘 다 실패해야 401.

## 왜 `ObjectProvider`로 받는가

`SystemTokenProvider`는 `@ConditionalOnProperty(security.jwt.system-secret)` — **시크릿이 설정된 환경에서만** 빈으로 등록된다. 그런데 어댑터(`GetBibleVerseRestClientAdapter`)는 `GetBibleVerseUseCase`의 유일한 구현이고, `ExplanationGenerationJobHandler`가 이를 **항상** 주입받는다. 만약 어댑터가 `SystemTokenProvider`를 생성자에서 직접 요구하면, 시크릿 없는 테스트/부팅에서 빈이 없어 **컨텍스트 로딩 자체가 실패**한다.

해결: `ObjectProvider<SystemTokenProvider>`로 받아 `getIfAvailable()`(없으면 null)로 보관. 빈 생성은 항상 성공하고, 실제 토큰 발급은 호출 시점에 시도한다. 발급기가 없으면 `EXTERNAL_API_FAILURE`. 이는 `JwtAuthenticationFilter`가 `ObjectProvider<SystemTokenValidator>`로 검증기를 선택 주입한 패턴과 대칭이다.

> 함정: 생성자가 2개(운영 `ObjectProvider`용 + 테스트 `SystemTokenProvider` 직접 주입용)면 Spring이 어느 것으로 autowire할지 몰라 `NoSuchMethodException`(기본 생성자 탐색)으로 부팅 실패한다. **운영 생성자에 `@Autowired`를 명시**해야 한다.

## 어댑터 단위테스트 패턴

`MockRestServiceServer.bindTo(RestClient.Builder)`로 빌더를 가로채 가짜 service-bible 응답을 흉내낸다. 검증 포인트: ① 정상 응답 매핑 ② `header("Authorization", startsWith("Bearer "))`로 시스템 토큰 주입 확인(토큰 값은 매번 다르니 prefix만) ③ 404/5xx → 공통 예외 매핑 ④ 빈 입력은 HTTP 없이 단락 ⑤ 발급기 null이면 실패. 시크릿은 테스트 전용 더미 문자열(32바이트↑)을 `.java`에 둔다 — gitleaks allowlist가 test resources yml만 덮지만, 더미 .java 시크릿은 기존 `SystemTokenTest`도 통과했다.

## 핵심 교훈

배치 cross-service 호출의 인증은 "토큰을 전달"이 아니라 "토큰을 발급"이다. 그리고 그 발급기는 환경 의존(시크릿 유무)이라 **빈 생성은 항상 가능, 사용 가능 여부는 호출 시점 판단**으로 설계해야 부팅이 안 깨진다.

관련: PR #440(시스템 인증), #435(note→bible 사용자맥락 어댑터, 템플릿), CLAUDE.md §4·§7·§9.
