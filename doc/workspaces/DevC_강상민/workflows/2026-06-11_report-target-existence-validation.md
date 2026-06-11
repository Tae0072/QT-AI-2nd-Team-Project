# Workflow - 2026-06-11 report-target-existence-validation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/report-target-validation` |
| PR 대상 | `dev` |
| 관련 F-ID | F-10, F-15 |
| 트리거 | `ReportService`에서 신고 대상 존재 확인이 `POST`에만 연결되어 있고 `COMMENT`, `AI_QA_REQUEST`, `AI_ASSET`은 후속 구현으로 남아 있음 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md` |
| 해당 경로 | `qtai-server/service-note/**`, `qtai-server/lib-common/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

사용자 신고 접수 시 `COMMENT` 대상의 존재 여부와 신고 가능 여부를 확인한다. `AI_QA_REQUEST`는 `service-ai` 사용자 조회 API가 아직 구현되지 않았으므로 기본값 비활성 피처 플래그로 기존 접수 동작을 유지하고, API 준비 후 설정으로 검증을 켤 수 있게 한다.

## 범위

- `COMMENT` 신고는 `sharing.api` 공개 UseCase를 통해 삭제되지 않은 댓글이면서 부모 나눔글이 `PUBLISHED`인 경우만 허용한다.
- `AI_QA_REQUEST` 신고 검증용 client 포트와 RestClient 어댑터를 추가하되, `qtai.report.target-validation.ai-qa-request-enabled=false` 기본값으로 기존 동작을 유지한다.
- `AI_QA_REQUEST` 검증이 활성화되면 403/404는 대상 없음으로 보고, 5xx/통신 실패는 외부 API 실패로 처리한다.
- 외부 RestClient 호출에는 connect 2초/read 3초 타임아웃을 명시한다.
- 관련 단위 테스트와 보안 통합 테스트를 보강한다.

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
| Create | `qtai-server/service-note/src/main/java/com/qtai/domain/report/internal/ReportTargetValidationProperties.java` | AI Q&A 대상 검증 활성화 설정 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/internal/CommentService.java` | `CheckCommentExistsUseCase` 구현 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/internal/CommentRepository.java` | 댓글과 부모 나눔글 상태를 함께 확인하는 쿼리 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/report/internal/ReportService.java` | 대상별 검증 연결 및 AI Q&A 검증 플래그 적용 |
| Modify | `qtai-server/lib-common/src/main/java/com/qtai/common/exception/BusinessException.java` | 외부 호출 실패 cause 보존 생성자 |
| Test | `qtai-server/service-note/src/test/java/com/qtai/domain/report/internal/ReportServiceTest.java` | 대상별 존재 확인, AI Q&A 검증 on/off, 중복 신고 검증 |
| Test | `qtai-server/service-note/src/test/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsRestClientAdapterTest.java` | AI Q&A client 오류 매핑과 Authorization 전달 검증 |

## 구현 순서

1. workflow 문서를 저장하고 `feature/report-target-validation` 브랜치에서 작업한다.
2. `ReportServiceTest`에 `COMMENT`, `AI_QA_REQUEST` 검증 on/off 케이스를 추가한다.
3. `CheckCommentExistsUseCase`와 repository 쿼리를 추가하고 `CommentService`가 구현하게 한다.
4. `CheckAiQaRequestExistsClient`와 RestClient 어댑터를 추가한다.
5. AI Q&A 검증 기본값을 비활성으로 두는 설정을 추가해 `service-ai` endpoint 미구현 상태의 회귀를 막는다.
6. adapter 단위 테스트를 추가해 2xx/403/404/5xx/Authorization 전달을 확인한다.
7. 검증 명령을 실행하고 결과를 report 문서에 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `ReportServiceTest` | 삭제/미존재 댓글이면 저장하지 않고 `REPORT_TARGET_NOT_FOUND` |
| `ReportServiceTest` | AI Q&A 검증 활성화 상태에서 대상 없음이면 `REPORT_TARGET_NOT_FOUND` |
| `ReportServiceTest` | AI Q&A 검증 비활성 상태에서는 기존처럼 존재 확인 없이 접수 |
| `ReportServiceTest` | 정상 `COMMENT`, `AI_QA_REQUEST` 신고는 `RECEIVED`로 저장 |
| `CheckAiQaRequestExistsRestClientAdapterTest` | service-ai 404/403 응답은 `false` 반환 |
| `CheckAiQaRequestExistsRestClientAdapterTest` | service-ai 5xx/RestClient 오류는 `EXTERNAL_API_FAILURE` |

## 수용 기준

- [ ] `POST`, `COMMENT` 신고 대상 존재 확인이 `ReportService`에서 수행된다.
- [ ] 존재하지 않거나 삭제/비공개 부모글에 속한 댓글은 신고가 저장되지 않는다.
- [ ] `AI_QA_REQUEST` 검증은 설정으로 켤 수 있지만 기본값은 비활성이다.
- [ ] `AI_ASSET`은 enum/API 계약을 유지하되 이번 구현에서 추가 검증하지 않는다.
- [ ] 관련 단위 테스트가 추가되고 실행 결과가 report 문서에 기록된다.

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
- `service-ai`의 실제 `/api/v1/ai/qa-requests/{requestId}` 구현이 완료되면 `qtai.report.target-validation.ai-qa-request-enabled=true` 전환과 통합 테스트 보강을 별도 작업으로 진행한다.
