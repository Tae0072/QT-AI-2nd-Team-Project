// service-user — 사용자/인증 서비스 (member, notification, mission). JWT 발급 주체.
// Day2-1: 부팅 스켈레톤. 도메인 코드/JWT 발급은 Day2-2에서 이전.
plugins {
    java
    id("org.springframework.boot")
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

dependencies {
    implementation(project(":lib-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 단일 DB: 로컬/테스트 H2, 운영 MySQL (접속정보 env 주입)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ArchUnit — 도메인 경계 자동 검증 (CLAUDE.md §3, §4)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
