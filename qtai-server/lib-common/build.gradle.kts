// lib-common — 모든 서비스가 공유하는 라이브러리 모듈 (부팅 앱 아님).
// 공통 응답/예외, JWT 검증 필터, RestClient 설정 등을 담는다. (내용은 ②단계에서 채움)
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

group = "com.qtai"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0")
    }
}

dependencies {
    // 모든 서비스가 공유하는 기반 (api로 노출 → 의존 서비스가 그대로 사용)
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-validation")
    // BaseEntity(JPA MappedSuperclass) 공유. spring-tx(dao 예외)도 전이로 포함.
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // JWT 검증 (각 서비스는 공개키로 검증만, 발급은 service-user 전용)
    api("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
