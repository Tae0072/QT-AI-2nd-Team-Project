# 워크플로우 — AD-07 감사 조회 비-AI 관리자 액션 노출

근거: 관리자웹 TODO(2026-06-15) AD-07 + QA 실행 발견(2026-06-15). F-06.

## 배경
`AuditQueryService`가 actionType/targetType을 AI 화이트리스트(`AI_ACTION_TYPES`, `ALLOWED_TARGET_TYPES`)로 제한해, 신고 처리(REPORT_RESOLVE/REJECT) 등 비-AI 관리자 액션이 `audit_logs`에 기록은 되지만 감사 조회 화면에 노출되지 않았다(#650 AI 모니터링 스코프 한정).

## 변경 (admin-server)
- 기본 actionType 목록을 `ADMIN_AUDIT_ACTION_TYPES`로 확장: AI(자산/해설/재생성/QA/평가/프롬프트/시뮬레이터) + 운영(신고/공지/배경음악/QT 본문).
- actionType allowlist 게이트 제거 — 명시 필터는 목록 밖이어도 허용(없으면 빈 결과).
- targetType allowlist 게이트 제거 — REPORT/NOTICE/MUSIC_TRACK 등 모든 대상 조회 허용.
- FE `AuditLogsPage`는 actionType이 자유 입력이라 변경 불필요.
- 단위 테스트 3건: 기본 목록에 비-AI 액션 포함, 비-AI actionType/targetType 필터 허용.

## 검증
- `./gradlew :admin-server:test --tests "*AuditQueryServiceTest"` → BUILD SUCCESSFUL.
- 라이브: 신고 처리 후 `audit-logs?actionType=REPORT_RESOLVE` 노출 확인(QA 실행 리포트 참조).

## 참고
- 시스템(SYSTEM_BATCH) 액션은 별도 `SystemAuditLogController` 영역. 본 변경은 관리자 audit 뷰(AdminAuditLogController)만 대상.