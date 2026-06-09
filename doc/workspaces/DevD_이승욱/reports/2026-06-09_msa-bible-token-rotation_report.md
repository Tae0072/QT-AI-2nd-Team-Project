# 2026-06-09 bible-service 토큰 회전 grace window(Inc3b) — 결과 보고

## 요약
공유 토큰(서비스 신뢰 자격)을 무중단 회전할 수 있게 필터에 grace window(현재값+직전값 동시 허용)를 추가했다. 운영 진입 전 충족해야 할 토대 항목을 완료. 추가로 Inc3a 후속 점검(부분 신원 헤더 다운스트림 영향)을 운영 진입 체크리스트에 반영. 필터+설정+테스트+체크리스트.

## 산출물

| 파일 | 설명 |
|------|------|
| `bible-service/.../GatewayHeaderAuthenticationFilter.java` | 직전 토큰 허용(grace window) — 3-arg 생성자 + `constantTimeEquals`(현재/직전 각각 상수시간) |
| `bible-service/.../BibleServiceInboundConfiguration.java` | `qtai.bible.gateway.previous-token` 주입 |
| `bible-service/src/main/resources/application.yml` | `previous-token: ${QTAI_BIBLE_GATEWAY_PREVIOUS_TOKEN:}` |
| `bible-service/.../GatewayHeaderAuthenticationFilterTest.java` | (+4) 직전 통과 / 현재 통과 / 미지 401 / 직전 미설정 거부 |
| `doc/.../bible-service-운영진입-체크리스트.md` | (신규) cutover 체크리스트 — grace window·부분헤더 점검·배포·롤백 추적 |

## 변경 성격
- **무중단 회전**: 현재값+직전값 동시 허용 → 토큰 교체 시 게이트웨이/소비자 점진 전환 중에도 401 없음. 전환 완료 후 previous 비움.
- **상수시간 비교 유지**: 현재·직전 각각 `MessageDigest.isEqual`, 단락 평가 회피.
- **하위 호환**: 2-arg 생성자 유지(현재값만), 단일 토큰 의미 불변 → 기존 테스트 전부 유지.
- **부분 신원 헤더 점검(Inc3a 후속)**: bible-service 도메인은 신원/role 인가 미사용(`X-Member-*`/`@PreAuthorize`/`SecurityContext` 0건, 컨트롤러는 `@RequestParam`만) → 부분/무 헤더의 침묵 인가 우회 위험 없음. 향후 member 인가 추가 시 SYSTEM 통과 재검토 불변식을 체크리스트에 기록.
- 게이트웨이·소비자·코어 무변경.

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (25건)**: 필터 16(+회전 4) + 토큰슬라이스 4 + inbound슬라이스 2 + persistence 1 + 캐시 1 + contextLoads 1
- 테스트 토큰 리터럴은 상수+`gitleaks:allow`(CI 게이트 대비).

## 미해결
- Inc3c(external/bible HTTP 클라이언트+어댑터) — getVerse(Long)/ListChapters/SearchBible은 대응 엔드포인트 부재라 소비자 사용 범위 조사 선행.
- Inc3d(소비자 전환) → Inc4(DB 분리) → Inc5(모놀리식 제거).
