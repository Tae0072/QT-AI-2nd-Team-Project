// AI Service — 강태오·김태혁·강상민
// DeepSeek API (OpenAI 호환 SSE) + ChromaDB RAG + Kafka 이벤트 발행
// Spring Boot 3.3 / Java 21 — 다른 서비스와 일관된 스택
//
// 사용 전 1회성 셋업:
//   gradle wrapper --gradle-version=8.10

plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
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

dependencies {
    // Spring MVC + SseEmitter (SSE 스트리밍)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 세션·턴 저장
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // JWT 검증 (RS256)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Kafka Producer (ai.session.completed)
    implementation("org.springframework.kafka:spring-kafka")

    // Metrics + Tracing (Jaeger)
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
