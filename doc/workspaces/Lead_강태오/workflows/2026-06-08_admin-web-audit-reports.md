# [Workflow] admin-web 관리자 화면 — AD-07 감사 로그 · AD-04 신고 처리 (2026-06-08)

## 목적

관리자 웹 첫 화면(C단계): 공통 목록 인프라 + AD-07(감사 로그, 읽기 전용) + AD-04(신고 처리) 구현.

## 구현

- 공통(C0): `hooks/usePagedList` — 서버 페이지네이션·로딩·에러·필터/페이지 상태를 한곳에서 관리(여러 목록 화면 재사용). `utils/datetime` — 시각 포맷.
- **AD-07 감사 로그**: `GET /admin/audit-logs`. 표(시각/행위자/액션/대상) + 필터(행위자 유형·액션·기간) + 페이지네이션 + 행 펼치기로 변경 전/후 JSON. `AuditLog` 타입을 백엔드 `AuditLogItem`과 정합.
- **AD-04 신고 처리**: `GET /admin/reports` + `resolve`/`reject`. 표(대상/사유/신고자/상태/처리) + 필터(상태·대상유형) + 처리/반려 모달(사유·신고자 알림). `Report` 타입을 `AdminReportListResponse.Item`과 정합.

## 계약 확인

- 두 응답 모두 표준 `Page<T>`(content/page/size/totalElements/totalPages)와 호환됨을 백엔드 DTO로 확인.
- resolve/reject 본문 = `{reason, notifyReporter}`, 결과 `ProcessReportResult`.

## 검증

- admin-web `typecheck`(tsc --noEmit) + `build`(vite) 통과.
- 런타임(데이터 표시·처리 동작)은 B(dev 로그인, `feature/admin-dev-login`) + 백엔드 기동 후 수동 확인 예정.

## 주의

- 권한은 백엔드에서 검증(AD-07 SUPER_ADMIN, AD-04 OPERATOR/SUPER_ADMIN). 화면별 메뉴 노출/접근 제어(D2)는 후속.
- 팀 MSA 분리 작업 중 — PR은 오늘 작업 종료 시 일괄(브랜치 보존).
