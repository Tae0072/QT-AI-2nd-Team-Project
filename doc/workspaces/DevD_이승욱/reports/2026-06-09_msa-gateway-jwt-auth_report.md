# 2026-06-09 MSA 게이트웨이 JWT 인증 필터 — 결과 보고

## 요약
`service-gateway`(WebFlux)에 JWT 인증을 추가했다. web-free 코어(#353) 덕에 게이트웨이가 `lib-common`을 의존해 `JwtTokenVerifier`로 access token을 **공개키 검증**하고, 검증된 신원을 `X-Member-Id`/`X-Member-Role`로 다운스트림에 주입한다. 인증을 게이트웨이 경계로 끌어올리는 MSA 인증 토대.

## 산출물

| 파일 | 설명 |
|------|------|
| `service-gateway/build.gradle.kts` | `implementation(project(":lib-common"))` 추가(web-free 코어 의존), 헤더 주석 갱신 |
| `service-gateway/.../config/GatewayJwtConfig.java` | (신규) `gateway.jwt.public-key`(`${JWT_PUBLIC_KEY}`)로 `JwtTokenVerifier` 빈 생성 — 검증 전용 |
| `service-gateway/.../filter/JwtAuthenticationFilter.java` | (신규) reactive `WebFilter` — Bearer 검증, 401 표준 envelope, 신원 헤더 주입+스푸핑 제거 |
| `service-gateway/src/main/resources/application.yml` | `gateway.jwt.public-key: ${JWT_PUBLIC_KEY}` 추가 |
| `service-gateway/src/test/resources/application.properties` | (신규) 컨텍스트 로드용 테스트 공개키(개인키 미포함) |
| `service-gateway/.../filter/JwtAuthenticationFilterTest.java` | (신규) 런타임 키쌍 기반 필터 단위 테스트 10건 |

## 변경 성격
- **인증 경계 상향(authn)**: 게이트웨이가 `/api/v1/**`의 access token을 공개키로 검증. `/api/v1/auth/**`(Kakao 로그인 시작·재발급)만 미인증 허용(CLAUDE.md §5).
- **검증 전용 책임분리**: 게이트웨이는 공개키만 보유, 발급(개인키)은 인증 서비스에만. 공개키는 환경변수 주입(저장소 평문 키 금지).
- **신원 위조 차단**: 검증 성공 시에만 게이트웨이가 `X-Member-Id`/`X-Member-Role` 설정. 클라이언트가 보낸 동일 헤더는 (인증 예외 경로 포함) 항상 제거 → 다운스트림은 게이트웨이 주입 헤더를 신뢰 가능.
- **envelope 일관성**: 401 본문을 `ApiResponse.error(M0002, ...)`로 직렬화 → 모놀리식/폴백과 동일 스키마. (web-free 코어 재사용의 직접 효과)
- 인가(role 기반)는 게이트웨이 범위 밖 — 세부 권한은 각 서비스 책임.

## 검증
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL / 0 failures (13건)**
  - `JwtAuthenticationFilterTest` **10**: 유효통과+헤더주입 / 위조헤더 검증값 덮어쓰기 / 헤더없음 401·M0002 / Bearer미접두 401 / 만료 401 / 서명불일치 401 / refresh 401 / auth경로 무토큰 통과 / auth경로 스푸핑헤더 제거 / 비-API 통과
  - `GatewayApplicationTests` 1(컨텍스트 로드 — 검증기 빈 배선) / `GatewayFallbackControllerTest` 1 / `GatewayRouteTest` 1(라우트 설정 유지)
- 전체 `./gradlew test`(Docker/Redis)는 CI

## 미해결
- PR 머지 대기
- 후속: 다운스트림에서 게이트웨이 주입 헤더 신뢰·소비 → **bible 추출(Phase 1)** 라우트 분기
