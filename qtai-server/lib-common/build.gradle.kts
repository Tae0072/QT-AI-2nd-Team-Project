// 서비스 공통 코어 라이브러리 (web-free) — ApiResponse/ErrorCode/BusinessException/JwtTokenVerifier/AuthenticatedUser.
// embedded Tomcat·JPA 자동설정을 강제하지 않는다 → WebFlux 게이트웨이/리액티브 서비스도 의존 가능.
// servlet MVC 예외처리(GlobalExceptionHandler)·JPA(BaseEntity)는 lib-common-web으로 분리됨.
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
    // ErrorCode: org.springframework.http.HttpStatus (spring-web 추상화 — Tomcat 미포함, MVC/WebFlux 공용)
    api("org.springframework:spring-web")
    // ApiResponse: org.slf4j.MDC (traceId)
    api("org.slf4j:slf4j-api")

    // JWT 검증 유틸(JwtTokenVerifier) — RS256 공개키 검증. 발급(개인키)은 인증 서비스에만.
    api("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test") // JUnit5 + AssertJ
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
