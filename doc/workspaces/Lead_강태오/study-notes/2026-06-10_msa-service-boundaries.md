# 스터디 노트 — MSA 서비스 경계 & 협업 패턴 (2026-06-10, Day 2)

> Day 2에서 사용자/노트/AI 서비스를 쪼개며 배우는 개념. 입문자 기준. (작업하며 보강)

## 1. 같은 서비스 안 호출 vs 다른 서비스 호출

도메인 A가 도메인 B를 부를 때:
- **A와 B가 같은 서비스 안**에 있으면 → 그냥 자바 객체 호출(in-process). 빠르고 트랜잭션도 같이 묶임.
- **B가 다른 서비스**면 → 네트워크 호출이 필요. 지금은 통합 전이라 **Mock**(가짜 구현)으로 대체하고, 나중에 RestClient(HTTP)로 바꾼다.

예) service-user의 notification은 member와 같은 서비스 → in-process. 하지만 mission은 note(다른 서비스)를 부르므로 → `client/note/...UseCaseMock`.

## 2. api 계약(contract)만 가져오기

다른 서비스의 `api` 패키지(UseCase 인터페이스 + DTO)는 "약속(계약)"이다. 그 계약 타입만 내 서비스에 두고, 실제 동작은 Mock으로 채운다. 상대 서비스의 `internal`(엔티티·서비스·리포지토리)은 절대 가져오지 않는다(CLAUDE.md §3).

## 3. JWT "발급"은 service-user만

RS256(비대칭 키): 개인키로 **발급**, 공개키로 **검증**.
- service-user: 카카오 로그인 성공 → **개인키로 토큰 발급**(JwtProvider). 개인키는 이 서비스에만.
- 나머지 서비스: 공개키로 **검증만**(lib-common의 JwtValidator).

→ 개인키를 한 곳(service-user)에만 둬서 보안 위험을 줄인다.

## 4. Kafka는 AI에서만 (왜)

회의 결정: Kafka는 **쓰기·잡(job) 처리**가 필요한 AI 영역에만. AI는 해설 생성 같은 무거운 비동기 작업이 있어 outbox→Kafka 워커 패턴을 쓴다. 단순 조회 서비스(성경·노트 등)는 동기 RestClient면 충분하고, Kafka를 끼우면 더 복잡해질 뿐이다.

## 5. 이벤트 핸들러 실패는 "재처리 가능 상태"로

note의 JournalEvent처럼 이벤트를 처리하다 실패하면, "유실 0%"라고 말하지 않고 **핸들러 실패 로그(eventId·type·handler·error)를 남기고 재처리 가능 상태**로 둔다(CLAUDE.md §9). 완벽 보장 대신 "복구 가능"을 설계한다.

## 6. (추가 예정)
작업하며 마주친 개념 계속 정리.
