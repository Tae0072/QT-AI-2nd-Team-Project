# 2026-05-28 GetTodayQtUseCase 구현 — 오늘의 QT 조회 + 본문 단건 조회

## 목표
W2 "핵심 API 데모" 목표의 핵심인 오늘의 QT 조회 기능을 구현한다.
GetTodayQtUseCase(오늘의 QT + 본문 단건 조회)를 1차 PR로 분리하여 진행한다.

## 작업 순서

| # | 작업 | 상태 |
|---|------|------|
| 1 | Qt 도메인 현재 코드 및 요구사항 파악 | 완료 |
| 2 | QtPassageRepository 생성 | 완료 |
| 3 | GetTodayQtUseCase 인터페이스 완성 | 완료 |
| 4 | QtService — GetTodayQtUseCase 구현 (캐시 정책 포함) | 완료 |
| 5 | QtController — GET /today, GET /passages/{id} 엔드포인트 | 완료 |
| 6 | CacheConfig — todayQt 캐시 등록 (1시간 TTL) | 완료 |
| 7 | QtServiceTest — 단위 테스트 7건 | 완료 |
| 8 | 코드 2차 검토 (CLAUDE.md 규칙 준수 확인) | 완료 |
| 9 | 빌드 + 테스트 검증 (BUILD SUCCESSFUL) | 완료 |
| 10 | 커밋 + push + PR #126 생성 | 완료 |
| 11 | 워크플로우 + 리포트 작성 | 완료 |
| 12 | PR #126 REQUEST_CHANGES 대응 — BLOCK 수정 | 완료 |
| 13 | QtServiceTest assertions 수정 + 캐시 통합 테스트 추가 | 완료 |
| 14 | fix 커밋 push + 재리뷰 대기 | 완료 |
| 15 | 2차 대응: QtControllerTest/SecurityTest MockMvc 테스트 추가 | 완료 |
| 16 | QtPassageFixture 공용 추출 + 캐시 키 memberId 경고 주석 | 완료 |
| 17 | 2차 fix 커밋 push + 재리뷰 대기 | 완료 |

## 핵심 구현 결정

### 캐시 정책 (CLAUDE.md §6)
- 오늘 본문 있음 → `HIT`
- 00:00~04:00 오늘 없음 → 어제 본문 `STALE_FALLBACK`
- 04:00 이후 오늘 없음 → `MISS` (배치 미완료 경고)
- 데이터 없음 → `EMPTY`

### 1차 PR 범위 결정
- Qt 엔티티(사용자 QT 기록)는 DB 테이블 미존재 → 후속 PR
- QtPassage(일일 QT 본문)는 이미 구현 + 마이그레이션 있음 → 1차 PR
- 해설/시뮬레이터/노트 진입점은 각 도메인 연동 전 기본값 사용

### Clock 주입
- MemberService와 동일한 패턴으로 `Clock` 빈 주입
- 테스트에서 시간 제어 가능 (00:00~04:00 경계 테스트)

## PR #126 REQUEST_CHANGES 대응 (2026-05-28)

Claude 자동 리뷰에서 BLOCK 2건, WARN 1건 지적:

### BLOCK 수정
1. **캐시 키에 KST 날짜 미포함** → `T(java.time.LocalDate).now(...)` SpEL로 날짜별 캐시 엔트리 분리
2. **non-HIT 응답 캐싱 방지 누락** → `unless = "!#result.cacheStatus().equals('HIT')"` 추가
3. **캐시 통합 테스트 부재** → `QtServiceCacheTest` 추가 (Spring 프록시 기반 @Cacheable 동작 검증)

### WARN 수정
1. **emptyResponse simulatorStatus** → `null` → `"DISABLED"` (CLAUDE.md §6 시뮬레이터 상태 규격 준수)
2. **테스트 이름 정정** → `memberId_null_허용` → `principal_미해석_시_방어적_처리`

### 추가 테스트
- `QtServiceCacheTest.HIT_응답_캐싱_검증` — HIT 응답 캐싱 후 repository 미호출 확인
- `QtServiceCacheTest.MISS_응답_미캐싱_검증` — MISS 응답 매번 repository 재호출 확인

## 산출물
- `feature/qt-today-read` 브랜치 → `dev` PR #126
- 초기 커밋: 변경 파일 6개, +528/-32 lines
- 1차 fix 커밋: 변경 파일 3개, +182/-5 lines (캐시 키/unless/통합 테스트)
- 2차 fix 커밋: 변경 파일 6개, +229/-74 lines (MockMvc 테스트/픽스처 공용화)
