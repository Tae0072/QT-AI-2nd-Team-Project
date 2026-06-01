# 워크플로우 — 대시보드 missionProgress 연결

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준 문서: `04_API_명세서.md` §4.6.1(GET /me/dashboard), `02_ERD_문서.md` §2.24, `CLAUDE.md` §3~§5
- 선행: PR #141/#142 (mission 읽기 모델)

## 1. 배경

mission 도메인의 진행률 조회 UseCase(`GetMemberMissionProgressUseCase`)는 #141에서 구현됐으나, 이를 소비할 `GET /api/v1/me/dashboard`의 `missionProgress` 위젯과는 아직 연결되지 않았다(DashboardResponse에 필드 자체가 없었음). 본 작업으로 연결한다.

## 2. 작업 절차

1. `dev` 최신화 후 `feature/dashboard-mission-progress` 브랜치
2. `DashboardResponse`에 `List<MissionProgressResponse> missionProgress` 필드 추가(방어적 복사). mission의 공개 api DTO를 그대로 노출(도메인 간 계약).
3. `MyPageController`에 `GetMemberMissionProgressUseCase` 주입 + `loadMissionProgress` 위젯 로더 추가(부분 실패 정책: 실패 시 빈 리스트 + widgetErrors에 "missionProgress").
4. 컨트롤러 슬라이스 테스트에 mock + 검증 추가
5. 전체 회귀 통과 후 PR

## 3. 도메인 경계

- member(me) 도메인이 mission의 `api/UseCase`를 직접 주입해 호출(CLAUDE.md §4: 다른 도메인은 api/UseCase로 호출). mission 실제 구현체가 dev에 있으므로 client Mock은 불필요.
- member.api(DashboardResponse)가 mission.api.dto를 참조 — api↔api 의존은 ArchUnit 경계 규칙에 위배되지 않음(internal 접근만 제한).
- 위젯별 부분 실패 정책 유지: 미션 위젯 실패가 대시보드 전체 실패로 이어지지 않는다.

## 4. 검증

```powershell
cd qtai-server
.\gradlew.bat --stop ; Remove-Item build\classes,build\test-results,build\tmp -Recurse -Force
.\gradlew.bat test --no-daemon --console=plain
```
