# bible-service Inc2 라우트 컷오버 설계 (MSA Phase 1) — 2026-06-09

> Inc1(스캐폴드)·Inc1b(persistence 검증) 머지 완료. Inc2는 **실트래픽을 모놀리식 → bible-service로 전환**한다.
> 작성: DevD 이승욱 (Lead 강태오 계정) · 협의: 게이트웨이/bible 오너 + Lead
> 근거: `bible-service-추출-설계_2026-06-09.md`, 게이트웨이 JWT 인증(#364)

## 1. 목표
게이트웨이가 `/api/v1/bible/**`를 bible-service로 라우팅하고, bible-service가 인증 필터 뒤에서 정상 서빙하도록 한다. DB는 Inc4 전까지 **모놀리식 공유 DB**의 bible 테이블을 읽는다(Strangler — DB 분리는 나중).

## 2. 컷오버 순서 (안전한 Strangler)
서비스가 "받을 준비"가 된 뒤 트래픽을 돌린다 — 순서를 지켜야 bible 엔드포인트 공백이 없다.
1. **bible-service 서빙 준비** (Inc2a): `qtai.bible.inbound.enabled=true` + `persistence.enabled=true`(→ 공유 DB) 활성 시 `BibleController`가 `/api/v1/bible/**`를 인증 필터 뒤에서 처리함을 슬라이스 테스트로 검증. 이 시점 게이트웨이는 **여전히 모놀리식**으로 라우팅(트래픽 없음).
2. **게이트웨이 라우트 전환** (Inc2b): bible 전용 라우트(`/api/v1/bible/**` → bible-service)를 모놀리식 catch-all보다 **앞 순서**로 추가 + `X-Gateway-Token` 주입(2차 방어선 활성) + CircuitBreaker 폴백. 이게 실제 컷오버.

## 3. 게이트웨이 변경 (Inc2b)
- **라우트 추가** (catch-all 이전 우선순위):
  ```yaml
  - id: bible-service
    uri: ${GATEWAY_BIBLE_URI:http://localhost:8082}
    predicates:
      - Path=/api/v1/bible/**
    filters:
      - name: CircuitBreaker
        args: { name: bibleCb, fallbackUri: forward:/__fallback }
      - AddRequestHeader=X-Gateway-Token, ${GATEWAY_BIBLE_SHARED_TOKEN:}
  ```
  Spring Cloud Gateway는 라우트를 순서대로 평가하므로 bible 라우트를 monolith(`/api/v1/**`)보다 먼저 둔다.
- **X-Gateway-Token 주입**: 공유 토큰을 env(`GATEWAY_BIBLE_SHARED_TOKEN`)로 주입 → bible-service의 2차 방어선(`qtai.bible.gateway.shared-token`)과 동일 값. 평문 키 미커밋.
- JWT 인증 필터(#364)는 그대로 — `X-Member-Id`/`X-Member-Role` 주입은 모든 `/api/v1/**`에 적용되므로 bible 라우트도 인증 후 전달됨.

## 4. bible-service 변경 (Inc2a)
- 코드 변경 없음(스캐폴드에 이미 구현). **활성화는 배포 설정**(`QTAI_BIBLE_INBOUND_ENABLED=true`, `QTAI_BIBLE_PERSISTENCE_ENABLED=true`, DB URL=공유 모놀리식 DB, `QTAI_BIBLE_GATEWAY_SHARED_TOKEN=<secret>`).
- **검증(슬라이스 테스트)**: inbound+persistence 활성 컨텍스트에서 MockMvc로
  - `GET /api/v1/bible/books` + `X-Member-Id`/`X-Member-Role` → 200 + 표준 envelope
  - 헤더 없음 → 401 + M0002 (GatewayHeaderAuthenticationFilter deny-by-default 실증)

## 5. 롤백 전략
- 게이트웨이 라우트는 설정 변경만으로 즉시 모놀리식 복귀 가능(코드 롤백 불필요). 모놀리식 bible 엔드포인트는 Inc5까지 **병존**.
- CircuitBreaker 폴백으로 bible-service 장애 시 503 표준 envelope(빈 응답 방지).

## 6. 위험 / 협의
- **라우트 순서**: bible 라우트가 monolith catch-all보다 뒤면 절대 매칭 안 됨 → 순서 회귀 테스트 필수(`GatewayRouteTest` 확장).
- **공유 DB 동시 접근**: Inc2~Inc3 동안 모놀리식·bible-service가 같은 bible 테이블을 읽음(읽기 전용이라 경합 낮음). 쓰기 없음.
- **토큰 동기화**: 게이트웨이 주입 토큰과 서비스 기대 토큰 불일치 시 전량 401 → 배포 설정 점검 체크리스트 필요.
- 소비자(qt/note/study/ai)는 아직 in-process 호출(Inc3에서 HTTP 어댑터 전환) — Inc2는 외부 HTTP 경로만 전환.

## 7. 증분 분할
| Inc | 범위 | 모듈 | 트래픽 |
|-----|------|------|--------|
| **2a** | bible-service inbound 슬라이스 검증(서빙 준비) | bible-service(테스트) | 없음 |
| **2b** | 게이트웨이 bible 라우트 + 토큰 주입 + 라우트 테스트 | service-gateway | **컷오버** |

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
