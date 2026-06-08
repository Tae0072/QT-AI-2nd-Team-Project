// lib-common-web — servlet/JPA 의존 공통(GlobalExceptionHandler, BaseEntity).
// servlet 앱(모놀리식/servlet 서비스)만 의존한다. 코어(lib-common)는 web-free로 유지.
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
    api(project(":lib-common"))                                    // ApiResponse/ErrorCode/BusinessException 등 코어

    api("org.springframework.boot:spring-boot-starter-web")        // @RestControllerAdvice, servlet MVC 예외
    api("org.springframework.boot:spring-boot-starter-data-jpa")   // BaseEntity: @MappedSuperclass, Auditing
    api("org.springframework.boot:spring-boot-starter-validation") // ConstraintViolationException
    api("org.springframework.boot:spring-boot-starter-security")   // AccessDeniedException

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
