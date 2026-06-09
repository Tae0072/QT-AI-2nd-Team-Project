// service-ai — AI 서비스 (ai 도메인: 사전 생성/검증, F-15 단발 Q&A). Kafka는 이 서비스에만.
// Day2-5-1: 부팅 스켈레톤. ai 도메인 코드·Kafka·LLM external은 후속 단계에서 이전.
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

    // 검증 참조자료(PDF) 텍스트 추출용. 모놀리식과 동일 버전.
    // 참고: 현재 baseline의 AI 생성 파이프라인은 Kafka가 아니라 @Scheduled 폴링(outbox) 방식이라
    //       spring-kafka 의존은 추가하지 않는다. (Kafka 도입은 후속 단계)
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 단일 DB: 로컬/테스트 H2, 운영 MySQL (접속정보 env 주입)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ArchUnit — 도메인 경계 자동 검증 (CLAUDE.md §3, §4)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    // Spring Security 테스트 지원 (MockMvc 인증 주입)
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
