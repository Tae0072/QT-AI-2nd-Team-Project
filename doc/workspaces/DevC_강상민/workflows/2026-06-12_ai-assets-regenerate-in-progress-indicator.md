# Workflow - 2026-06-12 ai-assets-regenerate-in-progress-indicator

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | 숨김/반려 산출물에서 재생성 요청이 성공한 뒤 같은 상세에서 다시 `재생성`을 누르면 서버가 진행 중 job 때문에 거절하여 사용자가 실패로 오해할 수 있음 |
| 기준 문서 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiService.java`, `admin-web/src/pages/AiAssetsPage.tsx`, `admin-web/src/api/aiAssets.ts` |
| 해당 경로 | `admin-web/src/pages/AiAssetsPage.tsx`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

재생성 요청이 정상 접수된 직후 같은 산출물 상세에서 다시 요청 버튼을 누를 수 없게 한다. 서버는 같은 대상의 `QUEUED/RUNNING` 생성 작업이 있으면 중복 재생성을 거절한다. 현재 프론트는 첫 요청 성공 뒤에도 상세 Drawer에 `재생성` 버튼을 계속 보여줘, 두 번째 클릭의 거절 메시지가 “첫 요청이 실패한 것처럼” 보일 수 있다.

재생성 성공 응답의 `generationJobId`와 `status`를 화면 상태에 보관하고, 해당 산출물 상세에서는 버튼 대신 진행 중 job 표시를 보여준다.

## 범위

- 재생성 성공 후 `assetId -> generationJobId/status`를 프론트 상태로 저장한다.
- 같은 상세 Drawer에서는 `재생성` 버튼 대신 `재생성 작업 진행중 · job #...` 표시를 보여준다.
- 재생성 모달과 API payload는 유지한다.
- 서버 API와 상세 응답 DTO는 변경하지 않는다.

## 제외 범위

- 서버에 active regeneration job 조회 필드 추가
- job 상태 polling 또는 자동 갱신
- 재생성 완료 후 새 산출물로 자동 이동
- 중복 job 정책 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/pages/AiAssetsPage.tsx` | 재생성 성공 후 진행 중 job 표시 상태 관리 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-regenerate-in-progress-indicator_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. 현재 서버가 active job 중복 요청을 거절하는 로직을 확인한다.
2. `AiAssetsPage`에 `regenerationJobsByAssetId` 상태를 추가한다.
3. 재생성 성공 시 응답의 `generationJobId/status`를 해당 `assetId`에 저장한다.
4. 상세 Drawer의 extra 영역에서 해당 assetId에 저장된 job이 있으면 버튼 대신 진행 중 표시를 렌더링한다.
5. `admin-web` TypeScript 검사를 실행한다.
6. 브라우저에서 숨김/반려 상세 재생성 성공 후 버튼이 진행 중 표시로 바뀌는지 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 단일 화면 상태 표시 수정으로 자동 테스트는 추가하지 않고 TypeScript 검사와 브라우저 수동 검증으로 확인 |

## 수용 기준

- [ ] 재생성 요청 성공 후 같은 산출물 상세에서 `재생성` 버튼이 다시 노출되지 않는다.
- [ ] 성공 후 `재생성 작업 진행중`과 job ID가 표시된다.
- [ ] 재생성 요청 전 숨김/반려 산출물에는 기존처럼 `재생성` 버튼이 표시된다.
- [ ] 승인/검증중 산출물에는 기존처럼 `재생성` 버튼이 표시되지 않는다.
- [ ] `npm.cmd --prefix admin-web run typecheck`가 성공한다.
- [ ] 서버 API/DTO 변경 없이 동작한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 화면의 로컬 UI 상태 관리에 한정된다.
- 재생성 성공 후 바로 같은 Drawer 상태를 검증해야 하므로 순차 실행이 적합하다.

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
rg -n "[T]BD|[T]ODO|[p]laceholder|\\?\\?|[나]중|[적]절" doc/workspaces/DevC_강상민/workflows/2026-06-12_ai-assets-regenerate-in-progress-indicator.md doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-regenerate-in-progress-indicator_report.md
```

브라우저 검증:

- `http://localhost:5173/ai-assets`에서 숨김 또는 반려 산출물 상세 열기
- 재생성 요청 성공
- 같은 상세에서 `재생성` 버튼 대신 진행 중 job 표시가 나오는지 확인

## 후속 작업으로 남길 항목

- 새로고침 후에도 active job 표시가 필요하면 상세 API에 active generation job 요약 필드를 추가하는 별도 서버 작업이 필요하다.
