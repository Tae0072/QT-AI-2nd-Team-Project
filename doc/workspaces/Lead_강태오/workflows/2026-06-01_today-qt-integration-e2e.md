# 워크플로우 — Today QT 풀 컨텍스트 통합 E2E

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 관련: F-01 성경 묵상 화면 / 기준 `CLAUDE.md` §6, §10. 3단계 "통합·E2E"
- PR: #182 (머지됨)

## 1. 배경

qt 도메인 테스트는 슬라이스(@WebMvcTest)·단위(mock)뿐이라 실제 빈 와이어링을 관통하지 않았다. "Today QT 100%"는 여러 도메인 조합이라 풀 컨텍스트로만 동작이 검증된다.

## 2. 작업 범위

- `GetTodayQtUseCase`가 qt 본문 + note 도메인(실제 `GetNoteUseCase`)을 조합하는 흐름을 H2 풀 컨텍스트로 검증.
- 프로덕션 코드 변경 없음(테스트만).

## 3. 절차

1. `test/today-qt-integration` 브랜치(dev 기준).
2. `TodayQtFlowIntegrationTest`(`@SpringBootTest @Transactional`): `@TestConfiguration`으로 `Clock`을 2026-05-27 06:00 KST(배치 04:00 이후)로 고정.
3. test 프로파일은 Flyway off → 테스트가 직접 `QtPassage` 시드(기존 `QtPassageFixture` 재사용). `todayQt` 캐시 격리.
4. 케이스 ① 본문 있음(HIT): 조립 반환 + 시뮬레이터 상태 enum + draftNoteId(note 실호출) ② 본문 없음: DISABLED+MISS 안전 응답.
5. 단독 통과 + 전체 회귀 통과 후 PR.

## 4. 정책 준수

- 시뮬레이터 상태는 허용 enum(READY/MISSING/FAILED/DISABLED)만 — §6.
- note 도메인은 api/UseCase로만 호출(실제 빈) — 경계 준수.

## 5. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat --stop
.\gradlew.bat test --tests "com.qtai.domain.qt.internal.TodayQtFlowIntegrationTest" --no-daemon
.\gradlew.bat test --no-daemon
```
