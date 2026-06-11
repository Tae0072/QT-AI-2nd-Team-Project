# 2026-06-11 공개 인증 경로 rate limit (feature/member-auth-rate-limit)

## 목표·배경
코드리뷰 TODO 1 (P2, `2026-06-10_코드리뷰_TODO_이승욱.md`): `POST /api/v1/auth/kakao`·`/api/v1/auth/refresh`·`/api/v1/admin/auth/kakao`가 무제한 호출 가능 — 카카오 토큰 무차별 대입·refresh 남용의 1차 방어선 부재.

## 작업 내용
- `RateLimitFilter`(service-user `com.qtai.security`): IP+경로 1분 고정창 카운터. 키 `rl:{path}:{ip}:{epochMinute}`, INCR 후 첫 증가면 60s EXPIRE. 기존 Redis(StringRedisTemplate) 재사용 — 별도 라이브러리 없음.
- 한도 초과 → 공통 봉투 429(`ErrorCode.RATE_LIMIT_EXCEEDED` C0007 신설 — 04 명세 §6.2에 이미 정의된 코드명 재사용, lib-common 추가는 enum 1줄).
- **fail-open**: Redis 장애 시 카운트 포기하고 통과(로그인 가용성 우선). 원인은 예외 클래스명만 warn 로그(IP·토큰 미기록, §9).
- **X-Forwarded-For**: `security.rate-limit.trust-forwarded-for`(기본 false) 토글 — 직접 노출 시 헤더 위조 우회 방지, nginx gateway(Lead 작업) 뒤에 설 때 env로 켠다. gateway PR과 머지 순서 무관.
- 경로·한도는 yml 주입(`security.rate-limit.rules`, kakao 10/분·refresh 30/분·admin kakao 10/분 기본, env 오버라이드).
- 서블릿 자동 등록 비활성(`FilterRegistrationBean(enabled=false)`) — 시큐리티 체인과 이중 실행 시 카운트 2배 방지.

## 범위
- 브랜치: `feature/member-auth-rate-limit` (origin/dev 분기), PR 대상 dev
- lib-common 1파일(ErrorCode 1줄) + service-user 4파일(필터·프로퍼티·SecurityConfig·yml) + 테스트 1파일

## 검증
- `RateLimitFilterTest` 6건: 한도 내 통과+EXPIRE / 초과 429·체인 중단 / 분 경계 창 리셋 / Redis 장애 fail-open / 비대상 경로 스킵 / XFF 신뢰 토글
- `:service-user:test`·`:lib-common:test` 전체 통과 (시큐리티 컨텍스트 로드 포함)
- 수동: dev 기동 후 `curl` 반복 호출로 11번째 429 확인 예정

## 미해결 / 후속
- gateway 도입 시 운영 env에 `RATE_LIMIT_TRUST_FORWARDED_FOR=true` 설정 필요(Lead gateway PR과 조율)
- 정밀한 슬라이딩 윈도/버스트 허용이 필요해지면 bucket4j 검토(금지기술 아님)

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
