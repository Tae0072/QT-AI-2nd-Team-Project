# 워크플로우 — AD-07 감사 조회 비-AI 관리자 액션 노출

근거: 관리자웹 TODO(2026-06-15) AD-07 + QA 실행 발견(2026-06-15). F-06.

## 배경
`AuditQueryService`가 actionType/targetType을 AI 화이트리스트(`AI_ACTION_TYPES`, `ALLOWED_TARGET_TYPES`)로 제한해, 신고 처리(REPORT_RESOLVE/REJECT) 등 비-AI 관리자 액션이 `audit_logs`에 기록은 되지만 감사 조회 화면에 노출되지 않았다(#650 AI 모니터링 스코프 한정).

## 변경 (admin-server)
- 기본 actionType allowlist를 `ADMIN_AUDIT_ACTION_TYPES`로 확장: AI(자산/해설/재생성/시뮬레이터) + 운영(신고/공지/배경음악/QT 본문).
- 대상 allowlist(`ALLOWED_TARGET_TYPES`)에 REPORT/NOTICE/MUSIC_TRACK 추가.
- **deny-by-default 유지**: 검증 참조 원문(VALIDATION_REFERENCE_JOB)·평가/프롬프트 등 민감 영역은 allowlist 미포함이라 계속 차단(400). 리뷰(#679) 반영.
- FE `AuditLogsPage`는 actionType이 자유 입력이라 변경 불필요.
- 단위 테스트 5건: 기본 목록에 비-AI 액션 포함, 비-AI actionType/targetType 허용, 민감 actionType/targetType 거부.

## 검증
- `./gradlew :admin-server:test --tests "*AuditQueryServiceTest"` → BUILD SUCCESSFUL.
- 라이브: 신고 처리 후 `audit-logs?actionType=REPORT_RESOLVE` 노출 확인(QA 실행 리포트 참조).

## 참고
- 시스템(SYSTEM_BATCH) 액션은 별도 `SystemAuditLogController` 영역. 본 변경은 관리자 audit 뷰(AdminAuditLogController)만 대상.