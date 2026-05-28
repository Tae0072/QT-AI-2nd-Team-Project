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
| 10 | 커밋 + push + PR 생성 | 완료 |
| 11 | 워크플로우 + 리포트 작성 | 완료 |

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

## 산출물
- `feature/qt-today-read` 브랜치 → `dev` PR
- 변경 파일 6개, +528/-32 lines
