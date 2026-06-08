// MSA service-gateway — Spring Cloud Gateway(reactive). /api/v1/** 라우팅.
// 현재는 Strangler: 추출 전까지 모놀리식(qtai-server)으로 전달.
//
// 주의: 게이트웨이는 WebFlux(reactive)다. lib-common이 servlet `spring-boot-starter-web`을
// api로 강제하므로 지금은 lib-common을 의존하지 않는다(servlet/reactive 충돌 회피).
// JwtTokenVerifier 재사용(게이트웨이 인증)은 lib-common의 web 결합을 좁힌 뒤 후속 증분에서.
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

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
