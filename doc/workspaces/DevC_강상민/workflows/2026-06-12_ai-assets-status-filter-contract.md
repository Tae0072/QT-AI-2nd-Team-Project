# Workflow - 2026-06-12 ai-assets-status-filter-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | 관리자 `AI 산출물 검증` 화면에서 상태 필터 `검토필요(NEEDS_REVIEW)` 선택 후 조회 시 `올바르지 않은 요청입니다.` 메시지가 표시됨 |
| 기준 문서 | `qtai-server/apis/api-v1/openapi.yaml`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AiGeneratedAssetStatus.java`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AiValidationResult.java` |
| 해당 경로 | `admin-web/src/pages/AiAssetsPage.tsx`, `admin-web/src/api/aiAssets.ts`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`AI 산출물 검증` 목록의 상단 상태 필터가 서버 목록 API의 `status` query enum과 같은 값만 보내도록 맞춘다. 현재 `NEEDS_REVIEW`는 산출물 상태가 아니라 검증 로그 결과값인데 상태 필터에 노출되어, 서버가 `AiGeneratedAssetStatus` 파싱 중 `INVALID_INPUT`으로 거절한다.

표의 `검증결과`에는 `NEEDS_REVIEW` 표시를 유지하되, 상단 `상태` 필터와 `AiAsset.status` 설명에서는 산출물 상태 enum만 다룬다.

## 범위

- `AiAssetsPage` 상단 `STATUS_OPTIONS`에서 `검토필요(NEEDS_REVIEW)` 옵션을 제거한다.
- `statusTag`는 산출물 상태 표시 기준으로 정리한다.
- `isReviewable` 조건을 실제 승인/반려 가능한 산출물 상태인 `VALIDATING` 기준으로 정리한다.
- `AiAsset.status` 타입 주석을 `VALIDATING / APPROVED / REJECTED / HIDDEN`으로 수정한다.
- 기존 목록의 `latestValidationResult`와 상세 검증 로그의 `NEEDS_REVIEW` 표시는 유지한다.

## 제외 범위

- 서버 enum, OpenAPI, DB schema 변경
- `AiValidationResult.NEEDS_REVIEW` 제거 또는 표시 제거
- 승인/반려/숨김/재생성 API 동작 변경
- 평가셋/평가케이스 후보 등록 기능 연결

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/AiAssetsPage.tsx` | 상태 필터 옵션과 산출물 상태 표시/액션 조건 정리 |
| Modify | `admin-web/src/api/aiAssets.ts` | `AiAsset.status` 설명을 서버 산출물 상태 enum에 맞춤 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-status-filter-contract_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. 브라우저에서 `/ai-assets` 상태 필터 `검토필요(NEEDS_REVIEW)` 선택 후 `조회` 시 오류가 나는지 재현한다.
2. 서버 `AiGeneratedAssetStatus`와 OpenAPI 목록 API status enum을 확인한다.
3. `STATUS_OPTIONS`에서 `NEEDS_REVIEW`를 제거한다.
4. `statusTag`와 `isReviewable`에서 산출물 상태가 아닌 `NEEDS_REVIEW` 의존을 제거한다.
5. `AiAsset.status` 주석을 실제 서버 enum으로 수정한다.
6. `admin-web` TypeScript 검사를 실행한다.
7. 브라우저에서 `/ai-assets` 상태 드롭다운에 `검토필요`가 없는지 확인하고, 유효 상태 조회가 오류 없이 동작하는지 확인한다.
8. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 단일 프론트 필터 옵션 계약 수정으로 자동 테스트는 추가하지 않고 TypeScript 검사와 브라우저 수동 검증으로 확인 |

## 수용 기준

- [ ] 상태 필터 드롭다운에 `검토필요(NEEDS_REVIEW)`가 노출되지 않는다.
- [ ] 상태 필터에는 `검증중(VALIDATING)`, `승인(APPROVED)`, `반려(REJECTED)`, `숨김(HIDDEN)`만 남는다.
- [ ] `검증결과` 컬럼과 상세 검증 로그의 `NEEDS_REVIEW` 표시는 유지된다.
- [ ] `승인/반려` 버튼은 실제 전이 가능한 `VALIDATING` 산출물에만 노출된다.
- [ ] `npm.cmd --prefix admin-web run typecheck`가 성공한다.
- [ ] 브라우저에서 `/ai-assets` 필터 조회가 `올바르지 않은 요청입니다.`를 유발하지 않는다.
- [ ] report에 재현 원인과 검증 결과가 기록된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `AiAssetsPage`와 `aiAssets.ts`의 작은 계약 정리에 한정된다.
- 브라우저 재현, 코드 수정, 타입 검증, 보고서 작성이 같은 화면 맥락에서 순차적으로 이어져야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 코드 수정, 타입 검사, 브라우저 재검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
git diff --check
rg -n "[T]BD|[T]ODO|[p]laceholder|\\?\\?|[나]중|[적]절" doc/workspaces/DevC_강상민/workflows/2026-06-12_ai-assets-status-filter-contract.md doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-status-filter-contract_report.md
```

브라우저 검증:

- `http://localhost:5173/ai-assets` 새로고침
- 상태 Select 옵션 확인
- `검증중(VALIDATING)` 또는 다른 유효 상태로 조회해 오류 메시지가 뜨지 않는지 확인

## 후속 작업으로 남길 항목

- 검증 결과(`AiValidationResult`)를 별도 필터로 제공할 필요가 생기면 `latestValidationResult` 전용 필터를 별도 API 계약으로 추가 검토한다.
