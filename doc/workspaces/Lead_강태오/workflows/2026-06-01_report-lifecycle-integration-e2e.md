# 워크플로우 — 신고 접수→처리 라이프사이클 통합 E2E

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 관련: 신고 접수(#140)·관리자 처리(#165) / 기준 `CLAUDE.md` §10. 3단계 "통합·E2E"
- PR: #183 (머지됨)

## 1. 배경

report 도메인 테스트는 슬라이스·단위(mock)뿐이라, 사용자 도메인(ReportService)과 관리자 도메인(AdminReportService)이 같은 저장소로 실제 연결되는 2주체 흐름을 관통하지 않았다.

## 2. 작업 범위

- "회원 신고 접수 → 관리자 목록 조회 → 처리 완료" 라이프사이클을 풀 컨텍스트로 검증.
- 프로덕션 코드 변경 없음(테스트만).

## 3. 절차

1. `test/report-lifecycle-integration` 브랜치(dev 기준).
2. `ReportLifecycleIntegrationTest`(`@SpringBootTest @Transactional`).
3. 대상 검증(#147)은 POST만 sharing을 거치므로, 시드 불필요한 `AI_ASSET`/`COMMENT` 대상으로 접수해 흐름 자체에 집중.
4. 케이스 ① createReport(RECEIVED) → 관리자 목록 노출 → resolve(RESOLVED) → H2 영속 상태(처리자/시각) 검증 ② 이미 처리된 신고 reject 재시도 → `REPORT_ALREADY_PROCESSED` 차단.
5. 단독 통과 + 전체 회귀 통과 후 PR.

## 4. 정책 준수

- 도메인 간 호출은 api/UseCase로만(실제 빈). Long FK만 보관.
- write 경로 `@Transactional`.

## 5. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat test --tests "com.qtai.domain.report.internal.ReportLifecycleIntegrationTest" --no-daemon
.\gradlew.bat test --no-daemon
```
