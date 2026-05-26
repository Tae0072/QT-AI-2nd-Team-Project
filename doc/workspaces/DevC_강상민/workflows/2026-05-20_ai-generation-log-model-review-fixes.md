# Workflow — 2026-05-20 ai-generation-log-model-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-log-model` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | Claude 자동 코드 리뷰 `REQUEST_CHANGES` |
| 기준 문서 | `07_요구사항_정의서.md` v3.1, `03_아키텍처_정의서.md` §8, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/{workflows,reports}/**` |

## 작업 목표

`ai-generation-log-model` PR의 REQUEST_CHANGES 항목을 머지 가능한 수준으로 정리한다. 핵심은 상태 전이 가드, 검증 로그와 산출물 상태 정합성, 누락 테스트 보강, 문서 표현 정리다.

## 리뷰 판단

| 구분 | 판단 | 대응 |
| --- | --- | --- |
| 상태 전이 가드 누락 | 타당 | 이번 PR에서 수정 |
| `registerValidationLog`의 `PASSED/NEEDS_REVIEW` 처리 불명확 | 타당 | 이번 PR에서 정책 명시 및 구현 |
| public 메서드 테스트 누락 | 타당 | 이번 PR에서 테스트 추가 |
| Entity invariant 테스트 누락 | 타당 | 이번 PR에서 테스트 추가 |
| Repository slice 테스트 누락 | 부분 수용 | Entity 매핑 smoke test를 추가하되, 상세 query는 후속 PR |
| `payloadJson`/`checklistJson` 저장 가드 | 타당 | raw response/reference keyword 차단 수준의 최소 가드 추가 |
| `AiLogService` internal Service 노출 위험 | 타당 | `public` class를 package-private으로 낮춤. api UseCase 분리는 후속 workflow |
| `Clock` 주입 | 보류 | 현재 메서드 시그니처가 명시 시간 주입 방식이고 batch/admin 호출부가 미구현. 후속 생성 workflow에서 결정 |
| 문서의 검증 참조 표현 | 타당 | "검증 참조 원문/자료"로 수정 |

## 범위

- `AiGenerationJob` 상태 전이 검증을 추가한다.
- `AiGeneratedAsset` 상태 전이 검증을 추가한다.
- `AiLogService.registerValidationLog`가 `PASSED`, `NEEDS_REVIEW`, `REJECTED`를 명확히 처리하도록 한다.
- `AiLogService`를 package-private으로 낮춰 외부 도메인 직접 주입 위험을 줄인다.
- 누락된 Service public 메서드와 부정 경로 테스트를 추가한다.
- Entity invariant 테스트를 추가한다.
- 문서의 혼동되는 표현을 정리한다.

## 제외 범위

- `domain.ai.api` UseCase 계약 추가는 `2026-05-20_ai-usecase-contracts.md`에서 처리한다.
- 관리자 승인 API, 배치 생성 API, 실제 DeepSeek 호출은 이번 작업에서 구현하지 않는다.
- `Clock` bean 도입은 batch/admin 호출부가 생기는 후속 작업에서 결정한다.
- 상세 Repository query/projection은 관리자 목록 API 작업에서 구현한다.

## 상태 전이 기준

### `AiGenerationJob`

```text
QUEUED -> RUNNING
RUNNING -> SUCCEEDED
RUNNING -> FAILED
QUEUED -> FAILED
```

- `SUCCEEDED`, `FAILED`는 terminal 상태다.
- `FAILED -> SUCCEEDED`, `SUCCEEDED -> FAILED`, `SUCCEEDED -> RUNNING`은 차단한다.
- invalid transition은 `BusinessException(ErrorCode.INVALID_INPUT, "...")`로 처리한다.

### `AiGeneratedAsset`

```text
VALIDATING -> APPROVED
VALIDATING -> REJECTED
VALIDATING -> HIDDEN
APPROVED -> HIDDEN
```

- `REJECTED`, `HIDDEN`은 terminal 상태다.
- `APPROVED -> REJECTED`, `REJECTED -> APPROVED`, `HIDDEN -> APPROVED`는 차단한다.
- `approve()`는 이번 PR에서 직접 호출하지 않지만, 호출 가능 메서드이므로 `VALIDATING`에서만 허용한다.

### `AiValidationLog.result`

| result | 산출물 상태 처리 |
| --- | --- |
| `PASSED` | 산출물은 `VALIDATING` 유지. 관리자 승인 전 `APPROVED`로 바꾸지 않는다. |
| `NEEDS_REVIEW` | 산출물은 `VALIDATING` 유지. 관리자 검토 대상으로 남긴다. |
| `REJECTED` | 산출물이 `VALIDATING`일 때만 `REJECTED`로 전환한다. |

## 구현 순서

1. `AiGenerationJob` 상태 전이 테스트를 먼저 추가한다.
2. 테스트 실패를 확인한다.
3. `AiGenerationJob`에 terminal 상태와 invalid transition 가드를 구현한다.
4. `AiGeneratedAsset` 상태 전이 테스트를 추가한다.
5. 테스트 실패를 확인한다.
6. `AiGeneratedAsset`에 `approve/reject/hide` 가드를 구현한다.
7. `AiLogServiceTest`에 `markGenerationRunning`, `markGenerationSucceeded`, `findById` 실패 경로 테스트를 추가한다.
8. `registerValidationLog`의 `PASSED`, `NEEDS_REVIEW`, `REJECTED` 정책 테스트를 추가한다.
9. `AiLogService`를 package-private으로 낮추고 테스트 패키지 접근이 유지되는지 확인한다.
10. `payloadJson`, `checklistJson`에 최소 저장 가드를 추가한다.
11. Entity invariant 테스트를 추가한다.
12. 문서와 리포트의 검증 참조 표현을 "검증 참조 원문/자료"로 바꾼다.
13. `gradle clean test --tests "*Ai*"`를 실행한다.
14. `gradle -p qtai-server build`를 실행한다.
15. 수정 커밋을 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `AiLogServiceTest` | `markGenerationRunning`, `markGenerationSucceeded`, job not found, asset not found |
| `AiLogServiceTest` | `PASSED/NEEDS_REVIEW`는 asset을 `APPROVED`로 바꾸지 않음 |
| `AiLogServiceTest` | `REJECTED`는 `VALIDATING` asset만 reject 가능 |
| `AiGenerationJobTest` | `FAILED -> SUCCEEDED` 차단, `SUCCEEDED -> FAILED` 차단 |
| `AiGeneratedAssetTest` | `APPROVED -> REJECTED` 차단, `REJECTED -> APPROVED` 차단, `APPROVED -> HIDDEN` 허용 |
| `AiValidationLogTest` | `layer < 1` 차단, 긴 error message truncate |
| `AiGeneratedAssetTest` | blank `payloadJson`, blank `promptVersion` 차단 |

## 수용 기준

- [ ] `AiGenerationJob`의 terminal 상태 재전이가 차단된다.
- [ ] `AiGeneratedAsset`은 `VALIDATING` 기준 상태 전이만 허용하고, `APPROVED -> HIDDEN`만 예외로 허용한다.
- [ ] `registerValidationLog(PASSED)`는 산출물을 자동 승인하지 않는다.
- [ ] `registerValidationLog(NEEDS_REVIEW)`는 산출물을 검토 대기로 유지한다.
- [ ] `registerValidationLog(REJECTED)`는 `VALIDATING` 산출물만 반려한다.
- [ ] `AiLogService`의 모든 public 메서드와 not found 경로가 테스트된다.
- [ ] Entity invariant 테스트가 추가된다.
- [ ] `payloadJson`/`checklistJson` 저장 시 provider raw response 또는 검증 참조 원문을 넣지 않도록 최소 가드가 있다.
- [ ] 문서 표현은 "검증 참조 원문/자료"로 통일된다.
- [ ] `gradle clean test --tests "*Ai*"`가 통과한다.
- [ ] `gradle -p qtai-server build`가 통과한다.

## PR 응답 요약 초안

```markdown
REQUEST_CHANGES 항목을 반영했습니다.

- AI 생성 작업과 산출물 상태 전이에 terminal state 가드를 추가했습니다.
- 검증 로그 등록 시 `PASSED/NEEDS_REVIEW/REJECTED`별 산출물 상태 정책을 명확히 했습니다.
- 누락된 Service public 메서드, not found 경로, Entity invariant 테스트를 추가했습니다.
- `AiLogService`를 package-private으로 낮춰 internal Service 직접 주입 위험을 줄였습니다.
- 문서의 검증 참조 표현을 "검증 참조 원문/자료"로 정리했습니다.

검증:
- `gradle clean test --tests "*Ai*"`
- `gradle -p qtai-server build`
```

## 후속 작업으로 남길 항목

- `domain.ai.api` UseCase 계약 정리
- 관리자 승인 API에서 checklist version 검증 후 `approve()` 호출
- 배치/admin 생성 경로에서 `Clock` 또는 KST 시간 정책 결정
- Repository projection 기반 관리자 목록 조회
- 감사 로그와 `SYSTEM_BATCH` 주체 기록 연결
