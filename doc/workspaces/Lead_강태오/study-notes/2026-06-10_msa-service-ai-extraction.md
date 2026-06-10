# 스터디 노트 — service-ai 추출에서 배운 것 (입문자용)

> 2026-06-10, MSA Day2 service-ai. 모놀리식 ai 도메인을 독립 서비스 모듈로 떼어내며 마주친 개념들.

## 1. "다른 서비스의 기능"을 부르는 법 — api 계약 + Mock
MSA로 쪼개면 ai 서비스 안에 audit·study·qt·bible·admin 도메인은 더 이상 없다. 그런데 ai 코드는 이들의 기능을 호출한다(예: 감사 로그 기록). 어떻게?

- **상대 도메인의 `api` 패키지(UseCase 인터페이스 + DTO)만 가져온다.** 이건 "계약서"라서 내부 구현(`internal`)을 안 끌고 온다 — 그래서 통째로 복사해도 안전(자기완결).
- 그 인터페이스를 구현하는 **임시 Mock(`client/{도메인}/...UseCaseMock`)** 을 만든다. 진짜 호출 대신 무해한 기본값을 돌려준다.
- 나중에 통합 단계에서 Mock을 **RestClient 어댑터**(진짜 HTTP 호출)로 바꾸고 Mock은 삭제한다.

핵심: 인터페이스(계약)는 그대로 두고 **구현만 Mock→RestClient로 갈아끼운다.** 이게 도메인 경계를 지키는 방법.

## 2. 함정: `@ConditionalOnMissingBean`을 `@Component`에 달지 마라
처음에 Mock에 `@Component @ConditionalOnMissingBean(WriteAuditLogUseCase.class)`를 달았더니, 부팅 시 "그 빈이 없다"며 죽었다. 이유:

- `@ConditionalOnMissingBean`은 **컴포넌트 스캔 순서에 의존**해서, 일반 `@Component`에선 신뢰할 수 없다(스프링 공식도 자동설정/`@Bean` 메서드용으로 권장).
- 결과적으로 Mock 빈 자체가 등록되지 않아, 그 Mock을 주입받아야 할 서비스가 "주입할 빈 없음(NoSuchBeanDefinitionException)"으로 실패.
- **해결**: 그냥 순수 `@Component`. "진짜 구현이 생기면 Mock을 삭제한다"는 규칙(CLAUDE.md §4)이 있으니 조건부가 필요 없다.

## 3. 스케줄러를 "테스트에선 끄기" — @EnableScheduling 게이트
ai엔 `@Scheduled`(매일 00:05 시딩, 생성 job 폴링)가 있다. 테스트나 기동 검증 때 이게 돌면 외부(LLM·DB)를 건드려 불안정하다.

- `@Scheduled`는 **`@EnableScheduling`이 있어야만** 동작한다.
- 그래서 `@EnableScheduling`을 단 설정 클래스에 `@ConditionalOnProperty(name="ai.scheduling.enabled", havingValue="true")`를 걸었다. 기본값 false → 테스트에선 스케줄링 자체가 꺼진다. 운영에선 env로 켠다.
- 워커 빈은 그대로 생성된다(주입은 됨). **트리거(스케줄링)만 제어**하는 게 포인트.

## 4. ArchUnit — 경계를 "코드로" 지키기
사람이 매번 import를 검사할 수 없으니 테스트로 강제한다.
```
noClasses().that().resideInAPackage("com.qtai.domain.ai..")
  .should().dependOnClassesThat().resideInAnyPackage("com.qtai.domain.audit.internal..", ...)
```
= "ai 코드가 다른 도메인의 internal을 import하면 빌드 실패". 경계가 무너지면 즉시 알 수 있다.

## 5. 금지기능이 "없다"를 테스트하기
CLAUDE.md §8은 자유챗봇·다중턴·SSE·세션을 금지한다. "없음"을 어떻게 테스트?
- `ClassPathScanningCandidateComponentProvider`로 `@RestController`를 모아 **매핑 경로**에 `/sessions`,`/sse`,`/chat`,`stream` 같은 토큰이 있으면 실패.
- 핸들러 **반환 타입**에 `SseEmitter`,`Flux`,`Mono` 등이 있으면 실패.
- 덤: 토큰 `"sse"`가 `"assets"`(a-**sse**-ts)에 부분일치하는 오탐을 겪음 → `"/sse"`처럼 경로 단위로 정밀화. **문자열 부분일치 검사는 항상 오탐을 의심하자.**

## 6. 작은 함정 둘
- **denyAll vs 미인증**: `/api/v1/admin/**`을 denyAll로 막으면 인증된 ADMIN도 403. 그런데 *미인증* 요청은 (JWT entry point가 없는 테스트에선) 401이 아니라 403이 날 수 있다 → 테스트는 "401 또는 403(=거부)"로 유연하게.
- **BOM**: PowerShell `Set-Content -Encoding UTF8`은 파일 앞에 보이지 않는 BOM(﻿)을 넣어 Java 컴파일을 깨뜨린다. 코드 파일은 BOM 없는 도구로 쓰자.

## 7. Strangler 패턴 한 줄 요약
모놀리식 원본은 **그대로 두고**, 새 모듈로 도메인을 **복사**해 독립 기동을 확인한다. 둘 다 살아있는 전환기 → 나중에 원본을 제거. "한 번에 갈아엎지 않는다"가 안전의 핵심.
