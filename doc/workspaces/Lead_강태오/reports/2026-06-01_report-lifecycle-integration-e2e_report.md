# 리포트 — 신고 라이프사이클 풀 컨텍스트 통합 E2E

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `test/report-lifecycle-integration` → PR 대상 `dev`
- 관련: 3단계 "통합·E2E·성능" 2번 작업. 신고 접수(#140)·관리자 처리(#165) 흐름.

## 1. 한 줄 요약

"회원 신고 접수 → 관리자 목록 조회 → 처리 완료"로 이어지는 신고 라이프사이클을, 사용자 도메인(ReportService)과 관리자 도메인(AdminReportService)이 같은 저장소를 통해 실제로 연결되는지 **풀 컨텍스트로 관통 검증**했다. 전체 회귀 545건 통과.

## 2. 배경

- 기존 report 테스트는 `@WebMvcTest`(usecase mock)·단위(mock)뿐이라, 접수→처리가 실제 빈·DB로 연결되는 흐름을 관통하지 않았다.
- 신고는 사용자가 접수하고 관리자가 처리하는 **2주체 흐름**이라, 상태 전이·처리자 기록이 실제로 영속되는지는 통합으로만 보장된다.

## 3. 구현

`ReportLifecycleIntegrationTest` (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional`):

- 대상 검증(#147)은 POST(나눔글)만 sharing 도메인을 거치므로, **시드가 필요 없는 AI_ASSET/COMMENT** 대상으로 접수해 흐름 자체에 집중.
- 케이스 ① 라이프사이클: createReport(RECEIVED) → 관리자 목록 노출 확인 → resolve(RESOLVED) → H2 영속 상태(status/처리자/처리시각) 검증.
- 케이스 ② 멱등/안전: 이미 RESOLVED된 신고를 reject 재시도 → `REPORT_ALREADY_PROCESSED`로 차단.

## 4. 변경 파일

| 구분 | 파일 |
|------|------|
| 신규 | `qtai-server/src/test/java/com/qtai/domain/report/internal/ReportLifecycleIntegrationTest.java` |
| 신규 | 본 리포트 |

> 프로덕션 코드 변경 없음 — 테스트만 추가.

## 5. 검증

- 단독: 2건 통과(풀 컨텍스트 9.2s).
- 전체 회귀: `./gradlew test` → **545건 통과(실패 0)**.

## 6. 후속 (3단계 잔여)

- 시뮬레이터 상태 → 버튼 활성화 규칙 E2E.
- HIDE_TARGET/알림/감사 cross-domain 연동 구현 후 그 흐름까지 E2E 확장.
- 기본 성능/부하 점검.
