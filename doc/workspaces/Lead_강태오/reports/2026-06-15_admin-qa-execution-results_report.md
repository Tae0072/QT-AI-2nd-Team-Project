# 리포트 — 관리자웹 AD-04/06/07/10 QA 실제 실행 결과

실행 2026-06-15 · dev 로컬(admin-server 8090, qtai-mysql) · 근거: 관리자웹 조치 TODO(2026-06-15) P2 데이터·QA

## 결과 요약 — 4건 전부 통과 ✅

| 항목 | 결과 | 증거 |
| --- | --- | --- |
| AD-04 신고 처리/반려 | ✅ | resolve→RESOLVED, reject→REJECTED. 감사로그 `REPORT_RESOLVE`/`REPORT_REJECT`(target REPORT, actor ADMIN:1). 신고자 알림 `REPORT_RESULT`(notifyReporter=true 건만) 생성 |
| AD-06 공지 발행→알림 | ✅ | 등록(DRAFT)→발행(PUBLISHED). `notificationResult{targetMemberCount:2, createdCount:2, failedCount:0}` — 활성 회원 수만큼 NOTICE 알림 fanout |
| AD-07 감사 조회 | ✅ | 해설 생성 트리거 후 `GET /admin/audit-logs?actionType=AI_EXPLANATION_GENERATE_REQUEST` 1건 — actor/target(QT_PASSAGE:1)/시각/afterJson 추적 노출 |
| AD-10 배치 로그 | ✅ | `GET /admin/ai/batch-run-logs?status=FAILED` 정상. 실패행 errorType·finishedAt(실패시각)·failedCount 노출 → 재시도 판단 가능("보류"→가능 확정) |

방식: 시드 SQL로 샘플 생성 → 관리자 API 호출 → DB·응답으로 연동 확인. 종료 후 샘플 정리(reports/알림/배치 0건, 테스트 공지 HIDDEN).

## 실행 중 발견 / 조치

1. [버그·조치완료] **QA 시드 batch_name이 enum이어야 함.** `ai_batch_run_logs.batch_name`은 컬럼은 VARCHAR지만 엔티티가 `@Enumerated AiBatchName`으로 매핑한다. 시드가 임의 문자열을 넣어 목록 조회 시 enum 매핑 실패로 500이 났다. → 유효값 `AI_DAILY_QT_VERSE_EXPLANATION_SEED`로 수정, QA 식별은 `error_message '[QA-SAMPLE]…'` 마커로 변경(본 PR). 수정 후 조회 정상.

2. [설계 관찰] **AD-07 감사 뷰는 AI 액션 스코프.** `AuditQueryService`가 `AI_ACTION_TYPES` allowlist만 허용(#650). 신고 처리 액션(REPORT_RESOLVE/REJECT)은 `audit_logs`에 기록은 되나(AD-04에서 DB 확인) 이 관리자 뷰엔 미노출. 비-AI 관리자 액션까지 화면 추적하려면 allowlist 확장 또는 별도 운영감사 뷰 필요 — Lead 판단/후속.

## 결론

P2 데이터·QA 4건(AD-04/06/07/10) 실제 환경 실행해 전부 통과. 코드·도구는 dev 반영(#671·#672), 본 PR로 시드 enum 버그 수정 + 결과 기록. AD-07 비-AI 액션 노출 범위는 별도 결정 대상.