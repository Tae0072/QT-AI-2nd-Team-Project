# 워크플로우 — 시뮬레이터 상태→버튼 규칙 풀 컨텍스트 통합 E2E

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 관련: F-12 시뮬레이터 보기 / 기준 `CLAUDE.md` §6, §10. 3단계 "통합·E2E"

## 1. 배경

CLAUDE.md §6: 시뮬레이터 버튼은 상태가 `READY`일 때만 활성화한다. "버튼 활성화"는 클라이언트(Flutter) 규칙이지만, 그 서버측 근거는 **"READY 응답만 재생용 클립 데이터(clipId, sceneScriptJson)를 노출하고, MISSING/FAILED는 데이터를 비운다"**는 계약이다. 이 계약이 풀 컨텍스트(study→qt 크로스도메인 + 실제 JSON 파싱)에서 지켜지는지 검증한다.

기존 `QtSimulatorServiceTest`는 mock 단위 테스트라 실제 빈 와이어링·DB·ObjectMapper를 관통하지 않는다.

## 2. 작업 범위

- `GetQtSimulatorUseCase.getSimulator`를 풀 컨텍스트로 호출해 READY/MISSING/FAILED 3경로 + 데이터 노출 계약 검증.
- 프로덕션 코드 변경 없음(테스트만).

## 3. 상태 결정 규칙(현행 구현)

- 본문(qt_passage) 존재(published) 확인 → 없으면 `QT_PASSAGE_NOT_FOUND`.
- APPROVED 클립 존재 + JSON 파싱 성공 → `READY`(clipId·sceneScript 노출).
- APPROVED 클립 없음 → `MISSING`(데이터 null).
- APPROVED 클립 있으나 JSON 깨짐 → `FAILED`(데이터 null).

## 4. 절차

1. `test/simulator-status-integration` 브랜치(dev 기준).
2. `SimulatorStatusFlowIntegrationTest`(`@SpringBootTest @ActiveProfiles("test")`).
3. 시드는 `@Sql`로 처리(엔티티 크로스패키지 생성 회피): `qt_passages` + `simulator_component_library_versions` + `simulator_clips`(status='APPROVED', `@Enumerated(STRING)`).
4. 케이스: ① READY(승인 클립+유효 JSON) → clipId/sceneScript 노출 ② MISSING(클립 없음) → 데이터 null ③ FAILED(승인 클립+깨진 JSON) → 데이터 null.
5. 단독 통과 + 전체 회귀 통과 후 PR.

## 5. 정책 준수

- study→qt 호출은 `qt.api/GetQtPassageContentContextUseCase`로만(경계 준수).
- read 경로 `@Transactional(readOnly = true)`.

## 6. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat test --tests "com.qtai.domain.study.internal.SimulatorStatusFlowIntegrationTest" --no-daemon
.\gradlew.bat test --no-daemon
```
