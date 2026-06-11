// admin-server — 관리자 서버 (모놀리식 통째 복사 후 admin 컨트롤러만 잔류, 같은 단일 DB 공유).
// 회의록 2026-06-09 §6 강사님 방식: 모놀리식 원본 복사 → admin API만 남김. lib-common 비의존 standalone.
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
    // 모놀리식과 동일 스택(통째 복사) — lib-common에 의존하지 않고 자체 com.qtai.common 보유.
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // Flyway — DB 마이그레이션(운영 MySQL). 로컬/테스트 H2에서는 비활성(application.yml).
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // JWT — 관리자도 회원 토큰(RS256)으로 1차 인증, 검증만 수행.
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
