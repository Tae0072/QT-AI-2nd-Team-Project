// Journal Service — 이승욱
// 이벤트 소싱 + Kafka 컨슈머 + @Lock PESSIMISTIC_WRITE
//
// ⚠️ JOURNAL_EVENTS append-only — 수정/삭제 코드 금지
// ⚠️ Kafka 컨슈머: idempotencyKey 검증 필수
// ⚠️ POST /api/v1/journals 없음 — ai.session.completed 컨슈머로 자동 생성

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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kafka Producer + Consumer
    implementation("org.springframework.kafka:spring-kafka")

    // MySQL 8.0
    runtimeOnly("com.mysql:mysql-connector-j")

    // Flyway Migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // JWT 검증
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Metrics + Tracing
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
