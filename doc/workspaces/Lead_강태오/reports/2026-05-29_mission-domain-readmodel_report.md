# 리포트 — mission 도메인 읽기 모델 구현

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/mission-progress-readmodel` → PR 대상 `dev`

## 1. 한 줄 요약

명세상 사용자 API가 없는 mission 도메인을 ERD 2테이블(`mission_definitions` + `member_mission_progress`) 기준의 **컨트롤러 없는 읽기 모델**로 구현했다. 대시보드가 호출할 진행률 조회 UseCase + 서비스 + 테스트 4건. 잘못 설계된 스텁(start/complete API 등)은 제거. 전체 회귀 통과.

## 2. 변경 요약

신규(읽기 모델):

- `internal/MissionDefinition.java`, `internal/MemberMissionProgress.java` — ERD §2.23/§2.24 매핑
- `internal/MissionMetricType.java`, `MissionPeriodType.java`, `MissionDefinitionStatus.java` — enum
- `internal/MissionDefinitionRepository.java`, `MemberMissionProgressRepository.java`
- `api/GetMemberMissionProgressUseCase.java` + `api/dto/MissionProgressResponse.java` — 대시보드용 계약
- `internal/MissionService.java` — 진행률 조회 + 정의 매핑(읽기 전용)
- `test/.../mission/internal/MissionServiceTest.java` — 단위 테스트 4건

제거(명세 불일치 스텁):

- `api/StartMissionUseCase`, `api/CompleteMissionUseCase`, `api/ListMissionUseCase`
- `api/dto/MissionStartRequest`, `api/dto/MissionResponse`
- `internal/Mission`(단일 테이블), `internal/MissionRepository`(구)
- `web/MissionController`, `client/member/GetMemberUseCaseMock`, `client/qt/GetQtUseCaseMock`

테스트 인프라:

- `architecture/DomainBoundaryArchTest.java` — web 규칙에 `allowEmptyShould(true)` 추가(web 없는 도메인 정상 처리)

## 3. 테스트 결과

| 테스트 | 케이스 | 결과 |
|--------|--------|------|
| MissionServiceTest | 진행률 없음 → 빈 리스트, 정의 조회 생략 | PASS |
| | 진행률↔정의 매핑(code/title/metric/진행값) | PASS |
| | 완료 미션 completed=true + completedAt | PASS |
| | 정의 누락 시 code/title null, 진행값 유지 | PASS |

- `./gradlew test --no-daemon` (전체) → **BUILD SUCCESSFUL (42s)** — ArchUnit 경계·엔티티 DDL 포함 543+ 테스트 통과.

## 4. 남은 리스크 / 후속 과제

- **진행률 계산 배치 미구현**: `current_count`/`progress_rate`/`completed_at` 갱신 로직(노트 활동 집계, ERD §2.24 `LEAST(current/target*100,100)`)은 별도 배치 작업. note 도메인 client 어댑터 필요.
- **대시보드 연결 미구현**: me 도메인의 `GET /me/dashboard`가 `GetMemberMissionProgressUseCase`를 호출해 `missionProgress`를 채우는 wiring은 member/대시보드 담당 작업과 조율 필요.
- **mission_definitions 시드/관리 API**: 운영자 미션 등록 경로는 admin 도메인 또는 시드 데이터로 후속 검토.

## 5. 설계 메모

mission은 사용자 web 엔드포인트가 없는 "읽기 모델 + 배치 갱신" 도메인이다. 스텁의 start/complete 흐름은 "미션은 사용자가 수동 시작/완료" 가정이었으나, 명세·ERD는 "노트 활동을 자동 집계"하는 방식이라 해당 스텁을 따르지 않았다.
