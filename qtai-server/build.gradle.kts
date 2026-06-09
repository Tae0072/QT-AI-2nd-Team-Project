plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.7"
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
    // MSA 공통 모듈 의존 — lib-common-web(servlet/JPA)이 코어 lib-common을 전이 노출
    // (GlobalExceptionHandler·BaseEntity + ApiResponse/ErrorCode/BusinessException/JwtTokenVerifier 등)
    implementation(project(":lib-common-web"))

    // .env 파일 자동 로딩 — 로컬 개발 전용, 운영 런타임에는 포함되지 않음
    developmentOnly("me.paulschwarz:spring-dotenv:4.0.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Caffeine — 로컬 캐시 (bible_books 등 불변 데이터)
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // Flyway — DB 마이그레이션
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Resilience4j Circuit Breaker — external/bible HTTP 호출 장애 격리(게이트웨이 Resilience4j와 정합, 2.x)
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // JWT — RS256 (access 30분 / refresh 14일, 키는 환경변수로 주입)
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ArchUnit — 도메인 경계 자동 검증 (CLAUDE.md §3, §4)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // Testcontainers — 실 MySQL 기반 Flyway migrate + Hibernate validate 가드 (버전은 Spring Boot BOM 관리)
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<JavaExec>("aiReviewReferencePdfIndexDiagnostics") {
    group = "ai"
    description = "Generate AI review reference PDF index candidates and quality diagnostics."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.qtai.domain.ai.internal.AiReviewReferencePdfIndexDiagnosticsTool")
}

tasks.register<JavaExec>("aiReviewReferencePromoteCandidateIndex") {
    group = "ai"
    description = "Promote AI review reference candidate index into production reference index."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.qtai.domain.ai.internal.AiReviewReferenceCandidatePromotionTool")
}

tasks.register<JavaExec>("aiReviewReferenceBookSectionMapCandidate") {
    group = "ai"
    description = "Generate AI review reference book section map candidates from a PDF."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.qtai.domain.ai.internal.AiReviewReferenceBookSectionMapCandidateTool")
}
