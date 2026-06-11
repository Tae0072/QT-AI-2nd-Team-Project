# 2026-06-11 공개 인증 경로 rate limit — 결과 보고

## 요약
코드리뷰 TODO 1(P2) 완료. 공개 인증 경로 3종(kakao 로그인·refresh·admin kakao 로그인)에 IP 단위 1분 고정창 rate limit을 추가했다. 기존 Redis 재사용, fail-open(가용성 우선), 429 공통 봉투, X-Forwarded-For는 게이트웨이 도입 대비 신뢰 토글로 설계.

## 산출물
| 파일 | 설명 |
|------|------|
| `lib-common/.../exception/ErrorCode.java` | `RATE_LIMIT_EXCEEDED`(C0007, 429) 신설 — 04 §6.2 정의 코드명 |
| `service-user/.../security/RateLimitFilter.java` | 고정창 카운터 필터(INCR+EXPIRE, fail-open, XFF 토글) |
| `service-user/.../security/RateLimitProperties.java` | `security.rate-limit.*` 설정(record, 경로별 한도) |
| `service-user/.../user/SecurityConfig.java` | 필터 빈 등록 + 체인 삽입(JWT보다 앞) + 서블릿 이중 등록 차단 |
| `service-user/.../application.yml` | 기본 한도(kakao 10/분, refresh 30/분, admin 10/분) — 전부 env 오버라이드 가능 |
| `service-user/.../security/RateLimitFilterTest.java` | 6케이스 단위 테스트 |

## 검증
- 신규 6건 포함 `:service-user:test`·`:lib-common:test` 전체 통과.
- 부정 경로: 초과 429(체인 중단), Redis 장애 fail-open(통과+warn), 위조 XFF 무시(기본값).

## 미해결 / 후속
- gateway(Lead) 뒤 배치 시 `RATE_LIMIT_TRUST_FORWARDED_FOR=true` env 필요 — gateway PR 머지 시점에 조율.
- 04 명세 §6.2 `RATE_LIMIT_EXCEEDED`는 이미 표에 존재(추가 개정 불필요).

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
