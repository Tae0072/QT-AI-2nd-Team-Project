# 2026-06-09 게이트웨이 JWT 인증 필터 harden — 결과 보고

## 요약
게이트웨이 JWT 인증 필터 PR(#364, 머지 완료)의 리뷰 WARN 4건을 보완했다. 회귀 안전망·관측성·운영 친화성 강화의 소규모 하드닝. refresh 블랙리스트 1건은 별도 트랙으로 분리.

## 산출물

| 파일 | 설명 |
|------|------|
| `service-gateway/.../filter/JwtAuthenticationFilter.java` | 빈 토큰 사전 분기 + 인증 실패 `log.debug`(사유·path, token값 제외) |
| `service-gateway/.../config/GatewayJwtConfig.java` | 공개키 blank 가드 — 명확한 메시지로 fail-fast |
| `service-gateway/src/main/resources/application.yml` | `gateway.jwt.public-key: ${JWT_PUBLIC_KEY:}`(빈 기본값 → placeholder 에러 회피) |
| `service-gateway/.../filter/JwtAuthenticationFilterTest.java` | role누락·subject누락·빈Bearer 401 테스트 추가(10→13) |

## 변경 성격
- **방어/명확화**: 빈 Bearer 토큰을 검증기 호출 전에 401로 차단(동작은 동일, 경로·의도 명확화).
- **관측성**: 인증 실패 사유를 `debug`로 기록(token 값 미포함, CLAUDE.md §9). 401이 흔한 미인증 프로브임을 감안해 레벨을 낮춰 스팸 방지.
- **운영 친화성**: `JWT_PUBLIC_KEY` 미설정 시 cryptic placeholder 에러 대신 "키를 주입하세요" 명확 메시지로 起動 차단(fail-fast).
- **회귀 안전망**: claim 누락(role/subject) 경로를 게이트웨이 필터 레벨에서 단언(검증기 단위 테스트와 별개로 필터 통합 경로 보호).
- 필터의 검증·헤더 주입·스푸핑 차단 핵심 로직은 불변.

## 검증
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL / 0 failures (16건)**
  - `JwtAuthenticationFilterTest` **13** (기존 10 + role누락/subject누락/빈Bearer)
  - `GatewayApplicationTests`/`FallbackControllerTest`/`GatewayRouteTest` 각 1 — 가드(빈 기본값) 후에도 컨텍스트·라우트 유지
- 전체 `./gradlew test`(Docker/Redis)는 CI

## 미해결
- PR 머지 대기
- 별도 트랙: refresh 토큰 블랙리스트/폐기(인증 서비스+다운스트림), 다운스트림 헤더 신뢰·소비 → bible 추출(Phase 1)
