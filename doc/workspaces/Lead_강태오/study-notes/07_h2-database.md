# H2 Database — 테스트용 인메모리 DB

> **왜 배워야 하나:** QT-AI는 운영에서 MySQL을 쓰지만, 테스트 코드에서는 H2 인메모리 DB를 사용한다. H2는 테스트할 때 별도 DB 서버 설치 없이 메모리에서 바로 실행되는 경량 DB다. 노션에서 MySQL과 SQL을 배웠지만 H2는 다루지 않았다.

---

## 1. H2가 뭔가?

H2는 Java로 만든 경량 관계형 DB다. 두 가지 모드가 있다:

- **인메모리 모드:** 메모리에서만 동작. 프로그램 종료 시 데이터 사라짐 → 테스트에 적합
- **파일 모드:** 파일에 저장. 프로그램 종료해도 데이터 유지

QT-AI에서는 **인메모리 모드**로 테스트에만 사용한다.

## 2. 왜 테스트에서 MySQL 대신 H2를 쓰나?

| 구분 | MySQL (운영) | H2 (테스트) |
|------|-------------|------------|
| 설치 필요 | 예 (서버 필요) | 아니오 (JVM에 내장) |
| 속도 | 상대적으로 느림 | 매우 빠름 (메모리) |
| 격리 | 테스트 간 데이터 충돌 가능 | 테스트마다 새 DB 생성 |
| CI 환경 | DB 서버 필요 | 의존성만 있으면 됨 |

## 3. 설정 방법

### 3.1 의존성 추가

```kotlin
// build.gradle.kts
testRuntimeOnly("com.h2database:h2")
```

### 3.2 테스트용 설정 파일

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb    # 인메모리 모드
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop    # 테스트 시작 시 테이블 생성, 종료 시 삭제
    database-platform: org.hibernate.dialect.H2Dialect
  h2:
    console:
      enabled: true            # H2 웹 콘솔 활성화 (디버깅용)
```

### 3.3 테스트에서 사용

```java
@DataJpaTest                    // JPA 관련 Bean만 로드
@ActiveProfiles("test")         // test 프로필 사용 → H2 연결
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 회원_저장_조회() {
        // H2 인메모리 DB에 저장
        Member member = new Member("카카오유저", "닉네임");
        memberRepository.save(member);

        // 조회 검증
        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getNickname()).isEqualTo("닉네임");
    }
    // 테스트 끝나면 H2 DB가 자동으로 사라짐
}
```

## 4. MySQL과 H2의 SQL 차이 주의

H2는 MySQL과 대부분 호환되지만, 일부 차이가 있다:

- MySQL의 `DATETIME`은 H2에서 `TIMESTAMP`
- MySQL의 `AUTO_INCREMENT`는 H2에서 `IDENTITY` 또는 `AUTO_INCREMENT` (호환 모드)
- H2 MySQL 호환 모드를 쓰면 차이를 줄일 수 있다: `jdbc:h2:mem:testdb;MODE=MySQL`

## 5. H2 웹 콘솔

`h2.console.enabled: true`로 설정하면 브라우저에서 DB 내용을 확인할 수 있다:

```
http://localhost:8080/h2-console
```

테스트 중 데이터가 제대로 들어갔는지 눈으로 확인할 때 유용하다.

## 6. 참고 자료

- H2 공식 사이트: https://www.h2database.com/
- Spring Boot + H2 가이드: https://www.baeldung.com/spring-boot-h2-database
