# Workflow - 2026-06-12 ai-assets-regenerate-button-visibility

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | 관리자 `AI 산출물 검증` 상세 Drawer에서 승인(`APPROVED`) 산출물에도 `재생성` 버튼이 노출되어, 누르면 서버 상태 전이 오류가 발생할 수 있음 |
| 기준 문서 | `qtai-server/apis/api-v1/openapi.yaml`, `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiService.java`, `admin-web/src/pages/AiAssetsPage.tsx` |
| 해당 경로 | `admin-web/src/pages/AiAssetsPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

AI 산출물 상세 Drawer의 `재생성` 버튼 노출 조건을 서버 재생성 계약과 맞춘다. 서버는 `REJECTED` 또는 `HIDDEN` 상태의 산출물만 재생성을 허용하는데, 현재 프론트는 상세 데이터가 있으면 상태와 무관하게 `재생성` 버튼을 노출한다.

사용자가 누를 수 없는 액션을 버튼으로 보여주지 않도록 하여, 승인(`APPROVED`) 또는 검증중(`VALIDATING`) 산출물에서 불필요한 오류 토스트가 발생하지 않게 한다.

## 범위

- `AiAssetsPage`에 재생성 가능 상태 판정 함수를 추가한다.
- 상세 Drawer의 `재생성` 버튼은 `REJECTED` 또는 `HIDDEN` 상태에서만 노출한다.
- 재생성 모달과 API 호출 로직은 유지한다.
- 승인/반려/숨김 액션 로직은 변경하지 않는다.

## 제외 범위

- 서버 재생성 허용 상태 변경
- 승인 산출물의 재생성 허용 정책 변경
- 재생성 요청 payload, promptVersion 입력 방식 변경
- AI 생성 작업 중복 처리 로직 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/AiAssetsPage.tsx` | 상세 Drawer 재생성 버튼 노출 조건을 서버 계약과 일치 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-regenerate-button-visibility_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. 브라우저에서 승인 산출물 상세에 `재생성` 버튼이 노출되는지 재현한다.
2. 서버 `AiService.requireRegeneratableStatus`에서 허용 상태가 `REJECTED/HIDDEN`인지 확인한다.
3. `AiAssetsPage`에 `isRegeneratable` helper를 추가한다.
4. Drawer `extra` 렌더링 조건을 `selectedAsset && isRegeneratable(selectedAsset.status)`로 변경한다.
5. `admin-web` TypeScript 검사를 실행한다.
6. 브라우저에서 승인 산출물 상세에는 `재생성` 버튼이 없고, 반려 또는 숨김 산출물 상세에는 버튼이 남는지 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 단일 화면 버튼 노출 조건 수정으로 자동 테스트는 추가하지 않고 TypeScript 검사와 브라우저 수동 검증으로 확인 |

## 수용 기준

- [ ] `APPROVED` 산출물 상세 Drawer에는 `재생성` 버튼이 노출되지 않는다.
- [ ] `VALIDATING` 산출물 상세 Drawer에는 `재생성` 버튼이 노출되지 않는다.
- [ ] `REJECTED` 또는 `HIDDEN` 산출물 상세 Drawer에는 `재생성` 버튼이 노출된다.
- [ ] 기존 재생성 모달과 요청 로직은 유지된다.
- [ ] `npm.cmd --prefix admin-web run typecheck`가 성공한다.
- [ ] 브라우저에서 원래 오류를 유발하던 승인 상세 버튼 노출이 사라진다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 화면의 버튼 노출 조건에 한정된다.
- 재현, 수정, 브라우저 확인이 같은 화면 상태를 유지하며 순차적으로 진행되어야 한다.

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
rg -n "[T]BD|[T]ODO|[p]laceholder|\\?\\?|[나]중|[적]절" doc/workspaces/DevC_강상민/workflows/2026-06-12_ai-assets-regenerate-button-visibility.md doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-regenerate-button-visibility_report.md
```

브라우저 검증:

- `http://localhost:5173/ai-assets` 새로고침
- 승인 산출물 상세 Drawer 확인
- 반려 또는 숨김 산출물 상세 Drawer 확인

## 후속 작업으로 남길 항목

- 정책상 승인 산출물도 재생성해야 한다면 서버 `requireRegeneratableStatus`와 운영 정책부터 별도 작업으로 변경한다.
