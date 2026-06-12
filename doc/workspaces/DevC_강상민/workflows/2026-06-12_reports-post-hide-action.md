# Workflow - 2026-06-12 reports-post-hide-action

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-04 |
| 트리거 | 관리자 `신고 처리` 화면 점검 중 POST 신고 처리 시 백엔드 숨김 액션이 호출되지 않는 구조 확인 |
| 기준 문서 | `admin-web/src/pages/ReportsPage.tsx`, `admin-web/src/api/reports.ts`, `qtai-server/admin-server/src/main/java/com/qtai/domain/report/internal/AdminReportService.java` |
| 해당 경로 | `admin-web/src/api/reports.ts`, `admin-web/src/pages/ReportsPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

관리자 신고 처리 화면에서 POST 신고를 `처리(인정)`할 때 서버가 지원하는 `HIDE_TARGET` 액션을 함께 보내도록 보강한다. 현재 백엔드는 `RESOLVED + HIDE_TARGET + POST` 조건에서 게시글 숨김 유스케이스를 호출하지만, 프런트엔드가 `action` 필드를 보내지 않아 실제 대상 조치가 빠질 수 있다.

댓글, AI Q&A, AI 산출물 대상의 후속 조치는 서버 주석상 추후 도메인 연동 범위이므로 이번 작업에서는 POST 처리에만 액션을 연결한다.

## 범위

- 신고 처리 API payload 타입에 `action` 필드를 추가한다.
- `ReportsPage`에서 `resolve` 모드이고 대상이 `POST`인 경우 `action: HIDE_TARGET`을 전송한다.
- 처리 모달에서 POST 처리 시 대상 게시글 숨김이 함께 수행됨을 운영자가 알 수 있게 표시한다.
- 타입 검사를 실행하고 브라우저에서 `/reports` 화면이 정상 렌더링되는지 확인한다.
- 작업 결과 report를 작성한다.

## 제외 범위

- 서버 API, DB schema, OpenAPI 변경
- 댓글/AI Q&A/AI 산출물 대상 숨김 또는 후속 조치 연동
- 신고 상태 `REVIEWING` 전환 API/UI 추가
- 대상 상세 페이지 링크 추가
- 기존 처리 완료 데이터 재처리

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/api/reports.ts` | `ProcessReportPayload.action` 타입 추가 |
| Modify | `admin-web/src/pages/ReportsPage.tsx` | POST resolve 시 `HIDE_TARGET` 전송 및 모달 안내 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_reports-post-hide-action_report.md` | 원인, 변경 내용, 검증 결과 기록 |

## 구현 순서

1. `ProcessReportPayload`에 `action?: 'HIDE_TARGET'`를 추가한다.
2. `submitAction` payload에 타입을 명시한다.
3. `action.mode === 'resolve' && action.report.targetType === 'POST'` 조건에서 `payload.action = 'HIDE_TARGET'`를 설정한다.
4. 모달 본문에 같은 조건일 때 “처리 시 대상 나눔글이 숨김 처리됩니다.” 안내를 표시한다.
5. `admin-web` TypeScript 검사를 실행한다.
6. 브라우저에서 `/reports` 화면을 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | UI payload 연결의 좁은 변경으로 자동 테스트는 추가하지 않고 TypeScript 검사와 브라우저 렌더링으로 검증 |

## 수용 기준

- [ ] `ProcessReportPayload`가 서버 `ProcessReportRequest.action` 계약을 표현한다.
- [ ] POST 신고를 처리할 때 `HIDE_TARGET` 액션이 전송된다.
- [ ] POST가 아닌 대상은 기존처럼 `action` 없이 처리된다.
- [ ] 처리 모달에서 POST 숨김 조치가 운영자에게 보인다.
- [ ] `npm.cmd --prefix admin-web run typecheck`가 통과한다.
- [ ] `/reports` 화면이 브라우저에서 정상 렌더링된다.
- [ ] report에 원인과 검증 결과가 남는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 프런트엔드 두 파일의 단일 payload 계약 보강에 한정된다.
- workflow 작성, 코드 수정, 브라우저 확인, report 작성이 같은 화면 맥락에서 순차적으로 검증되어야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 코드 수정, 타입 검사, 브라우저 확인, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
```

브라우저 검증:

- `http://localhost:5173/reports` 화면 렌더링 확인
- 목록, 필터, 처리 완료 상태 표시가 깨지지 않는지 확인
- 현재 데이터가 모두 종료 상태라 실제 처리 제출은 수행하지 않는다.

## 후속 작업으로 남길 항목

- 댓글/AI 대상 신고 처리 시 실제 대상 조치 정책 확정
- `REVIEWING` 상태 전환이 필요한지 정책 결정
- 대상 상세로 이동할 수 있는 링크 또는 drawer 추가 검토
