# 2026-06-08 MSA — service-gateway 스캐폴드 (+ JWT sub=null 가드)

## 목표
MSA 진입점 API Gateway(Spring Cloud Gateway) 모듈을 스캐폴드해 `/api/v1/**` 라우팅 토대를 만든다. 더불어 #349 머지 이후 누락된 `JwtTokenVerifier` sub=null 가드를 함께 반영(리뷰 §2).

## 배경
- Phase 0(lib-common·테스트·JWT) 이후 인프라 단계. 게이트웨이는 향후 도메인별 서비스 라우팅의 진입점.
- JWT 가드는 #349 squash 머지 **이후 push분**이라 dev 미반영 → 본 PR에 동봉(리뷰 §2 [WARN], 1줄 가드 + 테스트).

## 작업 내용
1. **fix**: `JwtTokenVerifier` sub=null 가드 + 회귀 테스트(#349 후속).
2. **feat**: `service-gateway` 모듈(멀티모듈 추가). Spring Cloud Gateway 2023.0.3, `/api/v1/**` → 모놀리식(`${GATEWAY_MONOLITH_URI:http://localhost:8080}`) **Strangler 라우팅**. `server.port` 8000.
3. `GatewayApplicationTests` 컨텍스트 로드 스모크 테스트.

## 설계 판단
- 게이트웨이는 **WebFlux(reactive)**. `lib-common`이 servlet `spring-boot-starter-web`을 `api`로 강제하므로 servlet/reactive 충돌을 피하려 **게이트웨이는 lib-common 미의존**. JWT 검증(`JwtTokenVerifier`) 게이트웨이 재사용은 **lib-common의 web 결합을 좁힌 뒤** 후속 — 리뷰의 "lib-common api 의존 폭 축소" 권고와 직결됨이 확인됨.

## 범위
- 브랜치: `feature/msa-service-gateway` (base: `dev`)
- 변경: `settings.gradle.kts` + `service-gateway/` 5파일 + `JwtTokenVerifier` 가드 2파일

## 검증
- `:service-gateway:assemble` — **BUILD SUCCESSFUL** (bootJar)
- `:service-gateway:test` — **BUILD SUCCESSFUL** (컨텍스트·라우트 로드 스모크)
- `:lib-common:test` — JWT 6건(sub=null 가드 포함) 통과

## 미해결 / 후속
- PR 머지 대기
- 후속: lib-common web 결합 축소 → 게이트웨이 JWT 인증 필터(JwtTokenVerifier) · bible 추출(서비스별 라우트 분기)

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
