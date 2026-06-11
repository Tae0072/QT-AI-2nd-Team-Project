# Workflow - 2026-06-11 report-target-existence-validation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/report-target-validation` |
| PR 대상 | `dev` |
| 관련 F-ID | F-10, F-15 |
| 트리거 | `ReportService`에서 `COMMENT`, `AI_QA_REQUEST`, `AI_ASSET` 대상 존재 확인이 후속 구현으로 남아 있음 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md` |
| 해당 경로 | `qtai-server/service-note/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

사용자 신고 접수 시 `POST` 외에도 `COMMENT`, `AI_QA_REQUEST` 대상의 존재 여부와 신고 가능 여부를 확인한다. 존재하지 않거나 삭제/접근 불가한 대상은 신고 row를 만들지 않고 `REPORT_TARGET_NOT_FOUND`로 응답하게 한다.

## 범위

- `COMMENT` 신고는 `sharing.api` 공개 UseCase를 통해 삭제되지 않은 댓글만 허용한다.
- `AI_QA_REQUEST` 신고는 `service-note`의 `ai` 클라이언트 포트를 통해 현재 사용자에게 조회 가능한 Q&A 요청만 허용한다.
- `ReportService`는 대상별 원본 예외를 `REPORT_TARGET_NOT_FOUND`로 통일한다.
- 관련 단위 테스트를 보강한다.

## 제외 범위

- `AI_ASSET` 사용자용 존재 확인 포트 구현
- 신고 가능 `targetType` enum 또는 OpenAPI 계약 변경
- 관리자 신고 처리의 대상 숨김 연계 확장
- `service-ai`의 실제 사용자 Q&A 컨트롤러 구현
- DB 마이그레이션 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/api/CheckCommentExistsUseCase.java` | 댓글 신고 가능 여부 확인 공개 포트 |
| Create | `qtai-server/service-note/src/main/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsClient.java` | 신고 도메인에서 사용할 AI Q&A 존재 확인 포트 |
| Create | `qtai-server/service-note/src/main/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsRestClientAdapter.java` | `service-ai` Q&A 조회 endpoint 호출 어댑터 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/internal/CommentService.java` | `CheckCommentExistsUseCase` 구현 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/internal/CommentRepository.java` | 삭제되지 않은 댓글 존재 확인 쿼리 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/report/internal/ReportService.java` | `COMMENT`, `AI_QA_REQUEST` 대상 검증 연결 |
| Test | `qtai-server/service-note/src/test/java/com/qtai/domain/report/internal/ReportServiceTest.java` | 대상별 존재 확인 및 예외 매핑 검증 |
| Test | `qtai-server/service-note/src/test/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsRestClientAdapterTest.java` | AI Q&A 클라이언트 오류 매핑 검증 |

## 구현 순서

1. workflow 문서를 저장하고 `feature/report-target-validation` 브랜치에서 작업한다.
2. `ReportServiceTest`에 `COMMENT`, `AI_QA_REQUEST` 존재/미존재 검증 케이스를 먼저 추가한다.
3. `CheckCommentExistsUseCase`와 repository 쿼리를 추가하고 `CommentService`가 구현하게 한다.
4. `CheckAiQaRequestExistsClient`와 RestClient 어댑터를 추가한다.
5. `ReportService` 생성자 의존성과 `validateTargetExists` 분기를 갱신한다.
6. adapter 단위 테스트를 추가해 404/403은 `REPORT_TARGET_NOT_FOUND`, 기타 오류는 `EXTERNAL_API_FAILURE`로 매핑되는지 확인한다.
7. 검증 명령을 실행하고 결과를 report 및 커밋 전에 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `ReportServiceTest` | 삭제/미존재 댓글이면 저장하지 않고 `REPORT_TARGET_NOT_FOUND` |
| `ReportServiceTest` | 접근 불가/미존재 AI Q&A 요청이면 저장하지 않고 `REPORT_TARGET_NOT_FOUND` |
| `ReportServiceTest` | 정상 `COMMENT`, `AI_QA_REQUEST` 신고는 `RECEIVED`로 저장 |
| `ReportServiceTest` | 중복 신고는 기존처럼 `DUPLICATE_REPORT` |
| `CheckAiQaRequestExistsRestClientAdapterTest` | service-ai 404/403 응답을 `REPORT_TARGET_NOT_FOUND`로 매핑 |
| `CheckAiQaRequestExistsRestClientAdapterTest` | service-ai 5xx/RestClient 오류를 `EXTERNAL_API_FAILURE`로 매핑 |

## 수용 기준

- [ ] `POST`, `COMMENT`, `AI_QA_REQUEST` 신고 대상 존재 확인이 `ReportService`에서 수행된다.
- [ ] 존재하지 않거나 삭제/접근 불가한 대상은 신고가 저장되지 않는다.
- [ ] 대상 없음 오류는 외부로 `REPORT_TARGET_NOT_FOUND`로 통일된다.
- [ ] `AI_ASSET`은 enum/API 계약을 유지하되 이번 구현에서 추가 검증하지 않는다.
- [ ] 관련 단위 테스트가 추가되고 실행 결과가 기록된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경이 `ReportService` 검증 흐름과 테스트에 강하게 연결되어 있어 한 맥락에서 순차 확인하는 편이 안전하다.
- `service-note` 내부 포트와 RestClient 어댑터가 같은 수용 기준을 공유하므로 병렬 편집 이점이 작다.
- 최종 검증과 커밋 범위 판단을 메인 에이전트가 직접 수행하는 편이 충돌 가능성을 줄인다.

### 위임 가능 작업

| Worker | 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 직접 실행 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 테스트, 커밋을 직접 수행한다.

## 검증 계획

```powershell
git diff --check
.\qtai-server\gradlew.bat -p qtai-server :service-note:test
.\qtai-server\gradlew.bat -p qtai-server build
```

전체 품질 게이트를 실행할 수 없는 경우 최종 리포트에 사유와 대체 검증을 기록한다.

## 후속 작업으로 남길 항목

- `AI_ASSET`이 사용자 신고 대상인지 정책 확정 후 사용자용 존재 확인 포트 또는 신고 대상 제외를 별도 작업으로 결정한다.
- `service-ai`의 실제 `/api/v1/ai/qa-requests/{requestId}` 구현 상태가 확정되면 RestClient 어댑터 계약을 통합 테스트로 보강한다.
