# Spring Boot 3.x — 자동 설정과 스타터

> **왜 배워야 하나:** QT-AI 서버(`qtai-server`)가 Spring Boot 3.3 기반이다. 노션에서 Spring v1-v4(전통 방식)를 배웠지만, Spring Boot의 자동 설정(Auto Configuration)과 스타터(Starter) 개념은 다루지 않았다.

---

## 1. Spring vs Spring Boot — 뭐가 다른가?

Spring(전통 방식)은 XML이나 Java Config로 Bean을 하나하나 등록해야 했다. 예를 들어 DataSource, EntityManagerFactory, TransactionManager를 직접 설정해야 했다.

Spring Boot는 이런 반복 설정을 **자동으로 해준다.** `spring-boot-starter-data-jpa`를 의존성에 넣으면 DataSource, JPA, 트랜잭션 매니저가 자동으로 설정된다.

## 2. 핵심 개념

### 2.1 스타터 (Starter)

스타터는 "이 기능을 쓰겠다"는 의존성 묶음이다. QT-AI에서 사용하는 주요 스타터:

```
spring-boot-starter-web         → 웹 서버 (내장 Tomcat 포함)
spring-boot-starter-data-jpa    → JPA + Hibernate
spring-boot-starter-security    → Spring Security
spring-boot-starter-validation  → 입력 검증 (@Valid)
spring-boot-starter-test        → 테스트 (JUnit 5 + Mockito)
```

### 2.2 자동 설정 (Auto Configuration)

`@SpringBootApplication` 어노테이션 하나로 3가지가 동시에 켜진다:

```java
@SpringBootApplication  // 이 하나가 아래 3개를 포함
// @SpringBootConfiguration  → 이 클래스가 설정 클래스
// @EnableAutoConfiguration  → 자동 설정 켜기
// @ComponentScan            → 같은 패키지 아래 Bean 자동 스캔
public class QtAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(QtAiApplication.class, args);
    }
}
```

### 2.3 application.yml / application.properties

Spring Boot는 설정 파일 하나로 거의 모든 설정을 관리한다:

```yaml
# application.yml 예시
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qtai
    username: root
    password: ${DB_PASSWORD}   # 환경 변수로 주입
  jpa:
    hibernate:
      ddl-auto: validate       # 운영에서는 validate
    show-sql: false

server:
  port: 8080
```

### 2.4 프로필 (Profile)

환경별로 다른 설정을 적용할 수 있다:

```
application.yml          → 공통 설정
application-local.yml    → 로컬 개발용
application-test.yml     → 테스트용 (H2 DB)
application-prod.yml     → 운영용
```

실행할 때 `--spring.profiles.active=local`로 선택한다.

## 3. QT-AI 프로젝트에서의 적용

QT-AI에서 Spring Boot 3.3을 사용하는 구체적인 위치:

- `QtAiApplication.java`: `@SpringBootApplication`으로 서버 시작
- `build.gradle.kts`: Spring Boot 스타터 의존성 선언
- `application.yml`: DB, 캐시, 보안, AI 외부 API 설정
- 내장 Tomcat: 별도 서버 설치 없이 `java -jar`로 실행

## 4. 더 알아볼 것

- `@ConfigurationProperties`: 설정값을 타입 안전하게 바인딩하는 방법
- `@ConditionalOnProperty`: 조건부 자동 설정
- Actuator: 서버 상태 모니터링 엔드포인트
- DevTools: 개발 중 자동 재시작

## 5. 참고 자료

- Spring Boot 공식 문서: https://docs.spring.io/spring-boot/docs/current/reference/html/
- Baeldung Spring Boot 튜토리얼: https://www.baeldung.com/spring-boot
