# Report — 2026-05-20 ai-generation-log-model-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-log-model` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model-review-fixes.md` |
| 관련 F-ID | F-02, F-14 |

## 작업 결과

`ai-generation-log-model` PR의 review 요청 사항 중 상태 전이 가드, 검증 로그 상태 정책, 누락 테스트, 저장 JSON 최소 가드, 문서 표현을 반영했다.

## 변경 요약

- `AiGenerationJob`은 `QUEUED -> RUNNING`, `RUNNING -> SUCCEEDED`, `RUNNING/QUEUED -> FAILED`만 허용하고 terminal 상태 전이를 `INVALID_INPUT`으로 차단한다.
- `AiGeneratedAsset`은 `VALIDATING` 기준 승인/반려/숨김과 `APPROVED -> HIDDEN`만 허용한다.
- `AiLogService.registerValidationLog`는 `PASSED`, `NEEDS_REVIEW`에서 산출물을 자동 승인하지 않고, `REJECTED`는 `VALIDATING` 산출물만 반려한다.
- `payloadJson`, `checklistJson`에 provider raw response 또는 검증 참조 원문 필드가 저장되지 않도록 최소 키워드 가드를 추가했다.
- `AiLogService`를 package-private으로 낮춰 내부 서비스 직접 주입 표면을 줄였다.
- DevC workflow 문서의 검증 참조 관련 표현을 "검증 참조 원문/자료"로 정리했다.

## 테스트 보강

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AiGenerationJobTest` | terminal 상태 전이 차단, `QUEUED -> FAILED` 허용 |
| `AiGeneratedAssetTest` | 승인/반려/숨김 상태 전이, blank 필드, raw response 저장 차단 |
| `AiValidationLogTest` | layer 불변식, 긴 error message truncate, checklist raw/reference 저장 차단 |
| `AiLogServiceTest` | not found 경로, `PASSED/NEEDS_REVIEW/REJECTED` 상태 정책, package-private 검증 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle clean test --tests "*Ai*"` (`qtai-server` 디렉터리) | 통과 |
| `gradle --project-dir qtai-server build` | 통과 |
| DevC workflow/report 금지 표현 검색 | 매치 없음 |
| `git diff --check` | 공백 오류 없음. CRLF 변환 경고만 출력됨 |

## 남은 후속 작업

- `domain.ai.api` UseCase 계약 정리
- 관리자 승인 API에서 checklist version 검증 후 `approve()` 호출
- batch/admin 생성 경로의 시간 주입 정책 결정
- Repository projection 기반 관리자 목록 조회
