// MSA service-gateway — Spring Cloud Gateway(reactive). /api/v1/** 라우팅.
// 현재는 Strangler: 추출 전까지 모놀리식(qtai-server)으로 전달.
//
// lib-common(web-free 코어, #353 분리)을 의존한다. 코어는 servlet starter-web을 더 이상
// 강제하지 않으므로(spring-web 추상화만) WebFlux 게이트웨이와 충돌하지 않는다.
// → JwtTokenVerifier/AuthenticatedUser/ApiResponse/ErrorCode 재사용으로 게이트웨이 JWT 인증.
plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.qtai"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

extra["springCloudVersion"] = "2023.0.3"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    // 라우트 Circuit Breaker(Resilience4j, reactive) — 다운스트림 장애 격리 + 폴백
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    // web-free 코어 — JwtTokenVerifier/AuthenticatedUser/ApiResponse/ErrorCode (게이트웨이 JWT 인증)
    implementation(project(":lib-common"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
