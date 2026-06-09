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

## 리뷰 후속 보강(2차)
| 권장 | 조치 |
|------|------|
| 감사 트레일(debug→감사/INFO) | SYSTEM 호출을 전용 감사 로거(`com.qtai.audit.bible`) INFO로 기록(주체·method·path, token/PII 제외) |
| 부분 신원 헤더 시나리오 테스트 | 토큰 유효+role 누락 → 통과 / 토큰 미설정+id만 → 401 단언 추가 |
| 토큰 유출 대비 정책(회전·범위) | 설계 (D): 서비스별 개별 토큰·env 주입·정기/유출 회전·grace window(Inc3b)·mTLS 병행 |

## ⚠ 머지 상태
**Lead 합의 기록이 PR에 첨부되기 전까지 머지 보류**(보안 모델 변경). workflow에 합의 체크리스트.

## 리뷰 후속 보강(3차 — audit 무결성)
| 권장 | 조치 |
|------|------|
| SYSTEM 판정 기준 명확화 | 단일 기준 고정 — "토큰 유효 + X-Member-Id 부재 = SYSTEM_BATCH"(role 미사용). 명시적 `systemCall` + 주석 |
| audit 로깅 단언 테스트 | logback `ListAppender`로 감사 로거 캡처 — SYSTEM=INFO 1건(SYSTEM_BATCH, token 미포함), USER=미기록 단언 |

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (21건)**: 필터 12(+부분 헤더 2 +감사 무결성 1) + 토큰슬라이스 4 + inbound슬라이스 2 + persistence 1 + 캐시 1 + contextLoads 1

## 미해결
- Lead/리뷰 합의(보안 모델 변경) — 머지 보류.
- Inc3b~3d(external/bible HTTP 클라이언트 + 어댑터 + qt/note/study 전환, 기본 inprocess=무변경) → Inc4(DB 분리) → Inc5(모놀리식 제거).
