# 리포트 — 시뮬레이터 상태→버튼 규칙 풀 컨텍스트 통합 E2E

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `test/simulator-status-integration` → PR 대상 `dev`
- 관련: F-12 시뮬레이터 보기 / 기준 `CLAUDE.md` §6, §10. 3단계 "통합·E2E"

## 1. 한 줄 요약

시뮬레이터 상태(READY/MISSING/FAILED) 결정과 "READY만 재생 데이터를 노출한다"는 계약을, study→qt 크로스도메인 + 실제 JSON 파싱을 거치는 풀 컨텍스트로 검증했다. 전체 회귀 572건 통과.

## 2. 배경

- CLAUDE.md §6: 시뮬레이터 버튼은 상태가 `READY`일 때만 활성화. "버튼 활성화"는 클라이언트 규칙이지만, 그 **서버측 근거 = "READY 응답만 clipId·sceneScriptJson을 노출하고, MISSING/FAILED는 비운다"**는 계약이다.
- 기존 `QtSimulatorServiceTest`는 mock 단위라 실제 빈·DB·ObjectMapper를 관통하지 않았다.

## 3. 구현 (테스트만, 프로덕션 코드 변경 없음)

`SimulatorStatusFlowIntegrationTest` (`@SpringBootTest @ActiveProfiles("test") @Transactional`):

- 시드는 `@Sql`로 `qt_passages` + `simulator_component_library_versions` + `simulator_clips`(status='APPROVED', `@Enumerated(STRING)`)를 삽입(엔티티 크로스패키지 생성 회피). 케이스마다 다른 키로 격리.
- 케이스:
  1. **READY** — 승인 클립 + 유효 JSON → `READY`, clipId·sceneScriptJson·componentLibraryVersion 노출.
  2. **MISSING** — 본문만 있고 승인 클립 없음 → `MISSING`, 데이터 null(버튼 비활성화 근거).
  3. **FAILED** — 승인 클립이나 JSON 깨짐 → `FAILED`, 데이터 null(깨진 산출물 미노출).

## 4. 변경 파일

| 구분 | 파일 |
|------|------|
| 신규 | `study/internal/SimulatorStatusFlowIntegrationTest.java` |
| 신규 | 워크플로우 `2026-06-01_simulator-status-integration-e2e.md` + 본 리포트 |

## 5. 검증

- 단독: 3건 통과(풀 컨텍스트 13.9s).
- 전체 회귀: `./gradlew test` → **572건 통과(실패 0)**.

## 6. 후속 (3단계 잔여)

- 기본 성능/부하 점검.
- (참고) `DISABLED` 상태는 시뮬레이터 서비스가 아니라 QT 레벨(본문 없음 등)에서 나오므로 Today QT E2E(#182)와 함께 본다.
