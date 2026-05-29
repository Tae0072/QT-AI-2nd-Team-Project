# 워크플로우 — mission 도메인 읽기 모델 구현

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준 문서: `02_ERD_문서.md` v2.2 §2.23/§2.24, `04_API_명세서.md` v1.7 §4.6.1(대시보드), `CLAUDE.md` §3~§5

## 1. 배경 / 설계 판단

mission 도메인도 report와 마찬가지로 구현이 비어 있는 스텁이었다. 그러나 스텁 설계는 명세와 **근본적으로 달랐다**:

- 스텁: `POST /api/v1/missions/start`, `/{id}/complete`, `/my` 등 사용자 API + 단일 `mission` 테이블
- 명세 실제: `/api/v1/missions/**` 엔드포인트가 **존재하지 않음**. 미션은 `GET /api/v1/me/dashboard` 응답의 `missionProgress`로만 노출되고, 데이터는 `mission_definitions`(정의) + `member_mission_progress`(회원 진행률) **2테이블**, 진행률은 노트 활동 집계 **배치**가 갱신(ERD §2.24 계산 기준).

→ 따라서 mission은 **컨트롤러 없는 읽기 모델 도메인**으로 구현한다: 2개 엔티티 + 리포지토리 + 대시보드가 호출할 조회 UseCase + 서비스. 스텁의 start/complete/list API·단일 엔티티·MissionController는 명세와 어긋나므로 제거한다.

## 2. 작업 절차

1. `dev` 최신화 후 `feature/mission-progress-readmodel` 브랜치
2. enum: `MissionMetricType`(MEDITATION_SAVED_DAYS/NOTE_SAVED_COUNT/STREAK_DAYS), `MissionPeriodType`(DAILY/WEEKLY/MONTHLY), `MissionDefinitionStatus`(ACTIVE/HIDDEN)
3. `MissionDefinition` 엔티티(mission_definitions, code UK), `MemberMissionProgress` 엔티티(member_mission_progress, (member,definition,period) UNIQUE)
4. `MissionDefinitionRepository`, `MemberMissionProgressRepository`
5. `GetMemberMissionProgressUseCase`(api) + `MissionProgressResponse`(api/dto): 대시보드 노출용 도메인 간 계약
6. `MissionService`: 회원 진행률 조회 → 정의 매핑(읽기 전용)
7. 비명세 스텁 제거: Start/Complete/ListMissionUseCase, MissionStartRequest, MissionResponse, Mission, (구)MissionRepository, MissionController, client/member·client/qt Mock
8. `MissionService` 단위 테스트
9. 전체 회귀 통과 후 PR

## 3. 도메인 경계 / 정책

- mission은 web 계층이 없는 도메인(대시보드=me 도메인이 소비). `internal` 외부 노출 없음, Long FK만 보관.
- 진행률 계산(배치)·대시보드 wiring은 **후속 과제** — 본 작업은 데이터 모델 + 조회 경로까지.
- ArchUnit `DomainBoundaryArchTest`의 web 규칙은 `noClasses().that().resideInAPackage(<도메인>.web)` 형태라, web 클래스가 0건인 mission에서 "검사 대상 없음"으로 실패한다. web 없는 도메인을 정상 케이스로 보고 해당 규칙에 `allowEmptyShould(true)` 추가(다른 도메인 의존성 검사는 그대로 유지).

## 4. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat --stop                                    # (Windows 파일잠금 회피)
Remove-Item build\classes,build\test-results,build\tmp -Recurse -Force
.\gradlew.bat test --no-daemon --console=plain          # 전체 회귀(ArchUnit/DDL 포함)
```
