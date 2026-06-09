# 2026-06-09 MSA Phase 1 — bible-service 토큰 회전 grace window (Inc3b)

## 목표
공유 토큰(서비스 신뢰 자격, Inc3a)을 **무중단 회전**할 수 있게 한다. 단일 토큰값만 허용하던 필터를 현재값+직전값 동시 허용으로 확장해, 회전 중에도 401 없이 전환한다. 운영 진입(실트래픽) 전 충족해야 할 토대 항목.

## 배경
- Inc3a에서 토큰을 서비스 신뢰 자격으로 격상했으나 단일값만 허용 → 토큰 교체 시 게이트웨이·서비스 동시 갱신이 불가능해 회전 순간 401 위험.
- 리뷰 후속 권고 + 운영 진입 체크리스트의 추적 항목(`bible-service-운영진입-체크리스트.md`).

## 작업 내용
1. **필터 grace window** — `GatewayHeaderAuthenticationFilter`에 직전 토큰(`previousGatewayToken`) 추가. `tokenMatches`가 현재값 **또는** 직전값(설정 시)과 상수시간 비교 일치하면 허용(단락 평가 회피). 기존 2-arg 생성자는 유지(현재값만), 3-arg 생성자 추가.
2. **설정** — `qtai.bible.gateway.previous-token`(`QTAI_BIBLE_GATEWAY_PREVIOUS_TOKEN`, 기본 빈값). `BibleServiceInboundConfiguration`이 현재·직전값을 함께 주입.
3. **부분 신원 헤더 점검(Inc3a 후속) 반영** — 운영 진입 체크리스트에 "bible 다운스트림은 신원/role 인가 미사용 → 부분 헤더 침묵 우회 위험 없음, 단 향후 member 인가 추가 시 SYSTEM 통과 재검토" 불변식 기록.

## 회전 절차(무중단)
새 토큰 N, 기존 토큰 O일 때: ① 서비스 `shared-token=N`, `previous-token=O` 배포(둘 다 허용) → ② 게이트웨이 `GATEWAY_BIBLE_SHARED_TOKEN=N` 전환 → ③ 모든 호출자 전환 확인 후 서비스 `previous-token` 비움(O 폐기).

## 범위
- 브랜치: `feature/msa-bible-token-rotation` (base: `dev`)
- 변경: 필터 + InboundConfiguration + application.yml + 테스트(+4) + 운영진입 체크리스트(신규) + workflow/report. 게이트웨이·소비자·코어 무변경.
- 관련: bible 추출 Inc3b(운영 토대)

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (25건)**: 필터 16(+4: 직전토큰 통과 / 현재토큰 통과 / 미지토큰 401 / 직전 미설정 시 거부) + 토큰슬라이스 4 + inbound슬라이스 2 + persistence 1 + 캐시 1 + contextLoads 1
- 기존 동작 무변경(2-arg 생성자 유지, 단일 토큰 의미 불변).

## 미해결 / 후속
- **Inc3c**: `external/bible` HTTP 클라이언트 + api 어댑터(`@Primary`, `qtai.bible.client.mode=http` 게이트, 기본 inprocess=무변경). 단, getVerse(Long)/ListChapters/SearchBible은 bible-service에 대응 엔드포인트가 없어 **소비자 사용 범위 조사 후 엔드포인트 추가 여부 결정** 필요.
- Inc3d(qt/note/study 소비자 전환, 오너 협의) → Inc4(DB 분리) → Inc5(모놀리식 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
