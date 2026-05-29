# 리포트 — 대시보드 missionProgress 연결

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/dashboard-mission-progress` → PR 대상 `dev`

## 1. 한 줄 요약

`GET /api/v1/me/dashboard`가 mission 도메인의 `GetMemberMissionProgressUseCase`를 호출해 `missionProgress` 위젯을 채우도록 연결했다. mission 읽기 모델(#141/#142)의 소비처가 생겼다. 전체 회귀 통과.

## 2. 변경 사항

| 구분 | 파일 | 내용 |
|------|------|------|
| 수정 | `member/api/dto/DashboardResponse.java` | `List<MissionProgressResponse> missionProgress` 필드 추가(방어적 복사) |
| 수정 | `member/web/MyPageController.java` | `GetMemberMissionProgressUseCase` 주입 + `loadMissionProgress` 위젯 로더(부분 실패 허용) |
| 수정 | `test/.../member/web/MyPageControllerTest.java` | mock 추가 + missionProgress 응답 검증 |

## 3. API 응답 (명세 §4.6.1 일치)

`GET /api/v1/me/dashboard` 응답에 `missionProgress` 배열 추가:

```json
"missionProgress": [
  {
    "missionDefinitionId": 5, "code": "MED_30", "title": "묵상 30일",
    "metricType": "MEDITATION_SAVED_DAYS", "periodType": "MONTHLY",
    "currentCount": 10, "targetCount": 30, "progressRate": 33.33,
    "completed": false, "periodStartDate": "2026-05-01", "periodEndDate": "2026-05-31",
    "completedAt": null
  }
]
```

미션 위젯 조회 실패 시 빈 배열 + `widgetErrors`에 `"missionProgress"` 기록(부분 실패 정책).

## 4. 테스트 결과

| 테스트 | 케이스 | 결과 |
|--------|--------|------|
| MyPageControllerTest | 정상 응답에 missionProgress 포함(title/progressRate) | PASS |
| | profile 위젯 실패 → widgetErrors 포함 | PASS |
| | **미션 위젯 실패 → missionProgress 빈 배열 + widgetErrors "missionProgress"** | PASS |

- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL.

> 정정: 초기 작성 시 미션 위젯 부정 경로가 별도 테스트로 커버된 것처럼 기재했으나 실제 누락이었음. 자동 리뷰(#143 REQUEST_CHANGES) 지적을 반영해 부정 경로 테스트를 추가하고 표를 정정함.

## 5. 남은 후속

- **진행률 배치 계산 미구현**: `member_mission_progress`의 `current_count`/`progress_rate`/`completed_at`을 노트 활동에서 집계·갱신하는 배치(ERD §2.24)는 별도 작업. 현재는 저장된 진행률을 조회해 노출만 한다.
- mission_definitions 시드/운영자 등록 경로는 후속 검토.
