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
| `QtServiceTest.java` | 신규 | 단위 테스트 7건 (캐시 정책 전 경우의 수) |

**총:** 6 files, +528, -32 lines

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
| `memberId_null_허용` | ✅ PASS |

`./gradlew build` → BUILD SUCCESSFUL

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
| QtController 통합 테스트 | 중간 | `@WebMvcTest` + `MockMvc` 추가 필요 |
| 캐시 키에 날짜 반영 | 낮음 | 현재 1시간 TTL로 대응, 정밀 제어 시 키 개선 |
