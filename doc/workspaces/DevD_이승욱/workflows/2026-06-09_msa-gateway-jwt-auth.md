# 2026-06-09 MSA 게이트웨이 JWT 인증 필터

## 목표
`service-gateway`(WebFlux)에서 `/api/v1/**` 요청의 access token을 **공개키로 검증**하고, 검증된 신원을 다운스트림에 전달한다. 인증을 게이트웨이 경계로 끌어올려 각 서비스/모놀리식이 신원 검증을 중복하지 않도록 하는 MSA 인증 토대.

## 배경
- `feature/msa-lib-common-webfree`(#353)가 dev에 머지되어 `lib-common`이 web-free 코어가 됨 → WebFlux 게이트웨이가 servlet `starter-web` 충돌 없이 코어(`JwtTokenVerifier`/`AuthenticatedUser`/`ApiResponse`/`ErrorCode`)를 의존 가능. 게이트웨이 스캐폴드(#350)·하드닝 PR의 후속으로 명시돼 있던 작업.
- 토큰 구조는 발급자(`com.qtai.security.JwtProvider`)와 동일: `sub=memberId`, `role` claim, `type=access|refresh`. 발급(개인키)은 인증 서비스에만, 게이트웨이는 **검증 전용**(CLAUDE.md §5).

## 작업 내용
1. **게이트웨이 lib-common 의존** — `service-gateway/build.gradle.kts`에 `implementation(project(":lib-common"))` 추가, 헤더 주석을 web-free 의존 반영으로 갱신.
2. **JwtTokenVerifier 빈** — `GatewayJwtConfig`가 `gateway.jwt.public-key`(`${JWT_PUBLIC_KEY}` 주입)로 검증기 빈 생성. 저장소에 평문 키 미저장.
3. **JwtAuthenticationFilter**(reactive `WebFilter`, `HIGHEST_PRECEDENCE+100`) —
   - `/api/v1/**` 보호, 단 `/api/v1/auth/**`(Kakao 로그인 시작·토큰 재발급)는 미인증 허용.
   - `Authorization: Bearer` access token을 공개키 검증. 누락·`Bearer` 미접두·만료·서명 불일치·refresh 타입·claim 누락이면 **401 표준 envelope**(`ErrorCode.UNAUTHORIZED` = M0002, `ApiResponse`로 직렬화).
   - 검증 성공 시 다운스트림에 `X-Member-Id`/`X-Member-Role` 주입. **클라이언트가 보낸 동일 헤더는 항상 제거**(인증 예외 경로 포함)해 신원 위조 차단.
   - 로그에 token 값 미기록(CLAUDE.md §9).
4. **테스트 키 런타임 주입** — 모놀리식의 `com.qtai.support.JwtTestKeysContextCustomizerFactory`와 동일 패턴으로 게이트웨이용 `JwtTestKeysContextCustomizerFactory`(+ `META-INF/spring.factories` 등록) 추가. 테스트 부팅 시 RSA 공개키를 생성해 `gateway.jwt.public-key`로 주입한다. **저장소에 평문 키를 커밋하지 않는다**(CLAUDE.md §8 — 테스트 키의 운영 키 승격 위험 차단). 게이트웨이는 검증 전용이라 공개키만 주입.

## 범위
- 브랜치: `feature/msa-gateway-jwt-auth` (base: `dev`)
- 변경: 게이트웨이 build(+lib-common) / 신규 `GatewayJwtConfig`·`JwtAuthenticationFilter` / main application.yml(jwt.public-key) / test application.properties / 신규 필터 테스트. 코어(lib-common)·모놀리식·다른 서비스 무변경.
- 관련: MSA 로드맵 Phase 0 인증 토대(게이트웨이 스캐폴드 #350 후속)

## 검증
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL / 0 failures** (13건)
  - `JwtAuthenticationFilterTest`(10): 유효통과+헤더주입 / 위조헤더 덮어쓰기 / 헤더없음 401·M0002 / Bearer미접두 401 / 만료 401 / 서명불일치 401 / refresh 401 / auth경로 통과 / auth경로 스푸핑헤더 제거 / 비-API 경로 통과
  - 기존 `GatewayApplicationTests`(1, 컨텍스트 로드 — 새 검증기 빈 배선 확인) / `GatewayFallbackControllerTest`(1) / `GatewayRouteTest`(1, 라우트 설정 유지 확인)
- gitleaks: 테스트 공개키는 `BEGIN PRIVATE KEY` 헤더 없는 base64 → 기본 룰 비대상(평문 비밀·개인키 아님). CI에서 최종 확인.
- 전체 `./gradlew test`(Docker/Redis 필요)는 CI.

## 미해결 / 후속
- PR 머지 대기.
- 후속: 다운스트림(모놀리식/서비스)에서 게이트웨이 주입 `X-Member-Id`/`X-Member-Role` 신뢰·소비(게이트웨이 미경유 직접 호출 차단 전제). 이어서 **bible 추출(Phase 1)** — 라우트 분기.
- 비고: 게이트웨이는 권한(role)별 인가까지는 하지 않고 **인증(authn)** 과 신원 전달만 담당. 세부 인가(admin 권한 등)는 각 서비스 책임.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
