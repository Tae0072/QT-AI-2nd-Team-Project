# 2026-05-27 meditation-event-processing 구현 리포트

## 기준

- workflow: `doc/workspaces/DevA_이지윤/workflows/2026-05-27_meditation-event-processing.md`
- 관련 F-ID: F-03, F-13
- 실행 방식: 직접 실행

## 구현 결과

- `JournalEvent` 임시 스켈레톤을 `journal_events` JPA Entity로 전환했다.
- `JournalEventType`, `JournalChangedEvent`, `JournalEventRepository`, `JournalEventHandler`를 추가했다.
- `NoteService.create/update/delete`에서 MEDITATION 노트의 저장 확정, 저장 취소, 삭제 이벤트를 `ApplicationEventPublisher`로 발행한다.
- 이벤트 핸들러는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 동작하며, 동일 `eventId`는 멱등 성공 처리한다.
- `GET /api/v1/me/meditation-calendar`를 `note.api`의 `GetMeditationCalendarUseCase`로 위임하도록 연결했다.
- 묵상 달력은 `notes.status=SAVED`, `notes.deleted_at IS NULL`, `notes.saved_at` 범위 기준으로 월별 완료 일자를 조회한다.
- `qtai-server/apis/api-v1/openapi.yaml`에 묵상 달력 path와 schema를 추가했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 통과 |
| `.\gradlew.bat test --tests "*NoteServiceTest"` | 통과 |
| `.\gradlew.bat test --tests "*JournalEventHandlerTest"` | 통과 |
| `.\gradlew.bat test --tests "*NoteRepositoryIntegrationTest"` | 통과 |
| `.\gradlew.bat test --tests "*MyPageControllerTest"` | 통과 |
| `.\gradlew.bat test --tests "*JpaEntityDdlTest"` | 통과 |
| `.\gradlew.bat test --tests "*ArchitectureBoundaryTest"` | 통과 |
| `.\gradlew.bat test --tests "*Note*" --tests "*MyPageControllerTest" --tests "*ArchitectureBoundaryTest"` | 통과 |
| `.\gradlew.bat build` | 통과. 샌드박스 안에서는 Gradle wrapper lock 접근 거부가 발생해 승인 후 샌드박스 밖에서 재실행했다. |
| `.\gradlew.bat test jacocoTestReport` | 실패: `jacocoTestReport` 태스크가 등록되어 있지 않음 |
| `.\gradlew.bat jacocoTestCoverageVerification` | 실패: `jacocoTestCoverageVerification` 태스크가 등록되어 있지 않음 |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | 실패: 저장소 루트에 `.spectral.yaml` 없음 |
| `gitleaks detect --source . --redact --exit-code 1` | 실패: 로컬에 `gitleaks` 명령 없음 |
| `rg -n "KafkaTemplate\|spring-kafka\|@KafkaListener\|SseEmitter\|text/event-stream\|VectorStore\|EmbeddingStore\|javax\\." qtai-server/src/main qtai-server/src/test` | 통과: 매칭 없음 |
| `rg -n "개역개정\|\\bESV\\b\|\\bNIV\\b\|plain secret\|password\|private key" ...` | 기존 설정 파일의 `password` 키와 workflow 자체의 금지 문구가 매칭됨. 이번 구현 파일 및 OpenAPI 예시에는 금지 본문/번역 fixture를 추가하지 않음 |

## 수용 기준 점검

- `JournalEvent` Entity, Repository, migration 추가: 충족
- MEDITATION 저장 확정·저장 취소·삭제 이벤트 기록: 충족
- 단순 DRAFT 생성·수정 이벤트 제외: 충족
- `ApplicationEventPublisher`와 AFTER_COMMIT 핸들러 사용: 충족
- Kafka, SSE, 외부 메시지 브로커 미추가: 충족
- 실패 로그 필드 제한: 충족
- 달력 API가 `notes` 기준으로 월별 완료 상태 반환: 충족
- 삭제 또는 `SAVED -> DRAFT` 후 달력 완료 제외: 테스트로 확인
- 기록 없는 날짜 조회 시 자동 생성 없음: 조회 쿼리만 사용
- Controller가 Repository 직접 호출하지 않음: 충족
- `member.web -> note.api` 경계 유지: Arch test로 확인

## 후속 확인

- Jacoco, Spectral, gitleaks는 로컬 도구/태스크 구성이 없어 CI 또는 환경 구성 후 재확인이 필요하다.
- PR 본문에는 `F-03`, `F-13`, workflow 경로, 이 report 경로를 포함해야 한다.
