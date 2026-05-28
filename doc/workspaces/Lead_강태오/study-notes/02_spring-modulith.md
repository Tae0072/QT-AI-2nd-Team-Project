# Spring Modulith — 모듈 경계 관리

> **왜 배워야 하나:** QT-AI는 13개 도메인(member, bible, qt 등)을 하나의 서버 안에서 모듈로 분리한다. Spring Modulith는 이 모듈 경계를 코드 레벨에서 강제하고 검증해주는 도구다. 노션에서 MSA(마이크로서비스)를 배웠지만, "하나의 서버 안에서 모듈을 나누는 방법"인 Modular Monolith는 다루지 않았다.

---

## 1. Modular Monolith가 뭔가?

MSA는 서비스를 여러 서버로 쪼개는 것이다. 하지만 QT-AI처럼 팀이 작고 초기 단계인 프로젝트에서 MSA는 과도하다.

Modular Monolith는 **하나의 서버(Monolith) 안에서 도메인별로 모듈을 깔끔하게 분리**하는 방식이다. 나중에 MSA로 전환이 필요하면 모듈 단위로 떼어낼 수 있다.

```
qtai-server (하나의 서버)
├── domain.member/    ← 모듈 1
├── domain.bible/     ← 모듈 2
├── domain.qt/        ← 모듈 3
├── domain.study/     ← 모듈 4
└── ...               ← 13개 도메인 모듈
```

## 2. Spring Modulith가 해주는 일

### 2.1 모듈 자동 감지

패키지 구조를 보고 자동으로 모듈을 인식한다:

```
com.qtai.server.domain.member/   → "member" 모듈
com.qtai.server.domain.bible/    → "bible" 모듈
```

### 2.2 경계 검증

`member` 모듈이 `bible`의 내부(`internal/`)를 직접 import하면 **테스트에서 실패**시킨다:

```java
// 이런 테스트를 작성하면 모듈 경계를 자동 검증
@Test
void 모듈_경계_검증() {
    ApplicationModules modules = ApplicationModules.of(QtAiApplication.class);
    modules.verify();  // 금지된 import가 있으면 여기서 실패
}
```

### 2.3 허용된 접근 vs 금지된 접근

```
✅ 허용: member → bible.api.GetBibleUseCase (api/ 패키지의 인터페이스)
❌ 금지: member → bible.internal.BibleService (internal/ 패키지)
❌ 금지: member → bible.internal.BibleRepository (내부 Repository)
```

QT-AI에서는 다른 도메인의 `api/` 패키지에 있는 UseCase 인터페이스만 호출할 수 있다.

## 3. QT-AI에서의 적용 예시

```java
// bible 도메인이 외부에 공개하는 인터페이스 (api/ 패키지)
public interface GetBibleUseCase {
    BibleResponse getBible(Long bibleId);
}

// qt 도메인이 bible을 호출할 때 (client/ 패키지)
// → 아직 bible이 구현 안 됐으면 Mock으로 임시 대체
@Component
public class GetBibleUseCaseMock implements GetBibleUseCase {
    @Override
    public BibleResponse getBible(Long bibleId) {
        return new BibleResponse(bibleId, "테스트 성경");
    }
}
```

## 4. MSA와의 차이점

| 구분 | MSA | Modular Monolith |
|------|-----|-------------------|
| 배포 단위 | 서비스별 별도 배포 | 하나의 JAR로 배포 |
| 통신 방식 | HTTP/gRPC/Kafka | Java 메서드 호출 |
| 복잡도 | 높음 (네트워크, 분산 트랜잭션) | 낮음 (단일 프로세스) |
| 팀 규모 | 대규모 | 소규모에 적합 |
| 전환 | - | 나중에 MSA로 분리 가능 |

## 5. 더 알아볼 것

- `@ApplicationModule`: 모듈에 명시적 메타데이터를 부여하는 방법
- 모듈 간 이벤트: `ApplicationEventPublisher`를 통한 느슨한 결합
- Documenter: 모듈 의존 관계를 시각화하는 기능

## 6. 참고 자료

- Spring Modulith 공식 문서: https://docs.spring.io/spring-modulith/reference/
- Spring Modulith 예제: https://github.com/spring-projects/spring-modulith
