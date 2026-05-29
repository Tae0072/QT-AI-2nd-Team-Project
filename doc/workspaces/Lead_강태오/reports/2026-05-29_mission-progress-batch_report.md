# 리포트 — mission 진행률 배치 계산 (A)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/mission-progress-batch` → PR 대상 `dev`
- 선행: #145(누락 마이그레이션 hotfix, V18 mission 테이블) 머지 완료

## 1. 한 줄 요약

note 월간 묵상 집계를 소비해 회원 MONTHLY 미션 진행률을 계산·upsert하는 서비스와 04:00 KST 배치를 구현했다. 샘플 미션 정의(V19)도 시드. 전체 회귀 통과.

## 2. 변경 사항

| 구분 | 파일 | 내용 |
|------|------|------|
| 신규 | `mission/api/RecalculateMissionProgressUseCase.java` | 배치 진입 포트(recalculate, recalculateAllEnrolled) |
| 신규 | `mission/internal/MissionProgressCalculator.java` | note 월간 집계 소비 → MONTHLY 진행률 계산·upsert |
| 수정 | `mission/internal/MemberMissionProgress.java` | `applyCalculation` mutator(멱등 update, completedAt 최초 시각 보존) |
| 수정 | `mission/internal/MemberMissionProgressRepository.java` | upsert 조회 + distinct 회원 조회 |
| 수정 | `mission/internal/MissionDefinitionRepository.java` | `findByStatus`(ACTIVE) |
| 신규 | `config/SchedulingConfig.java` | `@EnableScheduling` |
| 신규 | `batch/MissionProgressBatch.java` | `@Scheduled(04:00 KST)` → recalculateAllEnrolled |
| 신규 | `db/migration/V19__seed_sample_mission_definitions.sql` | 샘플 ACTIVE MONTHLY 미션 3종 |
| 신규 | `test/.../MissionProgressCalculatorTest.java` | 단위 테스트 5건 |

## 3. 계산 규칙 (ERD §2.24)

- 지표 매핑: `MEDITATION_SAVED_DAYS→savedDays`, `NOTE_SAVED_COUNT→savedNoteCount`, `STREAK_DAYS→meditationStreakDays`(note 월간 집계 Summary).
- 진행률 = `LEAST(current / target * 100, 100)`, DECIMAL(5,2) 반올림.
- 목표 달성 시 `completed_at` 최초 시각 보존(재달성으로 변경되지 않음).
- upsert: (member, definition, period_start) 기준 — 있으면 갱신, 없으면 생성(멱등).

## 4. 도메인 경계 / 정책

- mission이 note의 `api/GetMeditationCalendarUseCase`만 호출(Long FK만 보관). 다른 도메인 internal 직접 접근 없음.
- 배치는 `com.qtai.batch`에서 mission `api/UseCase`를 호출(SYSTEM_BATCH 주체). 04:00 KST(CLAUDE.md §6).
- write 경로 `@Transactional`. 한 회원 실패가 배치 전체를 멈추지 않도록 격리(실패 로그 후 계속).

## 5. 테스트 결과

| 케이스 | 결과 |
|--------|------|
| 신규 진행 생성 + 지표 매핑 + 진행률(50%) | PASS |
| 목표 달성 → completedAt 설정 + 진행률 100% 상한 | PASS |
| 기존 진행 갱신(dirty checking, save 미호출) | PASS |
| MONTHLY 외 주기(DAILY) 건너뜀 | PASS |
| 배치 회원 반복 + 한 명 실패 격리 | PASS |

- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL (44s).

## 6. 제약 / 남은 후속

- **MONTHLY 주기만 계산**: note가 월간 집계만 제공 → DAILY/WEEKLY는 note 기간별 집계 api가 생긴 뒤 후속(note 담당 조율).
- **enroll(최초 진행 레코드 생성)**: 배치는 이미 진행 레코드가 있는 회원만 재계산한다. 신규 회원의 최초 enroll 트리거(노트 저장 이벤트 또는 가입 시)는 후속 과제.
- H2 자동 검증은 기존 V13 H2 비호환으로 불가(#145 리포트 참조) — V19 시드/엔티티는 prod MySQL 기준 + 단위 테스트로 검증.

## 7. 자동 리뷰(#146) 대응

자동 리뷰의 BLOCK 2건을 구조적으로 수정했다.

- **[BLOCK] 트랜잭션 자기호출 격리 실패** → 단일 `MissionProgressCalculator`가 `@Transactional`에서 자기 메서드를 호출(self-invocation)해 회원 전체가 한 트랜잭션에 묶이고, 한 회원 실패 시 rollback-only로 배치 전체가 롤백될 수 있던 문제. → **비트랜잭션 코디네이터(`MissionProgressCoordinator`) + 회원별 `@Transactional` 계산기(`MissionProgressCalculator`) 빈 분리**로 변경. 코디네이터가 회원마다 계산기를 프록시 경유 호출 → 회원별 독립 트랜잭션 → 실패 격리 보장.
- **[BLOCK] mock 격리 테스트의 거짓 신뢰** → 격리 책임이 코디네이터로 이동했고, `MissionProgressCoordinatorTest`에서 계산기가 한 회원에 예외를 던져도 나머지 회원이 계속 처리되는지 검증. (구조상 회원별 TX는 스프링 프록시 모델로 보장)
- [WARN] 정의 N회 조회 → 코디네이터가 ACTIVE 정의를 **1회 로드해 전달**(테스트로 times(1) 검증).
- [WARN] `target<=0` 완료 오판 → `reached = target>0 && current>=target`로 수정(단위 테스트 추가).
- [INFO] Clock zone → `JpaAuditingConfig.clock()`이 `Asia/Seoul`이라 월경계 오계산 없음(확인 완료).
- 실 DB 격리 통합 테스트(@SpringBootTest)는 기존 @SpringBootTest 부재·H2/V13 제약으로 보류 — 컨텍스트 로드/Modulith 검증 가드는 전사 CI 개선(#149 W3 체크리스트)으로 제안.
