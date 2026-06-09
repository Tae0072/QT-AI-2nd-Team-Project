// MSA bible-service — 읽기 전용 성경 참조 데이터 서비스(MSA Phase 1).
// Strangler: 추출 동안 모놀리식 bible 도메인과 병존한다. 트래픽 컷오버는 게이트웨이 라우트 분기(Inc2)에서.
//
// 도메인 코드(com.qtai.domain.bible)는 모놀리식에서 복사해 standalone 구동한다.
// bible는 리프 도메인이라 lib-common(web-free 코어)만 의존하고 다른 도메인에 의존하지 않는다.
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
    implementation(project(":lib-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.security:spring-security-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.flywaydb:flyway-core")  // BibleServicePersistenceConfiguration이 Flyway를 직접 호출

    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.flywaydb:flyway-mysql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
