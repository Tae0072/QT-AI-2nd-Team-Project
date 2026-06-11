# 스터디노트 — 배치 쓰기(write) RestClient: 다계약 단일 어댑터와 메서드 보안 순서

작성일: 2026-06-10 / 작성: Claude (Lead 강태오)

## 1. 읽기 쌍 vs 쓰기 쌍

ai→bible/qt는 읽기(GET)였지만 ai→study는 **쓰기(게시·숨김)** 가 섞인다. 패턴은 같다(시스템 토큰 발급 →
Bearer → 수신 SYSTEM_BATCH 게이트). 다른 점: POST 본문 직렬화(`RestClient.post().body(command)`)와, 수신에서
`@Valid @RequestBody`로 명령을 1차 검증(서비스가 2차 방어). CSRF는 STATELESS라 비활성이므로 POST에 토큰 불필요.

## 2. 한 리소스의 여러 계약 = 한 어댑터

`Publish`/`Hide`/`List ApprovedVerseExplanationUseCase`는 모두 같은 수신 리소스(`/verse-explanations`)를
다룬다. 계약별로 어댑터를 3개 만들 수도 있지만, RestClient·토큰 발급·base URL을 공유하므로 **한 클래스가 3
인터페이스를 구현**하는 게 자연스럽다. Spring은 그 빈을 3개 UseCase 타입 각각으로 주입해 주고, 기존 Mock 3종을
한 번에 대체한다(CLAUDE.md §4). 도메인 경계도 깨지 않는다(study.api만 import, study.internal 미접근).

## 3. 메서드 보안(@PreAuthorize)과 @Valid의 실행 순서

`@PreAuthorize("hasRole('SYSTEM_BATCH')")`는 메서드 인터셉터라 **컨트롤러 메서드 호출 시점**에 평가된다. 반면
`@Valid @RequestBody`는 그 앞 단계(HandlerMethodArgumentResolver)에서 검증된다. 따라서 **본문이 유효하지 않으면
403이 아니라 400이 먼저** 난다. 권한(403) 테스트는 *유효한 본문*으로 보내야 @PreAuthorize까지 도달해 403이 뜬다
(무효 본문이면 400). 본문 없는 GET 엔드포인트로 403/401을 검증하면 이 함정을 피한다. 테스트는 실제 JWT 없이
`ROLE_SYSTEM_BATCH`/`ROLE_USER` 권한을 주입해 200/403을, 무인증으로 401을 고정한다.

## 핵심 교훈

배치 쓰기 쌍도 읽기 쌍과 같은 시스템 토큰·SYSTEM_BATCH 수신 게이트를 쓰되, 같은 리소스의 여러 계약은 한
어댑터로 묶고, 권한 테스트는 메서드 보안과 본문 검증의 순서를 의식해 유효 본문/무본문 GET으로 검증한다.

관련: [[2026-06-10_msa-batch-system-token-restclient]], [[2026-06-10_msa-batch-receiving-endpoint-and-404-optional]], PR #441·#443, CLAUDE.md §4·§7·§10.
