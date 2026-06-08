// 서비스 공통 라이브러리 — ApiResponse/ErrorCode/BusinessException/GlobalExceptionHandler/BaseEntity.
// MSA 분리 1단계(feature/msa-foundation): com.qtai.common 패키지를 모놀리식에서 분리.
plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.qtai"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4") }
}

dependencies {
    // 공통 타입이 쓰는 스프링/JPA/검증/보안 (api = 사용 모듈에 전이 노출)
    api("org.springframework.boot:spring-boot-starter-web")          // HttpStatus, @RestControllerAdvice, Jackson, slf4j
    api("org.springframework.boot:spring-boot-starter-data-jpa")     // BaseEntity: @MappedSuperclass, Auditing
    api("org.springframework.boot:spring-boot-starter-validation")   // ConstraintViolationException
    api("org.springframework.boot:spring-boot-starter-security")     // AccessDeniedException

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
