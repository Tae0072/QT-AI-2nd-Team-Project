# 2026-06-08 MSA service-gateway 스캐폴드 — 결과 보고

## 요약
MSA 진입점 API Gateway(Spring Cloud Gateway) 모듈을 멀티모듈로 스캐폴드하고 `/api/v1/**`를 모놀리식으로 Strangler 라우팅했다. #349 후속 `JwtTokenVerifier` sub=null 가드(리뷰 §2)도 동봉. 본 PR은 코드 7파일 + 문서.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/settings.gradle.kts` | `:service-gateway` 모듈 등록 |
| `service-gateway/build.gradle.kts` | (신규) Spring Cloud Gateway 2023.0.3, Boot 3.3.4. lib-common 미의존(reactive/servlet 충돌 회피) |
| `service-gateway/.../GatewayApplication.java` | (신규) `@SpringBootApplication` 진입점 |
| `service-gateway/.../resources/application.yml` | (신규) port 8000, `/api/v1/**` → `${GATEWAY_MONOLITH_URI:http://localhost:8080}` |
| `service-gateway/.../GatewayApplicationTests.java` | (신규) 컨텍스트 로드 스모크 테스트 |
| `lib-common/.../security/JwtTokenVerifier.java`(+test) | sub=null 가드 + 회귀 테스트(#349 후속, 리뷰 §2) |

## 설계 판단(중요)
- 게이트웨이는 **WebFlux(reactive)**인데 `lib-common`이 servlet `starter-web`을 `api`로 강제 → 게이트웨이에 넣으면 servlet 모드로 부팅돼 Gateway가 깨진다. 그래서 **게이트웨이는 lib-common을 의존하지 않음**. 이는 리뷰가 지적한 **lib-common 의존 폭(api starter 묶음) 축소**가 실제로 필요함을 확인시켜 준다 → 다음 증분에서 lib-common을 web-free 코어로 분리 후 게이트웨이가 `JwtTokenVerifier` 재사용.

## 검증
- `gradlew :service-gateway:assemble` — **BUILD SUCCESSFUL** (bootJar)
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL** (컨텍스트·라우트 정의 로드, RANDOM_PORT 부팅)
- `gradlew :lib-common:test` — **통과** (JwtTokenVerifier 6건 — sub=null 가드 포함)
- 전체 `./gradlew test`는 CI

## 미해결
- PR 머지 대기
- 후속: lib-common web 결합 축소 → 게이트웨이 JWT 인증 필터 · bible 추출(라우트 분기)
