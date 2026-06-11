# 2026-06-08 MSA lib-common web-free 분리 — 결과 보고

## 요약
공유 코어 `lib-common`을 **web-free**로 슬림화하고, servlet MVC·JPA가 필요한 공통(`GlobalExceptionHandler`, `BaseEntity`)을 신규 모듈 **`lib-common-web`**으로 분리했다. WebFlux 게이트웨이/리액티브 서비스가 embedded Tomcat을 끌고 오지 않고도 코어(ApiResponse/ErrorCode/BusinessException/JwtTokenVerifier)를 재사용할 수 있게 하는 Phase 0 마무리 작업이다. 본 PR은 빌드 배선 3 + 클래스 이동 2 + 신규 모듈 1 + 문서.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/lib-common/build.gradle.kts` | (슬림화) `starter-web`/`data-jpa`/`validation`/`security` 4종 제거 → `spring-web`(HttpStatus, Tomcat 미포함) + `slf4j-api`만 `api`로. jjwt 유지 |
| `qtai-server/lib-common-web/build.gradle.kts` | (신규) `api(project(":lib-common"))` + servlet/JPA/validation/security 4 starter. servlet 앱만 의존 |
| `qtai-server/lib-common-web/.../GlobalExceptionHandler.java` | (이동) lib-common → lib-common-web. @RestControllerAdvice, 내용 무변경, 패키지 동일 |
| `qtai-server/lib-common-web/.../BaseEntity.java` | (이동) lib-common → lib-common-web. @MappedSuperclass·JPA Auditing, 내용 무변경, 패키지 동일 |
| `qtai-server/settings.gradle.kts` | `include(":lib-common-web")` 추가 |
| `qtai-server/build.gradle.kts` | 모놀리식 의존 `:lib-common` → `:lib-common-web`(코어를 전이 노출 → 기존 import 전부 호환) |

## 변경 성격
- **의존 폭 축소(leaf 정상화)**: 코어가 servlet/JPA 스타터를 전이 노출하지 않음 → WebFlux 게이트웨이가 코어 의존 시 servlet `starter-web`과 충돌하지 않음. MVC/WebFlux 공용 타입만 코어에 잔존.
- **무영향 이동**: 이동 클래스 2개는 패키지 경로(`com.qtai.common.*`)를 그대로 유지 → 소비 측 import 무변경. 모놀리식은 lib-common-web을 의존해 동일 클래스 전부 접근.
- **로직 변경 없음**: GlobalExceptionHandler·BaseEntity 내용, 인증, 코어 타입 전부 불변. 순수 모듈 경계 재배치.

## 리뷰 후속 반영(머지 전 보강)
초기 분리 PR 리뷰의 회귀 안전망 지적 3건을 같은 브랜치에 보강:

| 지적 | 조치 |
|------|------|
| (a) 이동 클래스 회귀 테스트가 함께 옮겨졌는지 불명확 | `GlobalExceptionHandlerTest`(9건)를 모놀리식 → `lib-common-web/src/test`로 이동(클래스와 동일 모듈). lib-common-web에 test 소스셋·의존 추가. `EntityCompilationTest`는 도메인 엔티티 검증이라 모놀리식 잔존 |
| (b) lib-common-web boundary ArchUnit 신규 부재 | `LibCommonWebBoundaryArchTest` 신설 — lib-common-web가 도메인/앱 기술영역에 의존 못 하게 자기 소스셋에서 강제 |
| (c) JWT 책임분리 보안 주석 제거 | `lib-common/build.gradle.kts`에 `발급(개인키)은 인증 서비스에만.` 주석 복원 |

## 검증
- `gradlew :lib-common:test` — **BUILD SUCCESSFUL** (web 스타터 제거 후 코어 단위/JWT 테스트 통과)
- `gradlew :lib-common-web:test` — **BUILD SUCCESSFUL / 0 failures**: GlobalExceptionHandlerTest(9) + LibCommonWebBoundaryArchTest(2) = **11건**
- `gradlew :compileJava`(모놀리식) — **BUILD SUCCESSFUL** (lib-common-web 의존으로 기존 import 해소)
- 모놀리식 경계 테스트 **0 failures**: DomainBoundaryArchTest 30 / LibCommonBoundaryArchTest 2 / ArchitectureBoundaryTest 6 = **38건** → 코어 leaf 유지 확인
- 전체 `./gradlew test`(Docker/Redis)는 CI

## 미해결
- PR 머지 대기
- 후속: web-free 코어 위에 **게이트웨이 JWT 인증 필터**(WebFlux에서 `JwtTokenVerifier` 의존 가능) → **bible 추출(Phase 1)**
