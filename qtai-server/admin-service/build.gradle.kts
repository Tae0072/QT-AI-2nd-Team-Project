// MSA admin-service — 관리자 웹(admin-web) 백엔드 + 감사(audit) 소유 서비스 (MSA Phase 1 스캐폴드).
// Strangler: 추출 동안 모놀리식 admin/audit 도메인과 병존한다. 트래픽 컷오버는 게이트웨이 라우트 분기에서.
//
// 스캐폴드 단계 — 독립 부팅 + health 만 검증한다. persistence(전용 DataSource/EMF)·도메인 빈은 후속 작업(1-2~)에서
// @ConditionalOnProperty로 게이트해 추가한다. admin은 lib-common(web-free 코어)만 의존한다.
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

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
