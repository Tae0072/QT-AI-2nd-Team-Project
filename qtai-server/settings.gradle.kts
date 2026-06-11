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

// MSA 멀티모듈 (Strangler) — 도메인 추출 완료. 옛 모놀리식 root 소스(qtai-server/src)는 제거되었고,
// 루트는 빌드 묶음(aggregator) 역할만 한다. 실제 코드는 아래 6개 모듈에 있다.
include("lib-common")     // 공통: 응답/예외, JWT 검증 필터, RestClient 설정
include("service-user")   // 사용자/인증: member, notification, mission (JWT 발급, port 8081)
include("service-bible")  // 읽기전용 콘텐츠: bible, qt, study, music, praise (port 8082)
include("service-note")   // 노트/나눔/신고: note, sharing, report(제출) (port 8083)
include("service-ai")     // AI: ai (사전생성/검증·F-15 Q&A) (port 8084)
include("admin-server")   // 관리자: 모놀리식 복사 후 admin 컨트롤러만 (단일 DB 공유, port 8090)
