# 2026-06-08 MSA Phase 0 — 멀티모듈/lib-common — 결과 보고

## 요약
`qtai-server`를 Gradle 멀티모듈로 전환하고 서비스 공통 코드를 `lib-common` 모듈로 분리했다. 동작 무변경 구조 리팩토링이며 MSA 분리의 0단계 토대다. 본 PR은 코드 8파일 + 문서(설계서·workflow·report)를 포함한다.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/settings.gradle.kts` | `:lib-common` 모듈 등록 |
| `qtai-server/build.gradle.kts` | `implementation(project(":lib-common"))` 의존 추가 |
| `qtai-server/lib-common/build.gradle.kts` | (신규) 공통 모듈 — Spring(web/data-jpa/validation/security) + lombok, Boot BOM 관리 |
| `lib-common/.../common/dto/ApiResponse.java` | 이전(rename) |
| `lib-common/.../common/entity/BaseEntity.java` | 이전(rename) |
| `lib-common/.../common/exception/{ErrorCode,BusinessException,GlobalExceptionHandler}.java` | 이전(rename) |
| `lib-common/.../common/security/{JwtTokenVerifier,AuthenticatedUser}.java` | (신규) 공개키 RS256 JWT 검증기 — 서비스/게이트웨이 재사용. 발급(개인키)은 인증 서비스 잔존 |
| `lib-common/src/test/...`, `src/test/.../LibCommonBoundaryArchTest.java` | (신규) lib-common 단위 테스트 + 경계 ArchUnit |
| `doc/.../MSA분리계획_2026-06-08.md` | 설계 문서(v2) |

## 변경 성격
- **순수 구조 리팩토링**: 패키지명 `com.qtai.common.*` 유지 → 이를 import하는 **102개 파일 무수정**, git이 5개 전부 **100% rename**으로 인식.
- 최신 `dev` 머지(거버넌스 #341 MSA 허용 포함, 충돌 없음).

## 검증
- `gradlew assemble` — **BUILD SUCCESSFUL** (머지 전/후 모두). lib-common jar + app bootJar 정상.
- `gradlew compileTestJava` — **BUILD SUCCESSFUL** (앱 테스트 소스 전체가 새 모듈 구조에서 컴파일 — `GlobalExceptionHandler` 등 이전으로 인한 컨텍스트/스캔 회귀 없음).
- `gradlew :lib-common:test` — **통과** (단위 테스트: ApiResponse/ErrorCode/BusinessException + JwtTokenVerifier 5건 — 유효/만료/refresh거부/서명불일치/role누락).
- `gradlew :test --tests LibCommonBoundaryArchTest` — **통과** (com.qtai.common이 domain/config/security/external/batch에 의존하지 않음 = leaf 강제. jjwt 추가 후에도 유지).
- 전체 `./gradlew test`(Testcontainers/Docker·Redis 필요)는 **CI 파이프라인에서 수행**. 로컬 환경 한계로 PR CI 결과로 확인.

## 미해결
- PR 머지 대기
- 후속: service-gateway 스캐폴드 · bible 추출(1단계) · 각 서비스에서 JwtTokenVerifier 채택(게이트웨이 인증)
