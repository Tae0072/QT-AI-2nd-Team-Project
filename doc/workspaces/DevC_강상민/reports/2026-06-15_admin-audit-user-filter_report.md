# AD-07 감사 로그 USER 필터 제거 리포트

## 1. 작업 정보

| 항목 | 내용 |
| --- | --- |
| 작업일 | 2026-06-15 |
| 브랜치 | `bugfix/admin-audit-user-filter` |
| PR 대상 | `dev` |
| 관련 명세 | `doc/workspaces/DevC_강상민/workflows/2026-06-15_admin-audit-user-filter.md` |
| 관련 화면 | AD-07 감사 로그 |

## 2. 변경 요약

- `admin-web/src/pages/AuditLogsPage.tsx`
  - 감사 로그 행위자 필터 옵션에서 `USER`를 제거했다.
  - 기존 안내 문구인 `관리자/시스템(SYSTEM_BATCH)` 기준 문구는 변경하지 않았다.
- `admin-web/src/api/auditLogs.ts`
  - `actorType` 주석에서 `USER` 언급을 제거했다.
  - `actorType` 타입은 `string`으로 유지했다.

## 3. 제외한 범위

- 백엔드 `audit_logs.actor_type` 컬럼은 변경하지 않았다.
- 백엔드 감사 로그 조회 API 동작은 변경하지 않았다.
- OpenAPI 계약은 변경하지 않았다.
- `USER_REPORT`, `ROLE_USER` 등 다른 기능의 `USER` 문자열은 변경하지 않았다.

## 4. 검증 결과

| 구분 | 명령 또는 확인 | 결과 |
| --- | --- | --- |
| 타입 체크 | `npm.cmd run typecheck` (`admin-web`) | 통과 |
| 테스트 | `npm.cmd test` (`admin-web`) | 통과 |
| 계약 테스트 | `admin-page-contracts.test.mjs`의 `audit log actor filter exposes only admin and system batch actors` | 통과 |
| 정적 확인 | `rg -n "label: 'USER'|value: 'USER'|ADMIN, SYSTEM_BATCH, USER" admin-web/src` | 결과 없음 |
| 수동 확인 | `http://127.0.0.1:5173/audit-logs` 접속 | 인증 세션이 없어 `/login`으로 리다이렉트되어 화면 옵션 확인은 미완료 |

PowerShell 실행 정책 때문에 `npm run typecheck`는 `npm.ps1` 로딩 단계에서 차단되었다. 동일 검증은 Windows 실행 파일인 `npm.cmd`로 재실행해 통과를 확인했다.

## 5. 수용 기준 확인

- 감사 로그 화면 필터 상수에서 `USER` 옵션을 제거했다.
- 화면 문구는 관리자/시스템 기준으로 유지했다.
- `actorType` 타입과 백엔드 계약은 유지했다.
- 지정 경로에 작업 명세와 리포트를 작성했다.
- 자동 검증과 정적 확인을 완료했다.
- `npm test`에서 감사 로그 행위자 필터가 `ADMIN`, `SYSTEM_BATCH`만 포함하고 `USER`를 포함하지 않는 계약 테스트를 실행한다.

## 6. 리뷰 재확인 결과

Claude 자동 리뷰에서 PR diff에 문서 2개만 포함되어 있고 실제 코드 변경과 계약 테스트 변경이 보이지 않는다는 `REQUEST_CHANGES`가 있었다. 로컬 Git과 GitHub compare 기준으로 최신 PR diff를 다시 확인한 결과, 해당 리뷰는 stale diff 기준으로 작성된 것으로 판단했다.

현재 PR #646의 최신 diff 기준 변경 파일은 다음 5개다.

- `admin-web/src/pages/AuditLogsPage.tsx`
- `admin-web/src/api/auditLogs.ts`
- `admin-web/scripts/admin-page-contracts.test.mjs`
- `doc/workspaces/DevC_강상민/workflows/2026-06-15_admin-audit-user-filter.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-15_admin-audit-user-filter_report.md`

GitHub compare 기준 `dev...bugfix/admin-audit-user-filter`는 2 commits ahead이며, 위 5개 파일이 모두 포함되어 있음을 확인했다. 따라서 “문서 2개만 포함되어 실제 코드 변경을 확인할 수 없다”는 BLOCK은 현재 PR 상태와 맞지 않는다.

추가로 `admin-page-contracts.test.mjs`에 `audit log actor filter exposes only admin and system batch actors` 계약 테스트를 추가해, 감사 로그 행위자 필터가 `ADMIN`, `SYSTEM_BATCH`만 포함하고 `USER`를 포함하지 않음을 자동 검증하도록 보강했다.

백엔드 `actorType=USER` 쿼리 검증은 최초 작업 범위대로 제외했으며, 후속 검토 항목으로 문서에 남겼다.
