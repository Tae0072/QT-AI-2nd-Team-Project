# Workflow - 2026-06-15 admin-audit-user-filter

## 1. 작업 개요

| 항목 | 내용 |
| --- | --- |
| 담당 | DevC 강상민 |
| 작업 브랜치 | `bugfix/admin-audit-user-filter` |
| PR 대상 | `dev` |
| 관련 화면 | AD-07 감사 로그 |
| 대상 앱 | `admin-web` |
| 목표 | 감사 로그 행위자 필터에서 `USER` 옵션만 제거하고 화면 문구는 관리자/시스템 기준으로 유지 |

## 2. 배경과 판단

AD-07 감사 로그 화면은 현재 관리자와 시스템 배치의 주요 행위 기록을 조회하는 화면이다. 화면 안내 문구도 `관리자/시스템(SYSTEM_BATCH)` 기준으로 작성되어 있으므로, 행위자 필터에 `USER`가 노출되면 현재 화면 목적과 맞지 않는다.

다만 `audit_logs.actor_type` 컬럼은 향후 사용자 감사 로그 요구사항이 생길 수 있는 확장 지점이다. 이번 작업에서는 프런트엔드 필터 노출만 조정하고 백엔드 DB 스키마, 조회 API 동작, OpenAPI 계약은 변경하지 않는다.

## 3. 범위

### 포함

- `admin-web/src/pages/AuditLogsPage.tsx`
  - `ACTOR_TYPE_OPTIONS`에서 `USER` 옵션 제거
  - 기존 안내 문구 유지
- `admin-web/src/api/auditLogs.ts`
  - `actorType` 주석에서 `USER` 언급 제거
  - 타입은 `string` 유지
- 작업 리포트 작성
  - `doc/workspaces/DevC_강상민/reports/2026-06-15_admin-audit-user-filter_report.md`

### 제외

- 백엔드 `audit_logs.actor_type` 컬럼 변경
- 백엔드 조회 API의 `actorType` 검증 추가
- OpenAPI 변경
- 감사 로그 저장 로직 변경
- `USER_REPORT`, `ROLE_USER` 등 다른 도메인의 `USER` 문자열 변경

## 4. 파일 구조와 책임

| 파일 | 작업 | 책임 |
| --- | --- | --- |
| `admin-web/src/pages/AuditLogsPage.tsx` | 수정 | AD-07 화면 행위자 필터 옵션 정의 |
| `admin-web/src/api/auditLogs.ts` | 수정 | 감사 로그 API 타입 주석 정리 |
| `doc/workspaces/DevC_강상민/reports/2026-06-15_admin-audit-user-filter_report.md` | 생성 | 변경 결과와 검증 내역 기록 |

## 5. 구현 순서

1. `dev` 기준으로 `bugfix/admin-audit-user-filter` 브랜치를 생성한다.
2. `AuditLogsPage.tsx`의 `ACTOR_TYPE_OPTIONS`에서 `{ label: 'USER', value: 'USER' }` 항목만 제거한다.
3. 화면 안내 문구가 `관리자/시스템(SYSTEM_BATCH)` 기준으로 유지되는지 확인한다.
4. `auditLogs.ts`의 `actorType` 주석을 `ADMIN, SYSTEM_BATCH` 기준으로 정리한다.
5. 정적 검색으로 AD-07 필터 제거 대상 문자열이 남아 있지 않은지 확인한다.
6. `admin-web`에서 타입 체크와 테스트를 실행한다.
7. 가능하면 `/audit-logs` 화면에서 행위자 필터가 `ADMIN`, `SYSTEM_BATCH`만 노출되는지 수동 확인한다.
8. 작업 리포트를 작성한다.
9. 변경 파일만 스테이징하고 Conventional Commits 형식으로 커밋한다.

## 6. 테스트 계획

| 구분 | 명령 또는 확인 | 기대 결과 |
| --- | --- | --- |
| 타입 체크 | `npm run typecheck` (`admin-web`) | 성공 |
| 테스트 | `npm test` (`admin-web`) | 성공 |
| 계약 테스트 | `admin-page-contracts.test.mjs`에서 AD-07 행위자 필터 옵션 검증 | `ADMIN`, `SYSTEM_BATCH`만 포함하고 `USER` 없음 |
| 정적 확인 | `rg -n "label: 'USER'|value: 'USER'|ADMIN, SYSTEM_BATCH, USER" admin-web/src` | 결과 없음 |
| 수동 확인 | `/audit-logs` 행위자 필터 확인 | `ADMIN`, `SYSTEM_BATCH`만 표시 |

## 7. 수용 기준

- 감사 로그 화면 행위자 필터에 `USER` 옵션이 노출되지 않는다.
- 감사 로그 화면 안내 문구는 기존 `관리자/시스템(SYSTEM_BATCH)` 기준 문구를 유지한다.
- `actorType` 타입은 `string`으로 유지된다.
- 백엔드 DB 스키마와 조회 API 동작은 변경되지 않는다.
- 리포트가 지정 경로에 작성된다.
- 검증 명령 결과가 리포트와 최종 응답에 기록된다.

## 8. Subagent Decision

직접 실행한다.

| 판단 항목 | 결정 |
| --- | --- |
| 병렬 작업 필요성 | 낮음 |
| 변경 파일 수 | 프런트엔드 2개와 문서 2개 |
| 공유 상태 충돌 가능성 | 단일 상수와 주석 변경이라 직접 처리 가능 |
| 위임 여부 | 위임하지 않음 |

이번 작업은 작은 범위의 UI 옵션 제거와 문서화 작업이다. 하위 에이전트를 나누면 산출물 조율 비용이 변경 자체보다 커지므로, 명세 작성 후 같은 세션에서 직접 실행한다.

## 9. 커밋 계획

```text
fix(admin): 감사 로그 행위자 필터에서 USER 제거
```

## 10. 후속 검토 항목

- 사용자 감사 로그 요구사항이 별도로 생기면 사용자 감사 로그 화면 또는 별도 필터 정책을 다시 정의한다.
- 백엔드에서 엄격 검증이 필요해지면 `actorType` 쿼리 파라미터를 `ADMIN`, `SYSTEM_BATCH`로 제한하는 변경을 별도 작업으로 검토한다.
