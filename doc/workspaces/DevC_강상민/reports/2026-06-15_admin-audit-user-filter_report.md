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
| 정적 확인 | `rg -n "label: 'USER'|value: 'USER'|ADMIN, SYSTEM_BATCH, USER" admin-web/src` | 결과 없음 |
| 수동 확인 | `http://127.0.0.1:5173/audit-logs` 접속 | 인증 세션이 없어 `/login`으로 리다이렉트되어 화면 옵션 확인은 미완료 |

PowerShell 실행 정책 때문에 `npm run typecheck`는 `npm.ps1` 로딩 단계에서 차단되었다. 동일 검증은 Windows 실행 파일인 `npm.cmd`로 재실행해 통과를 확인했다.

## 5. 수용 기준 확인

- 감사 로그 화면 필터 상수에서 `USER` 옵션을 제거했다.
- 화면 문구는 관리자/시스템 기준으로 유지했다.
- `actorType` 타입과 백엔드 계약은 유지했다.
- 지정 경로에 작업 명세와 리포트를 작성했다.
- 자동 검증과 정적 확인을 완료했다.
