# Workflow - 2026-06-12 ai-assets-remove-asset-type-filter

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | 관리자 `AI 산출물 검증` QA 중 상단 `유형` 필터의 `성경구절(BIBLE_VERSE)` 옵션이 산출물 타입이 아니라 대상 타입 개념이라 혼동을 준다고 판단 |
| 기준 문서 | `doc/프로젝트 문서/04_API_명세서.md`, `admin-web/src/api/aiAssets.ts`, `admin-web/src/pages/AiAssetsPage.tsx` |
| 해당 경로 | `admin-web/src/pages/AiAssetsPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`AI 산출물 검증` 화면 상단에서 산출물 유형 필터를 제거한다. 현재 운영 흐름은 `assetType=EXPLANATION`, `targetType=BIBLE_VERSE` 구조에 가깝고, `BIBLE_VERSE`를 산출물 유형 필터로 노출하면 사용자가 성경 구절 산출물 자체가 별도로 존재한다고 오해할 수 있다.

목록/상세의 산출물 유형과 대상 표시는 검증 맥락에 필요하므로 유지하고, 필터는 실제 검수 업무에 유효한 상태 필터만 남긴다.

## 범위

- `AiAssetsPage` 상단 필터 영역에서 `유형` Select를 제거한다.
- `assetType` 상태값과 `setAssetType` 호출을 제거한다.
- 조회/초기화 요청에서 `assetType` 파라미터를 더 이상 보내지 않는다.
- 목록 컬럼과 상세 Drawer의 `assetType`, `targetType`, `targetId` 표시는 유지한다.
- `admin-web/src/api/aiAssets.ts`의 API 타입은 유지해 서버 계약과 향후 확장 가능성을 보존한다.

## 제외 범위

- 서버 API, OpenAPI, DB schema 변경
- `targetType` 필터 신규 추가
- 목록 컬럼 제거 또는 상세 Drawer 대상 정보 제거
- 승인/반려/숨김/재생성 액션 변경
- AI 산출물 생성/검증 서버 로직 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/AiAssetsPage.tsx` | `유형` Select와 관련 상태/필터 파라미터 제거 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-remove-asset-type-filter_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. `ASSET_TYPE_OPTIONS` 상수를 제거한다.
2. `assetType` React state와 `setAssetType` 사용처를 제거한다.
3. `onSearch`, `onReset`에서 `assetType` 파라미터를 제거한다.
4. 필터 영역 JSX에서 첫 번째 `Select`를 제거하고 상태 Select만 남긴다.
5. `admin-web` TypeScript 검사를 실행한다.
6. 브라우저에서 `/ai-assets`를 새로고침하고 상단 필터에 상태 Select만 남았는지 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 단일 프론트 UI 필터 제거라 신규 자동 테스트는 추가하지 않고 TypeScript 검사와 브라우저 수동 검증으로 확인 |

## 수용 기준

- [ ] 상단 필터에서 `유형` Select가 보이지 않는다.
- [ ] 상단 필터에는 `상태` Select, 조회, 초기화, 새로고침만 남는다.
- [ ] 목록/상세의 산출물 유형과 대상 정보 표시는 유지된다.
- [ ] 조회 요청이 `assetType` 없이 동작한다.
- [ ] `admin-web` TypeScript 검사가 통과한다.
- [ ] 브라우저에서 `/ai-assets` 화면 변경을 확인한다.
- [ ] report에 원인과 검증 결과가 남는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 프론트 화면의 필터 UI와 상태 관리에 한정된다.
- 코드 수정, 브라우저 확인, 문서 작성이 같은 화면 맥락에서 순차적으로 검증되어야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 코드 수정, 타입 검사, 브라우저 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
```

브라우저 검증:

- `http://localhost:5173/ai-assets` 새로고침
- 상단 필터에 `유형` Select가 사라졌는지 확인
- `상태` Select는 유지되고, 조회/초기화/새로고침 버튼이 정상 표시되는지 확인

## 후속 작업으로 남길 항목

- 향후 `assetType`이 여러 종류로 운영되면 그때 필터를 다시 도입한다.
- 필요하면 별도 작업으로 `targetType` 기반 필터를 설계한다.
