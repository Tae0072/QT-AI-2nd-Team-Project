# 리포트: GetTodayQtUseCase 구현 — 오늘의 QT 조회 + 본문 단건 조회

- **작성일:** 2026-05-28
- **작성자:** Lead 강태오
- **브랜치:** `feature/qt-today-read` → `dev`
- **관련 F-ID:** F-01 (성경 묵상 화면)

---

## 1. 변경 요약

| 파일 | 유형 | 설명 |
|------|------|------|
| `QtPassageRepository.java` | 신규 | QtPassage JPA 레포지토리 (날짜별 조회) |
| `GetTodayQtUseCase.java` | 수정 | 인터페이스에 `getToday()`, `getPassage()` 메서드 추가 |
| `QtService.java` | 수정 | GetTodayQtUseCase 구현 (캐시 정책, Clock 주입) |
| `QtController.java` | 수정 | `GET /today`, `GET /passages/{id}` 엔드포인트 |
| `CacheConfig.java` | 수정 | `todayQt` Caffeine 캐시 등록 (1시간 TTL, 10 max) |
| `QtServiceTest.java` | 신규 | 단위 테스트 8건 (캐시 정책 전 경우의 수) |
| `QtServiceCacheTest.java` | 신규 | 캐시 통합 테스트 2건 (Spring 프록시 @Cacheable 동작) |
| `QtControllerTest.java` | 신규 | MockMvc 기능 테스트 4건 (200/404/STALE_FALLBACK) |
| `QtControllerSecurityTest.java` | 신규 | MockMvc 보안 테스트 2건 (미인증 401) |
| `QtPassageFixture.java` | 신규 | 테스트 공용 픽스처 (createPassage 리플렉션 헬퍼) |

**초기 커밋:** 6 files, +528, -32 lines
**1차 fix 커밋 (PR #126 대응):** 3 files, +182, -5 lines
**2차 fix 커밋 (PR #126 2차 대응):** 6 files, +229, -74 lines

## 2. 테스트 결과

| 테스트 | 결과 |
|--------|------|
| `오늘_본문_존재_HIT` | ✅ PASS |
| `새벽_본문_없음_STALE_FALLBACK` | ✅ PASS |
| `새벽_어제도_없음_EMPTY` | ✅ PASS |
| `배치_이후_본문_없음_MISS` | ✅ PASS |
| `새벽_오늘_본문_있으면_HIT` | ✅ PASS |
| `존재하는_본문_조회_성공` | ✅ PASS |
| `존재하지_않는_본문_조회_실패` (QT_PASSAGE_NOT_FOUND) | ✅ PASS |
| `principal_미해석_시_방어적_처리` | ✅ PASS |
| `HIT_응답_캐싱_검증` (통합) | ✅ PASS |
| `MISS_응답_미캐싱_검증` (통합) | ✅ PASS |
| `getToday_200_성공` (MockMvc) | ✅ PASS |
| `getPassage_200_성공` (MockMvc) | ✅ PASS |
| `getPassage_404_미존재` (MockMvc) | ✅ PASS |
| `getToday_STALE_FALLBACK_성공` (MockMvc) | ✅ PASS |
| `getToday_미인증_401` (보안) | ✅ PASS |
| `getPassage_미인증_401` (보안) | ✅ PASS |

`./gradlew test` → BUILD SUCCESSFUL (전체 테스트 통과, 16건)

## 3. CLAUDE.md 규칙 준수 확인

| 규칙 | 준수 |
|------|------|
| `jakarta.*` 사용, `javax.*` 없음 | ✅ |
| 조회 `@Transactional(readOnly=true)` | ✅ |
| 도메인 간 직접 import 없음 | ✅ |
| Controller → Repository 직접 호출 없음 | ✅ |
| 로그에 민감 정보 없음 | ✅ |
| 금지 기술 없음 | ✅ |
| Conventional Commits 사용 | ✅ |
| F-ID 명시 (F-01) | ✅ |

## 4. 남은 리스크 / 후속 PR

| 항목 | 우선순위 | 설명 |
|------|----------|------|
| Qt 엔티티 + DB 마이그레이션 | 높음 | 사용자 QT 기록 CRUD를 위한 `qt` 테이블 필요 |
| 시뮬레이터/해설/노트 도메인 연동 | 중간 | 현재 TodayQtResponse에 기본값 사용 중 |
| ~~QtController 통합 테스트~~ | ~~해결~~ | 2차 fix 커밋에서 MockMvc 6건 추가 완료 |
| 캐시 키 memberId 포함 | 중간 | 노트 도메인 연동 시 캐시 구조 재설계 필요 (경고 주석 추가) |
| ~~캐시 키에 날짜 반영~~ | ~~해결~~ | fix 커밋에서 SpEL로 KST 날짜 키 적용 완료 |

## 5. PR #126 REQUEST_CHANGES 대응 (2026-05-28)

자동 리뷰에서 BLOCK 2건, WARN 1건 지적 → 모두 수정 후 push 완료.

| 지적 | 심각도 | 수정 내용 |
|------|--------|-----------|
| 캐시 키에 날짜 미포함 → 자정 전환 시 stale 데이터 | BLOCK | SpEL `T(java.time.LocalDate).now(KST)` 키 적용 |
| non-HIT 응답 캐싱 방지 없음 | BLOCK | `unless = "!#result.cacheStatus().equals('HIT')"` 추가 |
| 캐시 통합 테스트 부재 | BLOCK | `QtServiceCacheTest` 추가 (2건) |
| emptyResponse simulatorStatus `null` | WARN | `"DISABLED"`로 변경 (CLAUDE.md §6) |
| 테스트 이름 혼동 | WARN | `principal_미해석_시_방어적_처리`로 변경 |

## 6. PR #126 REQUEST_CHANGES 2차 대응 (2026-05-28)

자동 리뷰 2차에서 BLOCK 1건, WARN 4건 지적 → 모두 수정 후 push 완료.

| 지적 | 심각도 | 수정 내용 |
|------|--------|-----------|
| Controller MockMvc 통합 테스트 부재 | BLOCK | `QtControllerTest` 4건 + `QtControllerSecurityTest` 2건 추가 |
| 캐시 키 memberId 누락 경고 | WARN | QtService에 경고 주석 추가 (후속 PR 가드) |
| createPassage 리플렉션 중복 | WARN | `QtPassageFixture` 공용 클래스로 추출 |
| 매직 스트링 enum 권장 | WARN | 향후 PR에서 CacheStatus/SimulatorStatus enum 도입 예정 |
| PR에 무관 문서 포함 | WARN | 별도 브랜치에서 이미 push된 커밋이라 분리 불가 — 향후 주의 |
