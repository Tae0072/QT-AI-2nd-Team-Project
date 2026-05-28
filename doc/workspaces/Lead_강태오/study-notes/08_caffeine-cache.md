# Caffeine Cache — 애플리케이션 레벨 캐시

> **왜 배워야 하나:** QT-AI에서 Today QT는 00:00 KST에 공개되고 04:00에 배치가 돌아간다. 그 사이 시간에는 **캐시**에 저장된 이전 QT 데이터를 제공한다. Caffeine은 이 캐시를 구현하는 Java 라이브러리다. 노션에서 다루지 않은 기술이다.

---

## 1. 캐시가 뭔가?

자주 사용하는 데이터를 **메모리에 미리 저장**해두고, DB를 매번 조회하지 않고 메모리에서 바로 꺼내주는 것이다.

```
캐시 없이: 사용자 요청 → DB 조회 (느림) → 응답
캐시 있으면: 사용자 요청 → 메모리에서 꺼냄 (빠름) → 응답
```

## 2. Caffeine이 뭔가?

Caffeine은 Google Guava Cache의 후속 버전으로, Java에서 가장 성능이 좋은 로컬 캐시 라이브러리다.

"로컬 캐시"란 **같은 서버 메모리 안**에 데이터를 저장하는 것이다. Redis처럼 별도 서버가 필요 없다.

## 3. Spring Boot에서 사용법

### 3.1 의존성 추가

```kotlin
// build.gradle.kts
implementation("com.github.ben-manes.caffeine:caffeine")
implementation("org.springframework.boot:spring-boot-starter-cache")
```

### 3.2 캐시 활성화

```java
@SpringBootApplication
@EnableCaching           // 캐시 기능 켜기
public class QtAiApplication { ... }
```

### 3.3 캐시 설정

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(100)                    // 최대 100개 항목
            .expireAfterWrite(Duration.ofHours(4))  // 4시간 후 만료
        );
        return manager;
    }
}
```

### 3.4 서비스에서 사용

```java
@Service
public class QtService {

    // 조회 시 캐시에 저장, 같은 key로 다시 호출하면 캐시에서 반환
    @Cacheable(value = "todayQt", key = "#date")
    public QtResponse getTodayQt(LocalDate date) {
        // 이 메서드는 캐시에 없을 때만 실행됨
        return qtRepository.findByDate(date)
            .map(QtResponse::from)
            .orElseThrow();
    }

    // 데이터가 변경되면 캐시를 지움
    @CacheEvict(value = "todayQt", key = "#date")
    public void refreshTodayQt(LocalDate date) {
        // 배치가 새 QT를 가져온 후 캐시를 비움
    }
}
```

## 4. QT-AI에서의 적용

- **00:00 ~ 04:00:** 배치 실행 전이므로, 이전에 캐시에 저장해둔 QT 데이터를 제공
- **04:00 배치 실행 후:** 새 QT 데이터를 DB에 저장하고 캐시를 갱신
- **04:00 이후:** 새 QT 데이터가 캐시에 올라가서 빠르게 응답

## 5. 캐시 어노테이션 정리

| 어노테이션 | 역할 |
|-----------|------|
| `@Cacheable` | 캐시에 있으면 캐시에서 반환, 없으면 메서드 실행 후 캐시에 저장 |
| `@CacheEvict` | 캐시에서 해당 데이터를 삭제 |
| `@CachePut` | 메서드를 항상 실행하고 결과를 캐시에 저장 (갱신용) |

## 6. Redis와의 차이

| 구분 | Caffeine (로컬) | Redis (외부) |
|------|----------------|-------------|
| 저장 위치 | 서버 메모리 안 | 별도 Redis 서버 |
| 속도 | 매우 빠름 (나노초) | 빠름 (밀리초) |
| 서버 간 공유 | 불가 | 가능 |
| 설치 | 라이브러리만 추가 | Redis 서버 필요 |
| QT-AI v1 | 사용 | 필요 시 검토 |

## 7. 참고 자료

- Caffeine 공식 GitHub: https://github.com/ben-manes/caffeine
- Spring Cache 문서: https://docs.spring.io/spring-framework/reference/integration/cache.html
