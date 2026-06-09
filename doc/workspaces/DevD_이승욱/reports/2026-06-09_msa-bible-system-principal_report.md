# 2026-06-09 bible-service SYSTEM 주체 인증(Inc3a) — 결과 보고

## 요약
Inc3(소비자 HTTP 전환)의 인증 토대로, bible-service deny-by-default 필터에 **SYSTEM 주체**를 허용했다. 공유 토큰 환경에서 유효한 `X-Gateway-Token`이면 사용자 헤더 없이도 통과(배치/캐시 경계의 서비스-to-서비스 호출). 토큰=서비스 신뢰 자격으로 격상(설계 결정 B, 옵션 1). 필터 1파일 + 테스트 + 설계 문서.

## 산출물

| 파일 | 설명 |
|------|------|
| `bible-service/.../GatewayHeaderAuthenticationFilter.java` | 토큰 우선 로직 — 유효 토큰=신뢰(사용자/SYSTEM 모두), 토큰 미설정 시 신원 헤더 요구 |
| `bible-service/.../GatewayHeaderAuthenticationFilterTest.java` | (+2) 토큰+헤더없음 SYSTEM 통과 / 토큰미설정 SYSTEM 401 |
| `bible-service/.../BibleGatewayTokenSliceTest.java` | (+1) 토큰만으로 SYSTEM 호출 200 통합 |
| `doc/.../bible-service-Inc3-소비자HTTP어댑터-설계_2026-06-09.md` | (신규) Inc3 설계(빈 선택·서비스 인증·증분 분할) |

## 변경 성격
- **보안 모델 변경(⚠ Lead 합의 사안)**: Inc2 필터는 `X-Member-Id` 필수였으나, 토큰을 1차 신뢰 기준으로 격상. bible는 읽기 전용 참조 데이터라 member 단위 인가가 없고, 신뢰 증명은 서버 비밀 토큰(위조 불가·상수시간 비교).
- **하위 호환**: 토큰 미설정(dev)은 기존대로 헤더 요구. 토큰 설정+불일치=401 의미 불변. **SYSTEM 경로만 추가**되어 기존 테스트 전부 유지.
- **audit**: SYSTEM 호출(헤더 없음)은 `SYSTEM_BATCH` 주체로 debug 로깅(가짜 사용자 신원 미사용).
- 소비자·게이트웨이·코어 무변경.

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (18건)**: 필터 9 + 토큰슬라이스 4 + inbound슬라이스 2 + persistence 1 + 캐시 1 + contextLoads 1

## 미해결
- Lead/리뷰 합의(보안 모델 변경).
- Inc3b~3d(external/bible HTTP 클라이언트 + 어댑터 + qt/note/study 전환, 기본 inprocess=무변경) → Inc4(DB 분리) → Inc5(모놀리식 제거).
