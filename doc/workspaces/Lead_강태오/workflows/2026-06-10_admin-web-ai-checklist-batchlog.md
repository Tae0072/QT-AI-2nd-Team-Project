# 2026-06-10 · admin-web AD-09/AD-10 신규 화면 워크플로우

## 목적

관리자 웹의 AI 운영 영역을 백엔드 계약에 맞춰 마저 채운다. AD-01~08은 이미 구현·머지되어 있으나, 백엔드 `AdminAiValidationChecklistController`(검증 체크리스트)와 `AdminAiBatchRunLogController`(배치 실행 로그)에 대응하는 프런트 화면이 없었다. 이 두 화면(AD-09, AD-10)을 신규 구현하고, AD-08 모니터링·AD-03 검증 화면을 가볍게 보강한다.

## 배경 (현황 점검)

- 브랜치 `feature/admin-web-screens`는 `origin/dev-admin-web`과 동일 커밋. AD-01~08 표/필터/모달/페이지네이션까지 구현 완료 상태였다.
- 미구현은 백엔드 계약상 존재하나 프런트가 없던 두 컨트롤러였다.
  - `GET/POST /api/v1/admin/ai/validation-checklists`, `/{id}/activate`, `/{id}/retire` (권한 REVIEWER)
  - `GET /api/v1/admin/ai/batch-run-logs` (권한 OPERATOR/REVIEWER)

## 이번 작업 범위

- `admin-web/src/api/aiChecklists.ts` 추가: 목록/등록/활성화/폐기 호출.
- `admin-web/src/api/aiBatchRunLogs.ts` 추가: 배치 로그 목록 호출.
- `admin-web/src/pages/AiChecklistsPage.tsx` 추가 (AD-09): 목록·필터(유형/상태)·등록 모달·활성화/폐기.
- `admin-web/src/pages/AiBatchRunLogsPage.tsx` 추가 (AD-10): 목록·필터(배치명/상태/기간)·행 펼침 오류 상세.
- `constants/menu.ts`, `App.tsx`: AD-09/AD-10 메뉴·라우트 등록(권한 기준 노출).
- 보강: AD-08 모니터링 배치/체크리스트 카드 → AD-10/AD-09 바로가기. AD-03 산출물 행 펼침에 메타데이터 상세(원문 미노출 안내).

## 권한·정책 기준

- AD-09: REVIEWER / SUPER_ADMIN (`requireReviewer`). 체크리스트는 버전·해시·상태 등 메타데이터만 다루며 체크 항목 원문은 노출하지 않는다.
- AD-10: OPERATOR / REVIEWER / SUPER_ADMIN (`requireMonitoring`). 읽기 전용, 원문 미포함.
- AD-03 상세: CLAUDE.md §7에 따라 산출물 원문·검증 참조 자료는 표시하지 않는다.

## 제외

- `qtai-server/**` 백엔드 수정.
- AD-01/02/06 백엔드(E단계) 본격 구현.
- 카카오 웹 로그인 정식화(임시 토큰 로그인 유지).
- 다른 worktree(restclient/deploy/batch-rc/sysauth 등) 변경.

## 검증

- `npm ci` → `npm run build`(tsc + vite): 통과 (3119 modules).
- `tsc --noEmit` 2차 패스: 통과.
- Vite chunk size 경고는 기존 antd 번들 경고로 비차단.
