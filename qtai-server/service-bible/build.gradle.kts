// service-bible — 읽기전용 콘텐츠 서비스 (bible, qt, study, music, praise).
// ③단계: bible 도메인을 파일럿으로 이전(JPA + 캐시 + 단일 DB).
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

    // Caffeine — 로컬 캐시 (bible_books 등 불변 데이터)
    implementation("com.github.ben-manes.caffeine:caffeine")

    // QT 일자별 스냅샷 오브젝트 스토리지(S3 호환: 로컬 MinIO / 배포 S3·R2) — S3 어댑터에서만 사용.
    // 기본(로컬 파일) 어댑터는 이 의존 없이 동작. (회의록 2026-06-09 §2)
    implementation(platform("software.amazon.awssdk:bom:2.46.10"))
    implementation("software.amazon.awssdk:s3")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 단일 DB: 로컬/테스트 H2, 운영 MySQL (접속정보 env 주입)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ArchUnit — 도메인 경계 자동 검증 (CLAUDE.md §3, §4)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // Spring Security 테스트 지원 (MockMvc 인증 주입: SecurityMockMvcRequestPostProcessors)
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
