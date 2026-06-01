# Spring 프로파일과 외부 설정 주입 (.env, 테스트 오버라이드)

> **왜 배워야 하나:** 노션에서 Spring 기초는 배웠지만, "환경마다(test/local/dev/prod) 설정을 다르게 적용하는 프로파일"과 "비밀값을 코드 밖(.env/환경변수)에서 주입하는 방법"은 다루지 않았다. QT-AI는 같은 코드로 H2와 MySQL을 오가고, JWT 키 같은 비밀을 레포에 안 올린다.

---

## 1. 프로파일 — 환경별 설정 묶음

`application-{프로파일}.yml` 파일로 환경마다 다른 설정을 둔다.

```
application.yml          # 공통 (기본 active: local)
application-local.yml    # 로컬: H2
application-dev.yml      # 개발/시연: MySQL
application-prod.yml     # 운영: MySQL(주소는 환경변수)
application-test.yml     # 테스트: H2 (src/test/resources)
```

활성 프로파일 지정:
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}   # 환경변수 없으면 local
```
```bash
SPRING_PROFILES_ACTIVE=dev java -jar app.jar   # dev로 실행
```

QT-AI 프로파일별 DB:

| 프로파일 | DB | 용도 |
|---|---|---|
| test / local | H2 (인메모리) | 빠른 테스트·개발 |
| dev | MySQL (Docker) | 실DB 확인·시연 |
| prod | MySQL (주소 주입) | 운영 |

## 2. 외부 설정 주입 — `${ }` 문법

값을 코드/yml에 박지 않고 **환경변수에서** 가져온다:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/qtai}   # 환경변수 없으면 뒤가 기본값
security:
  jwt:
    private-key: ${JWT_PRIVATE_KEY}    # 기본값 없음 → 반드시 주입해야 함
```
- `${이름:기본값}` → 환경변수 `이름`이 있으면 그 값, 없으면 `기본값`.
- `${이름}` → 없으면 기동 실패(필수값 강제).

## 3. 비밀값은 `.env`로 (레포에 올리지 않기)

JWT 키·비밀번호 같은 건 git에 커밋하면 안 된다(보안). 그래서:

```
.gitignore 에  .env  등록  → git이 무시
.env.example  (빈 템플릿)만 커밋 → 팀원이 복사해서 채움
```
```bash
cp .env.example .env        # 한 번만
# .env 안에 JWT_PRIVATE_KEY=... 채우기
```
- Docker Compose는 `env_file: .env`로 컨테이너에 주입.
- (참고) 로컬 개발 땐 `spring-dotenv` 라이브러리가 `.env`를 자동으로 읽어주기도 한다.

## 4. 테스트에서 설정 덮어쓰기

통합 테스트에서 특정 설정만 바꾸고 싶을 때 쓰는 도구들(이번 세션에서 실제로 사용):

```java
// (1) 빈을 테스트용으로 교체 — 예: 시계를 고정
@TestConfiguration
static class FixedClockConfig {
    @Bean @Primary          // @Primary = 기존 빈보다 이걸 우선
    Clock fixedClock() {
        return Clock.fixed(...);   // "오늘"을 2026-05-27로 고정
    }
}

// (2) 실행 시점에 설정값 주입 — 예: Testcontainers 주소
@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MYSQL::getJdbcUrl);
}
```

- `@ActiveProfiles("test")` + `@DynamicPropertySource`를 섞으면, "test 프로파일의 JWT/더미값은 재사용하되 DB만 MySQL로 바꾸기" 같은 게 가능하다.
- 우선순위: `@DynamicPropertySource` > `application-{profile}.yml` > `application.yml`.

## 5. 설정 우선순위 (높은 게 이김)

```
명령행/환경변수  >  @DynamicPropertySource  >  application-{profile}.yml  >  application.yml
```

## 6. QT-AI에서의 적용

- 같은 코드가 프로파일만 바꿔 H2(test/local) ↔ MySQL(dev/prod)로 동작.
- JWT 키는 `.env`/환경변수로만 주입(레포 평문 금지, CLAUDE.md §8).
- 통합 테스트에서 `Clock` 고정·MySQL 주소 주입에 `@TestConfiguration`/`@DynamicPropertySource` 사용.

## 7. 참고 자료

- Spring Profiles: https://docs.spring.io/spring-boot/reference/features/profiles.html
- Externalized Configuration: https://docs.spring.io/spring-boot/reference/features/external-config.html
