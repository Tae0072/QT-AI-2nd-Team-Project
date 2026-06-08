# 2026-06-08 MSA Phase 0 마무리 — lib-common web-free 분리(lib-common-web 신설)

## 목표
공유 코어 `lib-common`에서 servlet/JPA 의존을 떼어내 **web-free 코어**로 만들고, servlet MVC·JPA가 필요한 공통(`GlobalExceptionHandler`, `BaseEntity`)은 신규 모듈 **`lib-common-web`**으로 분리한다. 목적은 WebFlux 게이트웨이·리액티브 서비스가 embedded Tomcat을 끌고 오지 않고도 코어(ApiResponse/ErrorCode/BusinessException/JwtTokenVerifier)를 재사용하게 하는 것.

## 배경
- `feature/msa-service-gateway`·`feature/msa-gateway-resilience` 리뷰에서 확인된 사항: 게이트웨이는 WebFlux라 servlet `starter-web`을 강제하는 `lib-common`을 의존할 수 없어 폴백 envelope를 Map으로 직접 구성하고 있었다. 두 리포트 모두 후속으로 **"lib-common web-free 분리 → 게이트웨이 JWT 필터"**를 명시.
- `lib-common`이 `starter-web`/`data-jpa`/`validation`/`security` 4종을 `api`로 전이 노출 → 의존 폭이 과대. MVC/WebFlux 공용 타입(HttpStatus, MDC, JWT 검증)만 코어에 남기고 servlet 특화는 분리하는 것이 leaf 라이브러리 원칙에 맞다.

## 작업 내용
1. **파일 이동(순수 이동, 내용 무변경)** — `com.qtai.common.exception.GlobalExceptionHandler`(@RestControllerAdvice, 160줄), `com.qtai.common.entity.BaseEntity`(@MappedSuperclass·JPA Auditing, 42줄)를 `lib-common` → `lib-common-web`로 이동. 패키지 경로(`com.qtai.common.*`) 동일 유지 → 소비 측 import 무변경.
2. **lib-common 슬림화(web-free)** — `starter-web`/`starter-data-jpa`/`starter-validation`/`starter-security` 제거. 코어가 실제 쓰는 `org.springframework:spring-web`(HttpStatus 추상화, Tomcat 미포함)와 `org.slf4j:slf4j-api`(MDC traceId)만 `api`로 남김. jjwt(JwtTokenVerifier)는 유지.
3. **lib-common-web 신설** — `java-library`. `api(project(":lib-common"))` + servlet/JPA/validation/security 4 starter를 `api`로. servlet 앱(모놀리식·향후 servlet 서비스)만 의존.
4. **배선 갱신** — `settings.gradle.kts`에 `include(":lib-common-web")` 추가. 모놀리식 root `build.gradle.kts`의 의존을 `project(":lib-common")` → `project(":lib-common-web")`로 교체(lib-common-web이 코어를 전이 노출하므로 기존 import 전부 호환).

## 범위
- 브랜치: `feature/msa-lib-common-webfree` (base: `dev`)
- 변경: `settings.gradle.kts`(+1), root `build.gradle.kts`(의존 1줄), `lib-common/build.gradle.kts`(슬림화), 클래스 2개 이동(GlobalExceptionHandler·BaseEntity), 신규 `lib-common-web/build.gradle.kts`. 코어 로직·인증·소비 측 코드 무영향.
- 관련: MSA 로드맵 Phase 0 마무리(게이트웨이 JWT 필터·bible 추출 전제 조건)

## 검증
- `gradlew :lib-common:test` — **BUILD SUCCESSFUL** (web 스타터 제거 후에도 코어 단위/JWT 테스트 통과)
- `gradlew :lib-common-web:assemble` — **BUILD SUCCESSFUL** (이동 클래스 컴파일)
- `gradlew :compileJava`(모놀리식) — **BUILD SUCCESSFUL** (lib-common-web 의존으로 기존 import 전부 해소)
- 경계 테스트 — **BUILD SUCCESSFUL / 0 failures**: DomainBoundaryArchTest(30), LibCommonBoundaryArchTest(2), ArchitectureBoundaryTest(6) = 38건. 코어가 여전히 leaf(도메인/config/security/external/batch 미의존) 유지.
- 전체 `./gradlew test`(Docker/Redis 필요)는 CI.

## 미해결 / 후속
- PR 머지 대기.
- 후속: 게이트웨이가 코어(`JwtTokenVerifier`) 의존 → **게이트웨이 JWT 인증 필터** 적용(이제 web-free 코어라 WebFlux에서 의존 가능). 이어서 **bible 추출(Phase 1)**.
- 참고: 작업 시작 시 이전 세션 크래시로 `.git/HEAD`(널바이트)·`index` 손상·stale `index.lock` 발견 → 복구 후 진행(원본은 `.git/*.corrupt.bak` 백업). 코드 변경과 무관한 git 메타데이터 손상이었음.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
