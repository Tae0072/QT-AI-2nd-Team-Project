# Spring Event Publisher — 도메인 간 이벤트 통신

> **왜 배워야 하나:** QT-AI에서 도메인 간 직접 호출을 최소화하기 위해 Spring의 이벤트 발행/구독 패턴을 사용한다. 예를 들어 "회원이 가입하면 → 미션 도메인에서 환영 미션을 생성"하는 식이다. 노션에서 이벤트 기반 통신을 별도로 다루지 않았다.

---

## 1. 이벤트 패턴이 뭔가?

보통 코드에서 도메인 A가 도메인 B를 호출하면 **직접 의존**이 생긴다:

```java
// ❌ member가 mission을 직접 호출 — 강한 결합
class MemberService {
    private final MissionService missionService;  // mission에 의존!

    void register(Member member) {
        memberRepository.save(member);
        missionService.createWelcomeMission(member.getId());  // 직접 호출
    }
}
```

이벤트 패턴은 "무슨 일이 일어났다"는 **이벤트를 발행**하고, 관심 있는 곳에서 **구독해서 처리**한다:

```java
// ✅ member는 이벤트만 발행 — 느슨한 결합
class MemberService {
    private final ApplicationEventPublisher eventPublisher;

    void register(Member member) {
        memberRepository.save(member);
        eventPublisher.publishEvent(new MemberRegisteredEvent(member.getId()));
        // member는 mission의 존재를 모른다!
    }
}
```

## 2. Spring에서 이벤트 사용법

### 2.1 이벤트 클래스 정의

```java
// 이벤트 = "무슨 일이 일어났다"를 담는 데이터 객체
public record MemberRegisteredEvent(Long memberId) {}
```

### 2.2 이벤트 발행

```java
@Service
@RequiredArgsConstructor
public class MemberService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void register(Member member) {
        memberRepository.save(member);
        // 이벤트 발행 — "회원이 가입했다!"
        eventPublisher.publishEvent(new MemberRegisteredEvent(member.getId()));
    }
}
```

### 2.3 이벤트 구독 (리스너)

```java
@Component
public class MissionEventListener {

    // 이 메서드는 MemberRegisteredEvent가 발행되면 자동으로 호출됨
    @EventListener
    public void onMemberRegistered(MemberRegisteredEvent event) {
        missionService.createWelcomeMission(event.memberId());
    }
}
```

## 3. @TransactionalEventListener

일반 `@EventListener`는 이벤트가 발행되면 즉시 실행된다. 하지만 트랜잭션이 실패해서 롤백되면 이벤트 처리도 취소해야 할 수 있다.

```java
// 트랜잭션이 성공적으로 커밋된 후에만 실행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onMemberRegistered(MemberRegisteredEvent event) {
    // 회원 가입이 확실히 완료된 후에만 환영 알림 전송
    notificationService.sendWelcomeNotification(event.memberId());
}
```

## 4. QT-AI에서의 적용

| 이벤트 발행 도메인 | 이벤트 | 구독 도메인 |
|-------------------|--------|------------|
| member | 회원 가입 | mission (환영 미션), notification (환영 알림) |
| qt | QT 완료 | mission (QT 미션 체크), study (학습 기록) |
| note | 노트 작성 | sharing (공유 가능 상태 업데이트) |

CLAUDE.md 규칙: v1에서는 `ApplicationEventPublisher`를 사용하고, Kafka는 v2 이후에 검토한다.

## 5. 이벤트 핸들러 실패 처리

CLAUDE.md §10에 따르면, 이벤트 핸들러 실패 시 로그에 다음 정보를 남겨야 한다:

```java
@EventListener
public void onQtCompleted(QtCompletedEvent event) {
    try {
        missionService.checkQtMission(event.memberId());
    } catch (Exception e) {
        log.error("이벤트 처리 실패 - eventId={}, type={}, handler={}, error={}",
            event.eventId(),
            "QtCompletedEvent",
            "MissionEventListener",
            e.getMessage());
        // 재처리 가능 상태를 남긴다
    }
}
```

## 6. Kafka와의 차이

| 구분 | ApplicationEventPublisher | Kafka |
|------|--------------------------|-------|
| 범위 | 같은 JVM 안 | 서버 간 통신 |
| 영속성 | 없음 (메모리) | 있음 (디스크) |
| 복잡도 | 낮음 | 높음 |
| QT-AI | v1 사용 | v2 이후 검토 |

## 7. 참고 자료

- Spring Events 문서: https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
- Baeldung Spring Events: https://www.baeldung.com/spring-events
