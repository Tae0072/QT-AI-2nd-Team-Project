# 신고 대상 존재 확인 보강 리포트 - 2026-06-11

## 요약

- `COMMENT` 신고 대상 존재 확인을 추가했다.
- 댓글은 삭제되지 않았고 부모 나눔글이 `PUBLISHED`인 경우에만 신고 가능하다.
- `AI_QA_REQUEST` 검증용 RestClient 어댑터를 추가했지만, `service-ai` 조회 API가 아직 미구현이므로 기본값 비활성 설정으로 기존 신고 접수 동작을 유지했다.
- RestClient 어댑터에는 connect 2초/read 3초 타임아웃과 실패 로그를 추가했다.
- 리뷰 보강으로 `CommentRepository.existsReportableComment` JPQL 슬라이스 테스트와 RestClient 예외/401 분기 테스트를 추가했다.
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
- `qtai-server/service-note/src/test/java/com/qtai/domain/sharing/internal/CommentRepositoryTest.java`

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
- 401은 사용자 인증 만료/부재로 보고 `UNAUTHORIZED`로 분리했다.
- RestClient 실패 로그와 cause 보존을 추가했다.
- `ReportService`의 TOCTOU 주석과 신고 접수 설명을 복원했다.
- `ReportServiceTest`는 한국어 테스트명으로 정리했다.
- 댓글 신고 가능 여부에 부모 나눔글 `PUBLISHED` 상태를 포함했다.

## 리뷰 보강 반영

- `CommentRepositoryTest`를 추가해 `existsReportableComment` JPQL 쿼리가 정상 댓글만 true를 반환하고, 삭제 댓글과 부모 글 `HIDDEN`/`DELETED` 댓글은 false를 반환함을 H2 JPA 슬라이스에서 검증했다.
- `CheckAiQaRequestExistsRestClientAdapterTest`에 `SocketTimeoutException` 기반 `RestClientException` 분기 테스트를 추가해 `EXTERNAL_API_FAILURE`와 root cause 보존을 검증했다.
- `CheckAiQaRequestExistsRestClientAdapter`에서 service-ai 401 응답은 `EXTERNAL_API_FAILURE`가 아니라 `UNAUTHORIZED`로 전파하도록 분리했다.
- `ReportService.validateTargetExists`에 default 방어를 추가하고, `COMMENT` 검증의 PUBLISHED 댓글 공개 전제를 주석으로 남겼다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-note:test` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server build` | 성공 |
| `.\gradlew.bat :service-note:test --tests "com.qtai.domain.sharing.internal.CommentRepositoryTest" --tests "com.qtai.domain.report.client.ai.CheckAiQaRequestExistsRestClientAdapterTest" --tests "com.qtai.domain.report.internal.ReportServiceTest"` | 성공 |
| `.\gradlew.bat :service-note:test` | 성공 |

> 리뷰 보강 후 추가 검증은 `qtai-server/` 디렉터리에서 Gradle wrapper를 실행했다. `git diff --check`는 CRLF 변환 경고만 있고 공백 오류는 없었다.

## 미실행 검증

- `npx @stoplight/spectral-cli lint ...`: 이번 변경은 OpenAPI 변경이 없어 실행하지 않았다.
- `gitleaks detect --source . --redact --exit-code 1`: 이번 리뷰 보강은 테스트/서비스 코드와 문서 변경으로 한정되어 실행하지 않았다.

## 수용 기준 확인

- [x] `POST`, `COMMENT` 신고 대상 존재 확인이 `ReportService`에서 수행된다.
- [x] 존재하지 않거나 삭제/비공개 부모글에 속한 댓글은 신고가 저장되지 않는다.
- [x] `AI_QA_REQUEST` 검증은 설정으로 켤 수 있고 기본값은 비활성이다.
- [x] `AI_ASSET`은 enum/API 계약을 유지하되 이번 구현에서 추가 검증하지 않는다.
- [x] 관련 단위 테스트와 보안 통합 테스트가 통과한다.

## 후속 작업

- `service-ai` 사용자 Q&A 조회 API 구현 후 `qtai.report.target-validation.ai-qa-request-enabled=true` 전환을 검토한다.
- `AI_ASSET`이 사용자 신고 대상인지 정책 확정 후 별도 존재 확인 포트를 구현하거나 신고 대상에서 제외한다.
