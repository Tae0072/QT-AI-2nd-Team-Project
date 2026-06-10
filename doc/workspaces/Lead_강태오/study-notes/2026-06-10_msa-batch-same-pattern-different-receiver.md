# 스터디노트 — 같은 패턴, 다른 수신 서비스: base URL과 미인증 응답코드의 차이

작성일: 2026-06-10 / 작성: Claude (Lead 강태오)

retention purge 4쌍은 어댑터 코드가 거의 동일하지만 **수신 서비스가 어디냐**에 따라 두 가지가 갈린다.

## 1. base URL은 도메인이 아니라 "그 도메인이 사는 서비스"로 정한다

note/sharing/report는 service-note(8083)에, praise는 service-bible(8082)에 산다. 그래서 note/sharing/report
어댑터는 `endpoints.getNoteBaseUrl()`, praise 어댑터는 `endpoints.getBibleBaseUrl()`을 쓴다. 도메인 이름이 아니라
**물리적 소속 서비스**의 base URL을 골라야 한다(qt가 service-bible에 있어 ai→qt 어댑터도 bibleBaseUrl을 쓴 것과 같은 이치).

## 2. 미인증 응답코드는 수신 서비스의 SecurityConfig에 달려 있다

service-bible은 `authenticationEntryPoint`로 `SecurityErrorResponseWriter`를 연결해 미인증을 **401**로 명확히 준다.
service-note는 별도 entry point가 없어 익명 요청이 기본 `Http403ForbiddenEntryPoint`로 떨어져 **403**이 날 수 있다.
그래서 수신 MockMvc 테스트의 미인증 기대값을 service-bible은 `isUnauthorized()`로, service-note는 `isIn(401,403)`으로
다르게 둔다(서비스별 기존 보안 테스트 관례를 따른다). 권한 거부(403)는 양쪽 모두 메서드 보안(@PreAuthorize)에서 동일하다.

## 핵심 교훈

cross-service 어댑터를 복제할 때 "도메인이 어느 서비스에 사는가"로 base URL을 고르고, 수신측 미인증 응답코드는
그 서비스의 SecurityConfig(entry point 유무)에 따라 다를 수 있으니 테스트 기대값을 수신 서비스 관례에 맞춘다.

관련: [[2026-06-10_msa-batch-cross-service-purge-no-distributed-tx]], CLAUDE.md §5.
