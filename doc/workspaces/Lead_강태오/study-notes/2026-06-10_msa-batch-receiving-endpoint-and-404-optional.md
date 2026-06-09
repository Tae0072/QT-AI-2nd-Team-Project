# 스터디노트 — 배치 수신 엔드포인트 보호(SYSTEM_BATCH)와 404→Optional 매핑

작성일: 2026-06-10 / 작성: Claude (Lead 강태오)

## 1. 호출쌍마다 "수신 엔드포인트 존재 여부"를 먼저 확인하라

RestClient 어댑터를 만들 때 절반은 호출자(어댑터), 절반은 **수신자(상대 서비스의 HTTP 엔드포인트)**다.
ai→bible은 수신 엔드포인트(`/verses/**`)가 이미 있어 어댑터만 만들면 됐지만, ai→qt는 콘텐츠 컨텍스트가
HTTP로 노출돼 있지 않아 **수신 컨트롤러를 신설**해야 했다. 같은 도메인 api UseCase라도 "사용자용 컨트롤러"가
있다고 "배치용 데이터"까지 노출돼 있는 건 아니다 — 계약(UseCase)과 노출(Controller)은 별개다.

## 2. 배치 전용 수신은 SYSTEM_BATCH로 잠근다

QT 콘텐츠 컨텍스트는 **선등록된 미공개 본문(published=false)도 반환**한다(사전 생성용). 이걸 `authenticated()`
로만 두면 일반 사용자가 미공개 본문·verseId를 읽을 수 있어 §8 위반이다. 그래서 메서드 보안으로
`@PreAuthorize("hasRole('SYSTEM_BATCH')")`를 건다 — 시스템 배치 토큰만 통과, 일반 사용자·ADMIN은 403.
(이는 "hasRole('ADMIN') 단독 금지" 규칙과 다른 맥락이다. 그 규칙은 admin_role 이중검증 우회를 막는 것이고,
여기선 *배치 역할*로 좁히는 정당한 제한이다.) 테스트는 실제 JWT 없이 `ROLE_SYSTEM_BATCH` 권한을 주입해
200, `ROLE_USER`로 403을 고정한다(service-bible 기존 보안 테스트 패턴 그대로).

## 3. 404를 Optional.empty()로 되살리려면 retrieve가 아니라 exchange

`findContentContextByDate`의 계약은 "해당 날짜 본문 없음 = `Optional.empty()`"다. 수신은 없을 때 404를
던진다. 어댑터에서 `retrieve().onStatus(404, noop)`로 하면 404 응답의 에러 본문이 그대로 `body()`로 흘러
역직렬화돼 정상/없음 구분이 지저분하다. `RestClient.exchange((req, res) -> ...)`를 쓰면 **상태코드를 직접
분기**해 404는 `Optional.empty()`, 2xx는 본문 언랩으로 깔끔하게 나눈다. 연결 오류(`RestClientException`)는
exchange 바깥 try로 잡아 `EXTERNAL_API_FAILURE`. (단건 `getContentContext`는 같은 헬퍼를 쓰되 empty면
`QT_PASSAGE_NOT_FOUND`로 올린다.)

## 핵심 교훈

배치 RestClient 한 쌍 = (어댑터 + 수신 엔드포인트 + 수신 권한 + 오류/Optional 매핑). 수신 노출이 없으면
신설하고, 미공개 데이터를 다루면 SYSTEM_BATCH로 잠그고, Optional 계약은 exchange로 상태코드를 분기해 보존한다.

관련: [[2026-06-10_msa-batch-system-token-restclient]], PR #441(ai→bible), CLAUDE.md §4·§6·§8·§9.
