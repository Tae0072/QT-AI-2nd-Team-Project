# 워크플로우 — mission 진행률 배치 계산 (A)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 기준 문서: `02_ERD_문서.md` §2.24, `04_API_명세서.md` §4.6.1, `CLAUDE.md` §6(배치 04:00 KST)

## 1. 배경

mission 진행률(`member_mission_progress`)은 사용자 요청이 아니라 노트 활동 집계로 갱신된다. #141/#142/#143에서 읽기 모델·대시보드 노출까지 마쳤고, 본 작업은 그 진행률을 실제로 계산·갱신하는 배치를 구현한다(#145로 mission 테이블 마이그레이션 선행 보강 완료).

## 2. 절차

1. #145 머지로 V18 mission 테이블 dev 반영 확인 → `feature/mission-progress-batch` 브랜치
2. note 집계 계약 확인: `GetMeditationCalendarUseCase`가 월간 Summary(savedDays/savedNoteCount/meditationStreakDays) 제공 → 지표 3종과 1:1 매핑
3. `RecalculateMissionProgressUseCase`(api) + `MissionProgressCalculator`(internal) 구현 — MONTHLY 미션 계산·upsert
4. `MemberMissionProgress.applyCalculation` mutator 추가, 리포지토리 upsert/distinct 조회 추가
5. `@EnableScheduling`(SchedulingConfig) + `MissionProgressBatch`(@Scheduled 04:00 KST)
6. `V19` 샘플 미션 정의 시드
7. 단위 테스트 + 전체 회귀 통과 후 PR

## 3. 검증

```powershell
cd qtai-server
.\gradlew.bat --stop ; Remove-Item build\classes,build\resources,build\test-results,build\tmp -Recurse -Force
.\gradlew.bat test --no-daemon --console=plain
```
