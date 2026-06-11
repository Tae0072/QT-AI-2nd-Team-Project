# 2026-06-09 bible-service inbound 슬라이스 검증(Inc2a) — 결과 보고

## 요약
라우트 컷오버(Inc2b) 전 단계 게이트로, `inbound` 활성 시 bible-service가 인증 필터 뒤에서 정상 서빙함을 슬라이스 테스트로 검증했다. `BibleController`+`BibleService`+필터+persistence 통합 동작 확인. 테스트 1파일 + 문서, main 무변경.

## 산출물

| 파일 | 설명 |
|------|------|
| `bible-service/.../BibleInboundSliceTest.java` | (신규) inbound+persistence 활성 컨텍스트 MockMvc 슬라이스 — 헤더 200 / 무헤더 401 |
| `doc/.../bible-service-Inc2-라우트컷오버-설계_2026-06-09.md` | (신규) Inc2 컷오버 설계(순서·게이트웨이·롤백·2a/2b 분할) |

## 변경 성격
- **서빙 준비 검증(테스트 전용)**: 스캐폴드에 구현된 controller/filter/service를 활성화해 통합 동작 확인. main 코드·구성 무변경.
- **보안 실증**: `GatewayHeaderAuthenticationFilter`가 게이트웨이 미경유(헤더 없는) 요청을 401로 차단하고, 신원 헤더가 있으면 컨트롤러까지 전달함을 MockMvc로 단언.
- **도메인 커버리지**: BibleController `/books` 엔드포인트 + BibleService 조회 경로를 통합 테스트로 커버 → 스캐폴드 단계의 도메인 테스트 공백 추가 해소.

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (12건)**: 슬라이스 2 + 필터 7 + persistence 1 + 캐시 1 + contextLoads 1
- 헤더 있음 200(+data) / 헤더 없음 401·M0002 확인.

## 미해결
- Inc2b(게이트웨이 라우트 전환 + 토큰 주입 + 라우트 순서 테스트 = 실제 컷오버)
- Inc3(소비자 어댑터) → Inc4(DB 분리) → Inc5(모놀리식 제거)
- 배포 활성 설정 체크리스트(Inc2b 전제)
