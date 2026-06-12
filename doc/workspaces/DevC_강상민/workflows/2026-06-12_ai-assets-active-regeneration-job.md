# Workflow - 2026-06-12 ai-assets-active-regeneration-job

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-03 |
| 트리거 | 관리자 `AI 산출물 검증` 상세에서 이미 재생성 job이 진행 중이어도 재생성 버튼이 먼저 보이고, 한 번 눌러야 진행 중 표시로 바뀌는 문제 확인 |
| 기준 문서 | `admin-web/src/pages/AiAssetsPage.tsx`, `admin-web/src/api/aiAssets.ts`, `qtai-server/*/domain/ai/internal/AdminAiAssetQueryService.java` |
| 해당 경로 | `admin-web/src/api/aiAssets.ts`, `admin-web/src/pages/AiAssetsPage.tsx`, `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**`, `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**` |

## 작업 목표

AI 산출물 상세 화면이 같은 대상의 진행 중 재생성 job을 상세 조회 시점부터 알 수 있게 한다. 운영자는 이미 재생성 중인 산출물에서 재생성 버튼을 다시 누르지 않아도 `재생성 작업 진행 중` 상태를 즉시 확인할 수 있어야 한다.

기존 구조는 프런트엔드가 방금 클릭해서 받은 job만 로컬 상태로 기억했다. 서버에 이미 `QUEUED` 또는 `RUNNING` job이 있어도 상세 응답 계약에 active job 정보가 없어 첫 렌더에서 버튼을 숨길 수 없었다.

## 범위

- 관리자 AI 산출물 상세 응답에 같은 대상의 active generation job 정보를 추가한다.
- `QUEUED`, `RUNNING` generation job을 active로 판단한다.
- admin-web 상세 drawer가 `activeGenerationJob`을 사용해 재생성 버튼 대신 진행 중 태그를 표시한다.
- 기존 로컬 클릭 직후 상태 보정 로직은 유지한다.
- OpenAPI 상세 응답 스키마에 `activeGenerationJob`을 반영한다.
- TypeScript 검사와 서버 compile 검사를 실행한다.
- 작업 결과 report를 작성한다.

## 제외 범위

- generation job 목록 화면 또는 모니터링 화면 개편
- 재생성 취소, 재시도, 강제 실패 처리
- DB schema 변경
- Ant Design `destroyOnClose` deprecated 경고 정리

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `admin-web/src/api/aiAssets.ts` | 상세 응답 타입에 `activeGenerationJob` 추가 |
| Modify | `admin-web/src/pages/AiAssetsPage.tsx` | active job 기반 재생성 버튼/진행 중 태그 분기 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/api/admin/asset/dto/AdminAiAssetDetailResponse.java` | 상세 응답 DTO 계약 추가 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | 같은 대상의 최신 active generation job 조회 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryService.java` | active job을 상세 응답에 매핑 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**` | 중복 모듈의 동일 계약/구현 동기화 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 상세 응답 스키마에 `activeGenerationJob` 반영 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_ai-assets-active-regeneration-job_report.md` | 원인, 변경 내용, 검증 결과 기록 |

## 구현 순서

1. 상세 DTO에 `activeGenerationJob` nullable 필드를 추가한다.
2. 상세 query repository에 `jobType`, `targetType`, `targetId`, `status in (QUEUED, RUNNING)` 조건으로 최신 active job을 조회하는 메서드를 추가한다.
3. query service에서 상세 산출물의 job type/target 기준으로 active job을 조회해 응답에 포함한다.
4. admin-web API 타입에 `activeGenerationJob`을 추가한다.
5. 상세 drawer에서 `activeGenerationJob` 또는 기존 `generationJob`이 active 상태이면 재생성 버튼 대신 진행 중 태그를 표시한다.
6. 로컬에서 방금 생성한 job 상태는 기존처럼 우선 적용한다.
7. OpenAPI 상세 응답 스키마를 갱신한다.
8. 타입 검사, 서버 compile, 브라우저 렌더링을 확인한다.
9. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | 이번 변경은 admin 상세 응답 계약과 UI 분기 보강이다. 기존 테스트 구조가 없어 자동 테스트는 추가하지 않고 TypeScript, Java compile, 브라우저 렌더링으로 검증한다. |

## 수용 기준

- [ ] 상세 응답에 같은 대상의 active generation job이 포함된다.
- [ ] active 상태는 `QUEUED`, `RUNNING`만 해당한다.
- [ ] 이미 재생성 중이면 상세 drawer에서 재생성 버튼이 보이지 않는다.
- [ ] 대신 `재생성 작업 진행 중 · job #... (QUEUED/RUNNING)` 태그가 보인다.
- [ ] active job이 없으면 기존처럼 `REJECTED/HIDDEN`에서 재생성 버튼이 보인다.
- [ ] `npm.cmd --prefix admin-web run typecheck`가 통과한다.
- [ ] `.\qtai-server\gradlew.bat -p qtai-server :service-ai:compileJava :admin-server:compileJava`가 통과한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 원인이 상세 API 계약과 프런트 분기 사이의 단일 데이터 흐름에 있다.
- 서버 DTO, query, 프런트 타입/UI가 같은 계약을 순서대로 맞춰야 하므로 한 에이전트가 직접 추적하는 편이 안전하다.
- 병렬 작업은 중복 모듈 동기화 누락 위험을 키운다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 원인 조사, 서버 계약 수정, 프런트 UI 분기 수정, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
npm.cmd --prefix admin-web run typecheck
.\qtai-server\gradlew.bat -p qtai-server :service-ai:compileJava :admin-server:compileJava
```

브라우저 검증:

- `http://localhost:5173/ai-assets` 화면 렌더링 확인
- 목록과 상세 drawer 진입이 깨지지 않는지 확인
- active 재생성 job 데이터가 있는 경우 버튼 대신 진행 중 태그가 표시되는지 확인

## 후속 작업으로 남길 항목

- `destroyOnClose` deprecated 경고 정리
- active job을 상세 화면 본문에도 별도 섹션으로 보여줄지 UX 검토
