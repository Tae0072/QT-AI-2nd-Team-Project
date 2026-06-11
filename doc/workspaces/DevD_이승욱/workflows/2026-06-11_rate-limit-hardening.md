# 2026-06-11 rate limit 후속 보강 3건 (bugfix/member-rate-limit-hardening)

## 목표·배경
PR #486(공개 인증 경로 rate limit) 자동 리뷰 후속 3건 — 머지 차단 사유는 아니었으나 후속 PR로 합의된 항목.

## 작업 내용
1. **XFF 신뢰 정책 확정(①)** — '첫 IP 신뢰' → '**마지막 IP 신뢰**'로 변경. 마지막 항목은 직전 신뢰 프록시(게이트웨이)가 기록한 실제 peer라, 게이트웨이가 XFF를 append(nginx `$proxy_add_x_forwarded_for`)하든 덮어쓰든(`$remote_addr`) 클라이언트 선두 위조 값으로 한도를 우회할 수 없다. 전제(신뢰 프록시 1단계)와 다단 프록시 시 재검토 필요성을 javadoc에 명시 — 게이트웨이 구성과 무관하게 안전해 Lead gateway PR과 조율 불필요해짐.
2. **시큐리티 체인 통합 테스트(②)** — `RateLimitIntegrationTest` 3건: 한도 초과 시 컨트롤러 진입 전 429+공통 봉투(C0007) / 한도 내 체인 통과(401 비즈니스 응답으로 증명) / Redis 장애 fail-open에도 로그인 경로 생존. 발견사항: 테스트 클래스패스에서 `src/test/resources/application.yml`이 main yml을 **대체**해 rate-limit 규칙이 비므로 테스트에서 규칙을 명시 주입(주석 기록).
3. **INCR/EXPIRE 원자화(③)** — Lua 스크립트(`INCR` + count==1이면 `EXPIRE`) 단일 호출로 전환. 비원자 구간에서 프로세스/커넥션 중단 시 TTL 없는 키가 영구 누적되던 틈 제거.

## 검증
- 단위 6건(스크립트 호출·TTL 인자·마지막 IP 검증으로 갱신) + 통합 3건 신규, `:service-user:test` 89건 전체 통과

## 미해결 / 후속
- 다단 프록시(CDN 등) 도입 시 XFF 정책 재검토(현재 전제: 신뢰 프록시 1단계)

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
