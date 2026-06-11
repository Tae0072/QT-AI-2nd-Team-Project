# Workflow - 2026-06-11 admin-ad0106-response-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/admin-ad0106-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-01, AD-06 |
| 트리거 | 코드리뷰 후속 TODO 2: AD-01/AD-06 응답 계약 확정 후 김지민에게 전달 |
| 기준 문서 | `doc/workspaces/DevC_강상민/2026-06-10_코드리뷰_TODO_강상민.md`, `qtai-server/apis/api-v1/openapi.yaml`, AD-01/AD-06 구현 report |
| 담당 경로 | `doc/workspaces/DevC_강상민/contracts/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

admin-web AD-01 대시보드와 AD-06 공지 화면의 임시 DTO를 실제 백엔드 응답 계약으로 교체할 수 있도록, 확정된 API envelope, request, response, 상태 코드, nullability를 문서화한다.

## 범위

- AD-01 `GET /api/v1/admin/dashboard` 응답 계약 문서화.
- AD-06 공지 목록/생성/수정/발행/숨김 API 계약 문서화.
- 공통 `ApiResponse<T>` envelope와 페이지 응답 구조를 명시한다.
- 김지민(admin-web) 전달용 확인 사항과 프런트 반영 포인트를 포함한다.

## 제외 범위

- `qtai-server/**` 코드 변경.
- `admin-web/**` 타입/화면/API client 변경.
- AD-08, Admin AI, 감사 로그 목록 등 인접 관리자 API 계약 정리.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/contracts/2026-06-11_admin-ad0106-response-contract.md` | AD-01/AD-06 전달용 API 계약 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_admin-ad0106-response-contract_report.md` | 작업 결과와 검증 기록 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-11_admin-ad0106-response-contract.md` | 문서 작업 workflow |

## 구현 순서

1. TODO 2와 AD-01/AD-06 report를 확인한다.
2. `qtai-server/apis/api-v1/openapi.yaml`과 DTO 레코드에서 필드/타입/상태 코드를 대조한다.
3. 전달용 contract 문서를 작성한다.
4. 문서 검증을 위해 OpenAPI schema 존재 여부와 대상 파일 목록을 확인한다.
5. report를 작성한다.

## 테스트 보강 목록

문서 전용 작업이므로 테스트 파일 추가는 없다.

## 수용 기준

- [ ] AD-01 응답 필드, 타입, nullable 규칙이 문서화된다.
- [ ] AD-06 목록/생성/수정/발행/숨김 API의 request/response/status code가 문서화된다.
- [ ] 공통 `ApiResponse<T>`와 공지 목록 페이지 봉투가 명시된다.
- [ ] `admin-web`에서 임시 DTO를 교체할 때 필요한 프런트 반영 포인트가 포함된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 문서 산출물 1건과 report/workflow만 작성하는 좁은 작업이다.
- OpenAPI와 DTO 대조 결과를 한 문맥에서 유지하는 편이 정확하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 문서 작성과 검증을 직접 수행한다.

## 검증 계획

```powershell
git diff --check
git status --short
```

필요 시 OpenAPI schema 이름 검색으로 AD-01/AD-06 계약 존재를 확인한다.

## 후속 작업으로 남길 항목

- 김지민의 admin-web 타입/API client 반영 PR에서 실제 화면 렌더링을 공동 확인한다.
