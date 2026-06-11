# 2026-06-11 rate limit 후속 보강 3건 — 결과 보고

## 요약
PR #486 리뷰 후속 3건 완료: ① XFF '마지막 IP 신뢰'로 위조 우회 차단(append/덮어쓰기 무관 — gateway 조율 불필요화) ② 시큐리티 체인 통합 테스트 3건 ③ Lua 스크립트로 INCR+EXPIRE 원자화(TTL 없는 키 누적 제거).

## 산출물
| 파일 | 설명 |
|------|------|
| `security/RateLimitFilter.java` | Lua 원자 스크립트, 마지막 IP 신뢰 + 정책 javadoc |
| `security/RateLimitFilterTest.java` | 스크립트 호출/TTL 인자/마지막 IP 기준으로 6건 갱신 |
| `user/RateLimitIntegrationTest.java` | 체인 통합 3건(429 봉투·체인 통과·fail-open 생존) 신규 |

## 검증
- `:service-user:test` 89건 전체 통과
- 발견: 테스트 클래스패스는 test yml이 main yml을 대체 → rate-limit 규칙 테스트 명시 주입(주석 기록)

## 미해결 / 후속
- 다단 프록시 도입 시 XFF 정책 재검토(전제: 신뢰 프록시 1단계)

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
