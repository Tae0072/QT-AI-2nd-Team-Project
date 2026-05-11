// BFF Aggregator — 강태오
// UseCase 패턴 + CompletableFuture 병렬 호출 + WebSocket(STOMP)
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")  // STOMP
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // RestClient (동기 HTTP) — Bible / Auth / Journal 호출
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // WebClient + SSE (AI Service 프록시)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Redis-WS (STOMP session, JWT blacklist)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka Producer (user.activity.tracked)
    implementation("org.springframework.kafka:spring-kafka")

    // JWT 검증
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Metrics + Tracing
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
