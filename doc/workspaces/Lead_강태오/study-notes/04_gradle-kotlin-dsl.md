# Gradle Kotlin DSL — 빌드 자동화

> **왜 배워야 하나:** QT-AI 서버의 빌드 파일이 `build.gradle.kts`(Kotlin DSL)로 작성되어 있다. 의존성 추가, 테스트 실행, JAR 빌드를 모두 이 파일이 관리한다. 노션에서 빌드 도구를 별도로 다루지 않았다.

---

## 1. Gradle이 뭔가?

Java 프로젝트를 빌드(컴파일 → 테스트 → 패키징)하는 자동화 도구다. Maven과 비슷한 역할이지만, 더 유연하고 빠르다.

```
소스 코드 (.java) → [Gradle] → 컴파일 → 테스트 → JAR 파일 (실행 가능)
```

## 2. build.gradle.kts 기본 구조

```kotlin
// 플러그인 선언 — "이 프로젝트는 Spring Boot + Java 프로젝트다"
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
}

// 프로젝트 정보
group = "com.qtai"
version = "0.0.1-SNAPSHOT"

// Java 버전 설정
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)  // Java 21 사용
    }
}

// 의존성 저장소 — "라이브러리를 어디서 다운받을까?"
repositories {
    mavenCentral()
}

// 의존성 — "어떤 라이브러리를 쓸까?"
dependencies {
    // 런타임에 필요한 것
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 컴파일 시에만 필요한 것 (Lombok 등)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 운영 DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // 테스트에서만 필요한 것
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")  // 테스트용 H2 DB
}
```

## 3. 자주 쓰는 명령어

```bash
# 전체 빌드 (컴파일 + 테스트 + JAR)
./gradlew build

# 테스트만 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "*MemberServiceTest*"

# 프로젝트 청소 (빌드 결과물 삭제)
./gradlew clean

# 실행 가능한 JAR 생성
./gradlew bootJar

# 서버 바로 실행
./gradlew bootRun
```

QT-AI 프로젝트에서는 서버 디렉토리를 지정해서 실행한다:

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test
```

## 4. 의존성 종류 이해

| 키워드 | 의미 | 예시 |
|--------|------|------|
| `implementation` | 런타임 + 컴파일 둘 다 필요 | Spring Boot Starter |
| `compileOnly` | 컴파일할 때만 필요 | Lombok |
| `runtimeOnly` | 실행할 때만 필요 | MySQL 드라이버 |
| `testImplementation` | 테스트 코드에서만 필요 | JUnit, Mockito |
| `annotationProcessor` | 어노테이션 처리기 | Lombok 처리기 |

## 5. Gradle Wrapper (gradlew)

`gradlew`는 Gradle을 별도 설치하지 않아도 프로젝트에 포함된 Gradle로 빌드할 수 있게 해주는 스크립트다. 팀원 모두 같은 Gradle 버전을 사용하게 보장한다.

```
프로젝트/
├── gradlew          ← Linux/Mac용 실행 스크립트
├── gradlew.bat      ← Windows용 실행 스크립트
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties  ← Gradle 버전 지정
```

## 6. 참고 자료

- Gradle 공식 문서: https://docs.gradle.org/current/userguide/userguide.html
- Kotlin DSL 가이드: https://docs.gradle.org/current/userguide/kotlin_dsl.html
