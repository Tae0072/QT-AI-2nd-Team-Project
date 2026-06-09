// 플러그인 버전을 한 곳에서 관리 → 모든 모듈이 버전 없이 id만 선언해 공유한다.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.springframework.boot") version "3.3.4"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

rootProject.name = "qtai-server"

// MSA 멀티모듈 (Strangler) — 기존 모놀리식 root(qtai-server)는 그대로 유지하고,
// 도메인을 모듈로 점진 추출한다. 추출이 끝나면 root 모놀리식 코드를 제거한다.
include("lib-common")     // 공통: 응답/예외, JWT 검증 필터, RestClient 설정
include("service-user")   // 사용자/인증: member, notification, mission (JWT 발급, port 8081)
include("service-bible")  // 읽기전용 콘텐츠: bible, qt, study, music, praise (port 8082)
include("service-note")   // 노트/나눔/신고: note, sharing, report(제출) (port 8083)
