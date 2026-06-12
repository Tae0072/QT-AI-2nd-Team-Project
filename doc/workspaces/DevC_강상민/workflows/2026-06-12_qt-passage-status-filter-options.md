# Workflow - 2026-06-12 qt-passage-status-filter-options

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-02 |
| 트리거 | 관리자 `/qt-passages` QA 중 상태 필터에 현재 화면에서 전이하거나 생성할 수 없는 `삭제 예정`, `제거됨` 값이 노출되는 것을 확인 |
| 기준 문서 | `doc/프로젝트 문서/04_API_명세서.md`, `doc/workspaces/DevE_김지민/workflows/2026-06-10_admin-qt-passages-api-contract.md` |
| 해당 경로 | `admin-web/src/pages/QtPassagesPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

오늘 QT 관리 화면의 상태 필터에서 현재 운영자가 실제로 사용할 수 있는 상태만 노출한다. 서버/API 계약과 목록 Tag 매핑은 5종 상태를 유지하되, 필터 드롭다운은 `검토 대기`, `게시됨`, `숨김` 3개만 제공해 사용자가 결과가 나오지 않는 비활성 상태를 선택하지 않게 한다.

## 범위

- `QtPassagesPage`의 상태 필터 옵션을 실제 운영 플로우에서 생성/전이 가능한 `pending_review`, `active`, `hidden`으로 제한한다.
- `STATUS_META`는 5종 상태를 유지해 서버가 `deletion_notified` 또는 `removed` row를 반환하더라도 Tag 표시가 깨지지 않게 한다.
- `QtPassageStatus` API 타입은 유지해 서버 계약과 호환성을 보존한다.
- 화면에서 상태 드롭다운을 열었을 때 `삭제 예정`, `제거됨`이 노출되지 않는지 브라우저로 확인한다.

## 제외 범위

- 서버 `QtPassageStatus` enum 축소
- DB migration 또는 기존 데이터 변경
- 삭제 예정/제거 상태로 전이하는 신규 API나 버튼 구현
- QT 목록 기본 필터, 페이지명, 등록/수정 폼 변경
- 이전 QT 게시 상태 노출 게이트 작업의 서버 코드 재수정

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/QtPassagesPage.tsx` | 상태 Tag 매핑은 유지하고 필터 옵션만 운영 가능 상태 3개로 제한 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_qt-passage-status-filter-options_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. `STATUS_META`와 별도로 필터에 노출할 상태 목록 상수를 추가한다.
2. `STATUS_OPTIONS`가 전체 enum 키가 아니라 필터 노출 목록에서 생성되도록 변경한다.
3. TypeScript 타입 검사를 실행해 `QtPassageStatus` 호환성이 유지되는지 확인한다.
4. 현재 브라우저 `/qt-passages` 화면을 새로고침하고 상태 드롭다운 옵션이 3개만 보이는지 확인한다.
5. 작업 report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 단일 프론트 상수 변경이라 신규 자동 테스트는 추가하지 않고 TypeScript 검사와 브라우저 수동 검증으로 확인 |

## 수용 기준

- [ ] 상태 필터에는 `검토 대기`, `게시됨`, `숨김`만 표시된다.
- [ ] 목록 Tag는 기존 5종 상태 매핑을 유지한다.
- [ ] API 타입 `QtPassageStatus`는 5종 상태를 유지한다.
- [ ] `admin-web` TypeScript 검사가 통과한다.
- [ ] 브라우저에서 `/qt-passages` 드롭다운 옵션이 3개만 보이는 것을 확인한다.
- [ ] report에 원인과 검증 결과가 남는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 프론트 화면의 상태 필터 상수에 한정된다.
- 구현, 검증, 문서 작성이 같은 맥락에서 순차 확인되어야 하므로 직접 실행이 더 안전하다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 코드 수정, 브라우저 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm --prefix admin-web run typecheck
```

브라우저 검증:

- `http://localhost:5173/qt-passages` 새로고침
- 상태 필터 드롭다운 열기
- `검토 대기`, `게시됨`, `숨김`만 보이고 `삭제 예정`, `제거됨`은 보이지 않는지 확인

## 후속 작업으로 남길 항목

- 삭제 예정/제거 상태가 실제 운영 플로우에 필요해지면 별도 서버 API와 화면 액션을 설계한다.
