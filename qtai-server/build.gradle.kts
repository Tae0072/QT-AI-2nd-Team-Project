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

// =============================================================================
// JaCoCo 커버리지 게이트 (코드리뷰 2026-06-10 TODO 3 — MSA 전환 때 빠진 게이트 복구)
// - CLAUDE.md §11 명령(test jacocoTestReport / jacocoTestCoverageVerification) 동작 보장.
// - 모듈별 최소 라인 커버리지는 실측값-5%p로 시작(빨간불 방지, 18_코드_품질_게이트.md 대조).
//   상향은 팀 합의로 진행한다.
// =============================================================================
val coverageFloors = mapOf(
    // 실측(2026-06-11) 라인 커버리지에서 5%p 내린 시작 기준
    "lib-common" to "0.58",    // 실측 63.5%
    "service-user" to "0.53",  // 실측 58.7%
    "service-bible" to "0.50", // 실측 55.4%
    "service-note" to "0.53",  // 실측 58.6%
    "service-ai" to "0.14",    // 실측 19.3% — 생성 워커/아웃박스 스켈레톤 비중 큼, 상향 필요
    "admin-server" to "0.12",  // 실측 17.6% — 모놀리식 복사본 전체 포함, 상향 필요
)

subprojects {
    plugins.withId("java") {
        apply(plugin = "jacoco")

        configure<JacocoPluginExtension> {
            toolVersion = "0.8.12"
        }

        tasks.named<Test>("test") {
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)   // CI 집계용
                html.required.set(true)  // 로컬 확인용
            }
        }

        tasks.named<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("jacocoTestReport"))
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = (coverageFloors[project.name] ?: "0.50").toBigDecimal()
                    }
                }
            }
        }
    }
}