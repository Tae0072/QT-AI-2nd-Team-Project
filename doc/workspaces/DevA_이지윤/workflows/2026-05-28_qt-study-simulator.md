# Workflow - 2026-05-28 qt-study-simulator

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 협업/리뷰 | 김태혁(Study 도메인 owner), 강상민(AI 산출물/검증 owner), 강태오(Lead) |
| 브랜치 | `feat/qt-study-simulator` |
| PR 대상 | `dev` |
| 관련 F-ID | F-08, F-12, F-02, F-14 |
| 트리거 | Today QT와 Note 기반 흐름이 먼저 고정된 뒤, QT 본문에서 선택 열람하는 해설/용어/요약과 시뮬레이터 조회 API를 실제 승인 콘텐츠 기준으로 연결한다. |
| 기준 문서 | `AGENTS.md`, `CODE_CONVENTION.md`, `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `qtai-server/02_ERD_문서.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/study/**`, `qtai-server/src/main/java/com/qtai/domain/qt/api/**`, `qtai-server/src/main/java/com/qtai/domain/qt/internal/**`, `qtai-server/src/test/java/com/qtai/domain/study/**`, `qtai-server/src/test/java/com/qtai/domain/qt/**`, `qtai-server/src/main/resources/db/migration/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

QT 본문에 연결된 부가 조회 API를 실제 승인 콘텐츠 기준으로 제공한다. `GET /api/v1/qt/{id}/study-content`는 승인된 절별 해설과 용어 풀이만 반환하고, `GET /api/v1/qt/{id}/simulator`는 승인된 시뮬레이터 클립만 사용자에게 노출한다.

사용자 요청 경로에서는 AI 생성, 재생성, 검증을 수행하지 않는다. 해설과 시뮬레이터는 F-02/F-14의 사전 생성·검증 결과를 조회하는 기능이며, 검증 참조 자료 원문, prompt 원문, provider 응답 원문, secret은 응답·로그·fixture에 포함하지 않는다.

## 범위

- `GET /api/v1/qt/{qtPassageId}/study-content` 사용자 API를 구현한다.
- `GET /api/v1/qt/{qtPassageId}/simulator` 사용자 API를 구현하되, 기준 API 문서 v1.7의 `/simulator-clips/{clipId}`와 충돌하는 부분은 아래 계약 결정 지점에 따라 Lead 검토 대상으로 남긴다.
- 두 API 모두 인증된 USER만 접근 가능하다.
- `qtPassageId`가 존재하지 않거나 게시/노출 가능한 QT 본문이 아니면 `QT_PASSAGE_NOT_FOUND` 또는 문서 기준 `404 NOT_FOUND`로 응답한다.
- study-content는 `verse_explanations.status=APPROVED`, `active_unique_key='ACTIVE'`인 해설만 반환한다.
- glossary는 `glossary_terms.status=APPROVED`인 용어만 반환한다.
- simulator는 `simulator_clips.status=APPROVED`인 클립만 반환한다.
- simulator 응답 status는 사용자 실행 가능 상태를 나타내는 `READY`, `MISSING`, `FAILED`, `DISABLED` 중 하나만 사용한다.
- 승인 클립이 있으면 `READY`와 함께 `clipId`, `qtPassageId`, `title`, `componentLibraryVersion`, `sceneScriptJson`, `status=APPROVED`를 반환한다.
- 승인 클립이 없으면 사용자에게 검증 전 콘텐츠를 노출하지 않고 `MISSING`, `FAILED`, `DISABLED` 중 정책에 맞는 상태만 반환한다.
- `domain.study`가 해설, 용어, 시뮬레이터 사용자 조회의 소유 도메인이다. URL prefix가 `/api/v1/qt`여도 Study 도메인 Controller에서 노출할 수 있다.
- Study 도메인은 QT 본문 검증과 절 ID 목록 조회를 위해 `domain.qt.api`의 UseCase/DTO만 호출한다.
- QT 도메인은 Study의 Entity, Repository, Service, web DTO를 직접 import하지 않는다.
- OpenAPI `qtai-server/apis/api-v1/openapi.yaml`에 실제 구현 경로, schema, 실패 응답을 반영한다.
- PR 본문에는 workflow 경로와 report 경로를 남긴다.

## 제외 범위

- Today QT 응답 구조 개편은 제외한다.
- Note 저장, 임시저장, 묵상 달력 흐름과 직접 결합하지 않는다.
- 관리자 AI 산출물 승인/반려, 재생성 트리거, 검증 체크리스트 관리는 제외한다.
- 사용자 요청 시점의 AI 생성, DeepSeek 호출, prompt 조립, 다중 턴 대화, SSE, `/ai/sessions/**`는 추가하지 않는다.
- RAG, Vector DB, Elasticsearch, Kafka, Kubernetes, Helm은 추가하지 않는다.
- 일반 성경 조회 화면과 자유 노트에서 시뮬레이터를 제공하지 않는다.
- 검증 참조 자료 원문과 생성 입력 자료 원문을 사용자 응답에 포함하지 않는다.
- 개역개정, ESV, NIV, 성서유니온, 두란노 본문 텍스트를 seed/test/fixture/OpenAPI 예시에 추가하지 않는다.
- plain secret, token, password, private key 예시는 추가하지 않는다.

## 계약 결정 지점

| 항목 | 결정 |
| --- | --- |
| study-content 경로 | 사용자 요청과 `04_API_명세서.md`가 모두 `GET /api/v1/qt/{qtPassageId}/study-content`를 가리키므로 그대로 구현한다. |
| simulator 경로 | 사용자 요청은 `GET /api/v1/qt/{qtPassageId}/simulator`지만, `04_API_명세서.md` v1.7은 `GET /api/v1/qt/{qtPassageId}/simulator-clips/{clipId}`를 기준으로 둔다. 이번 workflow에는 `/simulator` 구현 계획을 적되, 공개 API 변경 전 Lead 검토 필요로 표시한다. |
| 도메인 소유권 | `03_아키텍처_정의서.md` §3.2, §4.3 기준으로 해설/용어/시뮬레이터 사용자 조회는 `domain.study`가 소유한다. |
| QT 본문 정보 조회 | Study 도메인은 `qt.internal`을 직접 보지 않고 `domain.qt.api`에 공개 UseCase와 DTO를 추가해 `qtPassageId`, verse id 목록, 노출 가능 상태를 받는다. |
| simulator status | Today QT 진입점은 `READY`, `MISSING`, `FAILED`, `DISABLED`를 사용하고, 클립 상세의 저장 상태는 `APPROVED`, `HIDDEN`, `REJECTED`, `PENDING` 저장값과 구분한다. |
| 승인 콘텐츠 필터 | `APPROVED`가 아닌 해설, 용어, 시뮬레이터 클립은 사용자 API에서 빈 목록 또는 준비 안 됨 상태로 처리하고 원문 payload를 반환하지 않는다. |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/qt/api/GetQtPassageContentContextUseCase.java` | Study 도메인이 QT 본문 존재 여부와 절 ID 목록을 조회할 수 있는 공개 UseCase |
| Create | `qtai-server/src/main/java/com/qtai/domain/qt/api/dto/QtPassageContentContext.java` | `qtPassageId`, `qtDate`, `title`, `verseIds`, `published` 같은 도메인 간 DTO |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtService.java` | `GetQtPassageContentContextUseCase` 구현, read-only 트랜잭션, QT 미존재 예외 처리 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java` | QT 본문 존재/노출 가능 상태 조회에 필요한 메서드 |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtPassageVerseRepository.java` | QT 본문에 연결된 `bibleVerseId` 목록을 displayOrder 순서로 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/web/QtStudyContentController.java` | `/api/v1/qt/{qtPassageId}/study-content`, `/api/v1/qt/{qtPassageId}/simulator` 사용자 API |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/api/GetQtStudyContentUseCase.java` | study-content 조회 공개 UseCase |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/api/GetQtSimulatorUseCase.java` | simulator 조회 공개 UseCase |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/api/dto/QtStudyContentResponse.java` | `summary`, `explanations`, `glossaryTerms` 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/api/dto/QtSimulatorResponse.java` | `status`, `clipId`, `qtPassageId`, `title`, `componentLibraryVersion`, `sceneScriptJson` 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/GlossaryTerm.java` | `glossary_terms` Entity |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/GlossaryTermRepository.java` | 승인 용어 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/GlossaryTermStatus.java` | `APPROVED`, `HIDDEN` 상태 enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/SimulatorClip.java` | `simulator_clips` Entity |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/SimulatorClipRepository.java` | 승인 클립 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/SimulatorClipStatus.java` | `PENDING`, `APPROVED`, `REJECTED`, `HIDDEN` 저장 상태 enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/SimulatorComponentLibraryVersion.java` | `simulator_component_library_versions` Entity |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/SimulatorComponentLibraryVersionRepository.java` | 컴포넌트 라이브러리 버전 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/study/internal/VerseExplanationService.java` | 기존 승인 해설 조회를 study-content 조립에 재사용 |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/QtStudyContentService.java` | QT context, 승인 해설, 승인 용어를 조립 |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/internal/QtSimulatorService.java` | 승인 시뮬레이터 클립 조회와 사용자 상태 변환 |
| Create | `qtai-server/src/main/java/com/qtai/domain/study/client/qt/GetQtPassageContentContextUseCaseMock.java` | QT 실제 구현 전 또는 테스트 분리용 임시 어댑터. 통합 후 삭제 표시 포함 |
| Create | `qtai-server/src/main/resources/db/migration/V15__create_study_supplemental_content.sql` | `glossary_terms`, `simulator_component_library_versions`, `simulator_clips` 생성. 이미 존재하면 실제 최신 번호와 분리 migration 사용 |
| Create | `qtai-server/src/test/java/com/qtai/domain/study/web/QtStudyContentControllerTest.java` | API 성공/실패, envelope, 인증, path variable 검증 |
| Create | `qtai-server/src/test/java/com/qtai/domain/study/internal/QtStudyContentServiceTest.java` | 승인 해설/용어만 조립, 미승인 콘텐츠 제외 |
| Create | `qtai-server/src/test/java/com/qtai/domain/study/internal/QtSimulatorServiceTest.java` | APPROVED 클립만 READY로 노출, 미승인/없음 상태 처리 |
| Modify/Create | `qtai-server/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java` | QT context UseCase와 verse id 정렬 조회 검증 |
| Modify | `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | study/qt 도메인 간 `internal`/`web` 직접 import 금지 확인 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | study-content와 simulator 경로, schema, 실패 응답 반영 |

## 구현 순서

1. `04_API_명세서.md` §4.2.5, §4.2.6과 `25_기능_명세서.md` F-08/F-12를 다시 확인한다.
2. `/simulator` 단축 경로를 공개 API로 둘지, 기준 문서의 `/simulator-clips/{clipId}`를 유지할지 Lead 검토 결과를 먼저 기록한다.
3. `qtai-server/apis/api-v1/openapi.yaml`에 현재 QT 경로가 없는 상태를 확인하고, 이번 PR에서 추가할 경로만 좁게 반영한다.
4. `qt.domain.api`에 `GetQtPassageContentContextUseCase`와 `QtPassageContentContext`를 추가한다.
5. `QtService`가 QT 본문 ID로 `qt_passages`와 `qt_passage_verses`를 조회해 verse id 목록을 displayOrder 순서로 반환하게 한다.
6. QT 본문이 없거나 노출 불가 상태이면 `BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND)`로 통일한다.
7. `domain.study.client.qt`에는 통합 전용 Mock이 필요한 경우에만 `GetQtPassageContentContextUseCaseMock`를 만들고, 코드 컨벤션의 통합 후 삭제 표시 규칙을 따른다.
8. 누락된 `glossary_terms`, `simulator_component_library_versions`, `simulator_clips` 테이블 migration을 추가한다. 실제 최신 migration 번호가 `V15`가 아니면 저장소의 마지막 번호 다음으로 조정한다.
9. `GlossaryTerm`, `SimulatorClip`, `SimulatorComponentLibraryVersion` Entity와 Repository를 `domain.study.internal`에 둔다.
10. Entity는 `BaseEntity`를 상속하고 setter를 열지 않으며, 상태값은 enum으로 표현한다.
11. `QtStudyContentService`는 QT context에서 받은 verse id 목록으로 `VerseExplanationService`와 `GlossaryTermRepository`를 조회한다.
12. study-content summary는 승인 해설의 summary를 기준으로 3문장 이내의 안전한 조합값을 반환한다. 별도 summary 저장소가 없다면 빈 문자열이 아니라 `summary=null` 허용 여부를 OpenAPI와 DTO에서 명확히 한다.
13. `QtSimulatorService`는 `qtPassageId` 기준 승인 클립을 조회한다.
14. 승인 클립이 있으면 사용자 상태 `READY`, 저장 상태 `APPROVED`, `sceneScriptJson`, `componentLibraryVersion`을 반환한다.
15. 승인 클립이 없고 생성 실패/비활성 상태를 구분할 저장 근거가 없으면 `MISSING`으로 반환한다. 실패/비활성 상태 저장 근거가 필요하면 Lead 검토 필요로 남기고 임의 컬럼을 추가하지 않는다.
16. `QtStudyContentController`는 `/api/v1/qt/{qtPassageId}/study-content`, `/api/v1/qt/{qtPassageId}/simulator`를 `ApiResponse.success(data)` envelope로 반환한다.
17. Controller는 Repository를 직접 호출하지 않고 UseCase만 호출한다.
18. path variable `qtPassageId`는 1 이상만 허용한다. Bean Validation 적용이 어렵다면 Service에서 `INVALID_INPUT`으로 방어하고 Controller 테스트로 고정한다.
19. 기존 `domain.qt.api.GetQtStudyContentUseCase`, `GetQtSimulatorUseCase`, DTO stub는 사용 여부를 확인한다. Study 도메인으로 소유권을 옮기면 기존 qt stub는 삭제 또는 deprecated 정리 대상이지만, 삭제가 다른 변경과 충돌하면 후속 정리 항목으로 남긴다.
20. OpenAPI에는 실제 endpoint, `bearerAuth`, 성공 schema, `400`, `401`, `403`, `404` 응답을 추가한다.
21. OpenAPI 예시는 실제 성경 본문, 금지 번역본, 검증 참조 자료 원문을 포함하지 않는 더미 문장으로 작성한다.
22. Controller test를 먼저 작성해 endpoint, envelope, 인증 principal, 실패 응답을 고정한다.
23. Service test에서 `APPROVED`와 `active_unique_key='ACTIVE'` 필터, glossary `APPROVED` 필터, simulator `APPROVED` 필터를 검증한다.
24. 미승인 해설, HIDDEN 용어, REJECTED/HIDDEN/PENDING 시뮬레이터 클립이 사용자 응답에 포함되지 않음을 검증한다.
25. `ArchitectureBoundaryTest` 또는 `rg`로 `domain.study`와 `domain.qt`가 서로의 `internal`/`web`을 직접 import하지 않는지 확인한다.
26. 금지 기술과 금지 데이터 문자열을 `rg`로 확인한다.
27. 변경 후 workflow/report 경로를 PR 본문에 적는다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/study/web/QtStudyContentControllerTest.java` | `GET /api/v1/qt/{qtPassageId}/study-content` 200, 공통 envelope, `summary`, `explanations`, `glossaryTerms` 반환 |
| `qtai-server/src/test/java/com/qtai/domain/study/web/QtStudyContentControllerTest.java` | `GET /api/v1/qt/{qtPassageId}/simulator` 200, status 4종 중 하나, READY일 때 clip payload 반환 |
| `qtai-server/src/test/java/com/qtai/domain/study/web/QtStudyContentControllerTest.java` | `qtPassageId < 1` 요청은 `400 INVALID_INPUT` 또는 프로젝트 공통 검증 오류로 응답 |
| `qtai-server/src/test/java/com/qtai/domain/study/web/QtStudyContentControllerTest.java` | 미존재 QT 본문은 `404 QT_PASSAGE_NOT_FOUND`로 응답 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/QtStudyContentServiceTest.java` | `APPROVED` + `ACTIVE` 해설만 반환하고 `PENDING`, `REJECTED`, `HIDDEN` 해설은 제외 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/QtStudyContentServiceTest.java` | glossary는 `APPROVED`만 반환하고 `HIDDEN`은 제외 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/QtStudyContentServiceTest.java` | QT context의 verse id 순서를 유지해 해설과 용어를 반환 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/QtSimulatorServiceTest.java` | `APPROVED` 클립만 `READY`로 노출 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/QtSimulatorServiceTest.java` | 승인 클립 없음은 검증 전 payload 없이 `MISSING`으로 응답 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/QtSimulatorServiceTest.java` | `PENDING`, `REJECTED`, `HIDDEN` 클립은 sceneScriptJson을 노출하지 않음 |
| `qtai-server/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java` | QT 본문 context 조회 시 verse id가 displayOrder 순서로 반환됨 |
| `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | study/qt 도메인이 서로의 `internal`/`web`을 직접 import하지 않음 |

## 수용 기준

- [ ] `GET /api/v1/qt/{qtPassageId}/study-content`가 인증된 사용자에게 공통 envelope로 응답한다.
- [ ] study-content 응답은 `summary`, `explanations`, `glossaryTerms`를 포함한다.
- [ ] 승인되지 않은 해설과 용어는 사용자 응답에 포함되지 않는다.
- [ ] `GET /api/v1/qt/{qtPassageId}/simulator` 경로 적용 여부가 Lead 검토 결과와 일치한다.
- [ ] simulator는 `APPROVED` 클립만 사용자에게 노출한다.
- [ ] simulator 사용자 상태는 `READY`, `MISSING`, `FAILED`, `DISABLED` 중 하나만 반환한다.
- [ ] `READY`가 아닌 응답은 `sceneScriptJson`을 반환하지 않는다.
- [ ] Study 도메인은 QT 도메인의 `api` UseCase/DTO만 사용하고 `qt.internal`, `qt.web`을 import하지 않는다.
- [ ] Controller는 Repository를 직접 호출하지 않는다.
- [ ] 사용자 요청 경로에서 AI 생성, 재생성, DeepSeek 호출을 수행하지 않는다.
- [ ] OpenAPI와 Controller 경로, schema, 실패 응답이 일치한다.
- [ ] 테스트/fixture/OpenAPI 예시에 금지 번역본과 실제 성서유니온/두란노 본문 텍스트가 없다.
- [ ] PR 본문에 workflow/report 경로가 포함된다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- QT context UseCase, Study 조회 구현, DB migration, OpenAPI, 테스트가 서로 다른 경로에 나뉘어 병렬화 이점이 있다.
- 단, `/simulator`와 `/simulator-clips/{clipId}` 경로 결정은 공개 API 계약이므로 메인 에이전트가 Lead 검토 결과를 확인한 뒤 통합해야 한다.
- 승인 콘텐츠 미노출, 금지 데이터 검사, 도메인 경계 검증은 최종 통합 단계에서 한 번 더 직접 확인해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | QT context UseCase와 verse id 조회 구현 | `qtai-server/src/main/java/com/qtai/domain/qt/**`, `qtai-server/src/test/java/com/qtai/domain/qt/**` |
| Worker 2 | Study content와 simulator 조회 구현 | `qtai-server/src/main/java/com/qtai/domain/study/**`, `qtai-server/src/test/java/com/qtai/domain/study/**` |
| Worker 3 | DB migration과 Entity/Repository 정합성 확인 | `qtai-server/src/main/resources/db/migration/**`, `qtai-server/src/main/java/com/qtai/domain/study/internal/**` |
| Worker 4 | OpenAPI와 guardrail 검증 | `qtai-server/apis/api-v1/openapi.yaml`, `qtai-server/src/test/java/com/qtai/common/**` |

### 직접 실행 판단

메인 에이전트는 공개 API 경로 결정, 도메인 경계, 승인 콘텐츠 필터, 금지 기술/데이터 검사, 최종 Gradle 검증을 직접 확인한다.

## 검증 계획

- `git diff --check`
- `.\qtai-server\gradlew.bat test --tests "*QtStudyContentControllerTest"`
- `.\qtai-server\gradlew.bat test --tests "*QtStudyContentServiceTest"`
- `.\qtai-server\gradlew.bat test --tests "*QtSimulatorServiceTest"`
- `.\qtai-server\gradlew.bat test --tests "*QtServiceTest"`
- `.\qtai-server\gradlew.bat test --tests "*ArchitectureBoundaryTest"`
- `.\qtai-server\gradlew.bat test`
- `.\qtai-server\gradlew.bat build`
- `.\qtai-server\gradlew.bat jacocoTestReport`
- `.\qtai-server\gradlew.bat jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "com\\.qtai\\.domain\\.qt\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/study --glob "*.java"`
- `rg -n "com\\.qtai\\.domain\\.study\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/qt --glob "*.java"`
- `rg -n "KafkaTemplate|spring-kafka|@KafkaListener|SseEmitter|text/event-stream|/ai/sessions|VectorStore|EmbeddingStore|Elasticsearch|RAG" qtai-server/src/main qtai-server/src/test`
- `rg -n "개역개정|ESV|NIV|성서유니온|두란노|plain secret|private key|password|token" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml`

`.spectral.yaml`이 없으면 `--ruleset .spectral.yaml` 없이 OpenAPI lint를 실행한다. `jacocoTestReport`, `jacocoTestCoverageVerification`, `spectral`, `gitleaks`가 로컬에서 실행되지 않으면 실패 사유를 report와 PR 본문에 기록하고 CI 결과로 보완한다.

## 후속 작업으로 남길 항목

- 기준 API 문서의 `/simulator-clips/{clipId}`와 사용자 요청의 `/simulator` 경로 정합화
- Today QT 응답의 `simulatorStatus`를 실제 `simulator_clips` 승인 상태와 연결
- 관리자 웹에서 시뮬레이터 클립 승인/숨김 상태를 관리하는 API
- AI 배치가 `simulator_clips`와 `verse_explanations` 노출본을 생성·갱신하는 실제 연동
- glossary 생성·승인 관리 화면과 운영 정책
