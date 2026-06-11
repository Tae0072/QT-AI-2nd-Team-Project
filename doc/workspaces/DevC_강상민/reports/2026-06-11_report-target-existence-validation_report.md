# 신고 대상 존재 확인 보강 리포트 - 2026-06-11

## 요약

- `COMMENT` 신고 대상 존재 확인을 추가했다.
- 댓글은 삭제되지 않았고 부모 나눔글이 `PUBLISHED`인 경우에만 신고 가능하다.
- `AI_QA_REQUEST` 검증용 RestClient 어댑터를 추가했지만, `service-ai` 조회 API가 아직 미구현이므로 기본값 비활성 설정으로 기존 신고 접수 동작을 유지했다.
- RestClient 어댑터에는 connect 2초/read 3초 타임아웃과 실패 로그를 추가했다.
- `AI_ASSET` 존재 확인은 사용자 신고 정책 확정 후 후속 작업으로 남겼다.

## 변경 파일

### 생성

- `doc/workspaces/DevC_강상민/workflows/2026-06-11_report-target-existence-validation.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-11_report-target-existence-validation_report.md`
- `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/api/CheckCommentExistsUseCase.java`
- `qtai-server/service-note/src/main/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsClient.java`
- `qtai-server/service-note/src/main/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsRestClientAdapter.java`
- `qtai-server/service-note/src/main/java/com/qtai/domain/report/internal/ReportTargetValidationProperties.java`
- `qtai-server/service-note/src/test/java/com/qtai/domain/report/client/ai/CheckAiQaRequestExistsRestClientAdapterTest.java`

### 수정

- `qtai-server/lib-common/src/main/java/com/qtai/common/exception/BusinessException.java`
- `qtai-server/service-note/src/main/java/com/qtai/domain/report/internal/ReportService.java`
- `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/internal/CommentRepository.java`
- `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/internal/CommentService.java`
- `qtai-server/service-note/src/test/java/com/qtai/domain/report/internal/ReportServiceTest.java`
- `qtai-server/service-note/src/test/java/com/qtai/note/NoteApiSecurityIntegrationTest.java`

## 리뷰 대응

- `service-ai`의 `GET /api/v1/ai/qa-requests/{requestId}` 미구현 상태를 고려해 `AI_QA_REQUEST` 존재 확인은 기본 비활성으로 변경했다.
- `qtai.report.target-validation.ai-qa-request-enabled=true` 설정 시에만 `service-ai` 조회를 수행한다.
- AI Q&A RestClient 어댑터에 timeout을 명시했다.
- 403/404는 `exists=false`, 5xx/RestClient 실패는 `EXTERNAL_API_FAILURE`로 분리했다.
- RestClient 실패 로그와 cause 보존을 추가했다.
- `ReportService`의 TOCTOU 주석과 신고 접수 설명을 복원했다.
- `ReportServiceTest`는 한국어 테스트명으로 정리했다.
- 댓글 신고 가능 여부에 부모 나눔글 `PUBLISHED` 상태를 포함했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-note:test` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server build` | 성공 |

## 수용 기준 확인

- [x] `POST`, `COMMENT` 신고 대상 존재 확인이 `ReportService`에서 수행된다.
- [x] 존재하지 않거나 삭제/비공개 부모글에 속한 댓글은 신고가 저장되지 않는다.
- [x] `AI_QA_REQUEST` 검증은 설정으로 켤 수 있고 기본값은 비활성이다.
- [x] `AI_ASSET`은 enum/API 계약을 유지하되 이번 구현에서 추가 검증하지 않는다.
- [x] 관련 단위 테스트와 보안 통합 테스트가 통과한다.

## 후속 작업

- `service-ai` 사용자 Q&A 조회 API 구현 후 `qtai.report.target-validation.ai-qa-request-enabled=true` 전환을 검토한다.
- `AI_ASSET`이 사용자 신고 대상인지 정책 확정 후 별도 존재 확인 포트를 구현하거나 신고 대상에서 제외한다.
