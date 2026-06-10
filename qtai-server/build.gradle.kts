// QT-AI 멀티모듈 루트 (aggregator)
//
// Strangler 전환 완료: 모놀리식 root 소스(qtai-server/src)는 제거되었다.
// 실제 코드는 다음 모듈에 있다:
//   - lib-common   : 공통 응답/예외, JWT 검증 필터, RestClient 설정
//   - service-user : member, notification, mission (JWT 발급, 8081)
//   - service-bible: bible, qt, study, music, praise (읽기 콘텐츠, 8082)
//   - service-note : note, sharing, report(제출) (8083)
//   - service-ai   : ai (사전생성/검증·F-15 Q&A, 8084)
//   - admin-server : 관리자 (모놀리식 복사본, 단일 DB 공유, 8090)
//
// 루트는 자체 소스/애플리케이션이 없고 빌드 묶음 역할만 한다.
// (옛 모놀리식 bootJar Dockerfile은 제거됨 — 배포는 모듈별 Dockerfile/compose/k8s 사용)
plugins {
    base
}

group = "com.qtai"
version = "0.0.1-SNAPSHOT"