# ArchUnit — 아키텍처 규칙을 코드로 검증

> **왜 배워야 하나:** QT-AI는 도메인 간 import 금지, Controller→Repository 직접 호출 금지 같은 아키텍처 규칙이 있다. ArchUnit은 이런 규칙을 **테스트 코드로 작성해서 자동으로 검증**하는 라이브러리다. 노션에서 다루지 않은 도구다.

---

## 1. ArchUnit이 뭔가?

코딩 규칙을 "사람이 눈으로 확인"하면 실수가 생긴다. ArchUnit은 JUnit 테스트로 아키텍처 규칙을 검사해서, 규칙을 어기면 빌드가 실패하게 만든다.

## 2. 기본 사용법

### 2.1 의존성 추가

```kotlin
// build.gradle.kts
testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
```

### 2.2 규칙 작성 예시

```java
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

class ArchitectureTest {

    // 규칙 1: Controller는 Repository를 직접 호출하면 안 된다
    @ArchTest
    static final ArchRule controller는_repository를_직접_호출하지_않는다 =
        noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..internal..");

    // 규칙 2: member 도메인은 bible의 internal을 import하면 안 된다
    @ArchTest
    static final ArchRule 도메인_간_internal_접근_금지 =
        noClasses()
            .that().resideInAPackage("..domain.member..")
            .should().dependOnClassesThat()
            .resideInAPackage("..domain.bible.internal..");
}
```

### 2.3 실행

일반 JUnit 테스트처럼 실행하면 된다:

```bash
./gradlew test --tests "*ArchitectureTest*"
```

## 3. QT-AI에서 검증하는 규칙들

| 규칙 | ArchUnit으로 검증하는 내용 |
|------|--------------------------|
| internal 접근 금지 | 다른 도메인의 `internal/` 패키지 import 차단 |
| Controller→Repository 금지 | web 패키지에서 Repository 직접 사용 차단 |
| Entity 외부 노출 금지 | api/ 패키지에 Entity 타입이 들어가지 않는지 확인 |
| UseCase만 호출 | 다른 도메인은 `api/` 패키지의 인터페이스만 사용하는지 확인 |

## 4. 자주 쓰는 API

```java
// 패키지 안의 클래스 선택
classes().that().resideInAPackage("..domain.member..")

// 어노테이션이 붙은 클래스 선택
classes().that().areAnnotatedWith(Service.class)

// 금지 규칙
noClasses().that().resideInAPackage("..web..")
    .should().dependOnClassesThat().resideInAPackage("..internal..")

// 강제 규칙
classes().that().areAnnotatedWith(Controller.class)
    .should().resideInAPackage("..web..")
```

## 5. Spring Modulith와의 관계

Spring Modulith도 모듈 경계를 검증하지만, ArchUnit은 **더 세밀한 규칙**을 정의할 수 있다. QT-AI에서는 두 가지를 함께 사용한다.

## 6. 참고 자료

- ArchUnit 공식 문서: https://www.archunit.org/userguide/html/000_Index.html
- ArchUnit 예제 모음: https://www.archunit.org/userguide/html/000_Index.html#_examples
