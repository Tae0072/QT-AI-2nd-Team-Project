// API Gateway — 강태오
// Spring Cloud Gateway (WebFlux 기반 reactive)
//
// 사용 전 1회성 셋업:
//   gradle wrapper --gradle-version=8.10
// 그러면 gradle-wrapper.jar가 생성되어 ./gradlew 사용 가능

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

extra["springCloudVersion"] = "2023.0.3"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Cloud Gateway (reactive)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // JWT 검증 (RS256)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Actuator (Health, Prometheus)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Observability — Jaeger (Tempo 금지)
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Rate Limit
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
