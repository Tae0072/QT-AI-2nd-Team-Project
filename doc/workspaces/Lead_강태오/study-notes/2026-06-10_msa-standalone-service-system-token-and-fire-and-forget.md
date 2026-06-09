# 스터디노트 — 독립(standalone) 서비스의 시스템 토큰 수용 · fire-and-forget · 불변식 갱신

작성일: 2026-06-10 / 작성: Claude (Lead 강태오)

## 1. lib-common 비의존 서비스가 공유 시스템 토큰을 받게 하려면

admin-server는 강사 방식대로 모놀리식을 통째 복사한 **lib-common 비의존 standalone**이다(자체 `com.qtai.common`,
자체 `JwtAuthenticationFilter`/`JwtProvider`). 그래서 lib-common의 `SystemTokenValidator`(PR #440)를 못 쓴다.
다른 서비스가 lib-common `SystemTokenProvider`로 발급한 HS256 토큰을 admin-server도 받으려면, **같은 규약(HS256
공유 시크릿, type=system, role=SYSTEM_BATCH)의 검증기를 admin-server 자체 패키지에 추가**하고, admin-server의
`JwtAuthenticationFilter`가 RS256 사용자 검증 실패 시 그 검증기로 폴백하게 한다. 핵심은 **검증기만** 두고
**발급기(Provider)는 두지 않는** 것 — admin-server는 시스템 토큰을 *받기만* 하므로 비대칭이 옳다.

수신 인가는 새로 만들 필요가 없었다: admin-server SecurityConfig엔 이미 `/api/v1/system/** → hasRole("SYSTEM_BATCH")`
규칙이 있어, 그 아래 엔드포인트를 두면 컨트롤러에 `@PreAuthorize`를 또 달지 않아도 된다(경로 규칙 = 단일 진실).

## 2. fire-and-forget: 감사 기록 실패가 본업을 깨면 안 된다

audit은 횡단 관심사이고 원래 Mock이 no-op이었다. RestClient 어댑터로 바꾸면서 전송 실패에 예외를 던지면, AI
검수/생성 트랜잭션이 "로그를 못 남겼다"는 이유로 롤백될 수 있다. 그래서 어댑터는 `RestClientException`만 잡아
경고만 남기고 진행한다(광범위 catch 아님, 토큰·본문 미로깅). "유실 0%"를 약속하는 대신 "실패 로그 + 본업 보호"를
택한다(프로젝트 표현 규칙). 단위 테스트도 `assertThatCode(...).doesNotThrowAnyException()`로 무던함을 고정한다.

## 3. 불변식 테스트는 코드와 함께 명시적으로 갱신한다

admin-server엔 "모든 컨트롤러는 /api/v1/admin 하위"라는 surface 불변식 테스트가 있었다(모놀리식 복사 후 admin만
남긴다는 결정). 시스템 배치 수신 엔드포인트(/api/v1/system/audit-logs)를 추가하면 이 테스트가 깨진다. 무작정 끄지
않고, **불변식을 "admin 또는 system 허용"으로 확장하고 근거 주석을 같이 단다**(admin-server가 audit 소유 →
시스템 배치 수신은 정당). PR#432에서 배운 "가드/불변식 완화는 SSoT를 먼저·함께 갱신" 원칙의 적용이다.

## 핵심 교훈

standalone 서비스가 공유 시스템 토큰을 받으려면 검증기만 자체 도입(발급기는 비대칭으로 생략)하고 기존 경로 규칙을
재사용한다. 횡단 관심사(audit)는 fire-and-forget로 본업을 보호하고, 깨지는 불변식 테스트는 끄지 말고 근거와 함께 확장한다.

관련: [[2026-06-10_msa-batch-system-token-restclient]], [[qtai-pr-firstpush-checklist]], PR #440·#432, CLAUDE.md §4·§5·§7·§9.
