# Workflow - 2026-05-26 qt-integrated-api-mock (수정)

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/qt-integrated-api-mock` |
| PR 대상 | `dev` |
| 관련 F-ID | F-01, F-03, F-08, F-12, F-15 |
| 트리거 | Flutter 앱의 오늘 QT 화면 연동을 먼저 진행할 수 있도록 QT 조회, 학습 콘텐츠, 시뮬레이터 진입점의 통합 목업 API 계약과 구현 순서를 고정한다. |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/qt/**`, `qtai-server/src/main/java/com/qtai/domain/study/**`, `qtai-server/src/test/java/com/qtai/domain/qt/**`, `qtai-server/src/test/java/com/qtai/domain/study/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

오늘 QT 화면에서 앱이 한 번에 확인해야 하는 본문 메타데이터, 학습 콘텐츠 진입 가능 여부, 묵상 노트 진입점, 시뮬레이터 상태, AI 질문 진입 여부를 서버 목업 API로 제공한다. 실제 Bible/Study/AI/Note 도메인 통합 전에도 Flutter가 안정적으로 붙을 수 있는 고정 응답과 계약 테스트를 목표로 한다.

목업은 임시 구현이지만 API 경로, 응답 필드명, 공통 envelope, 인증 전제, 금지 데이터 정책은 실제 구현과 동일하게 맞춘다. 실제 성경 본문, 금지 번역본, 성서유니온/두란노 본문, 프롬프트 원문, provider 응답 원문은 seed, fixture, response 예시 어디에도 넣지 않는다.

## 범위

- `domain.qt`는 `GET /api/v1/qt/today` 목업 응답을 구현한다.
- `domain.qt`는 `GET /api/v1/qt/{qtPassageId}` 목업 응답을 구현한다.
- `domain.study`는 `GET /api/v1/qt/{qtPassageId}/study-content` 목업 응답을 구현한다.
- `domain.study`는 `GET /api/v1/qt/{qtPassageId}/simulator-clips/{clipId}` 목업 응답을 구현한다.
- 응답은 `ApiResponse.success(data)` envelope를 사용한다.
- 인증된 USER 기준 접근을 전제로 하고, `/api/v1/qt/**`에 `permitAll`을 추가하지 않는다.
- Today QT 응답의 `availableActions.simulatorStatus`는 `READY`, `MISSING`, `FAILED`, `DISABLED` 중 하나로 반환한다.
- v1.7 deprecation 정책에 맞춰 Today QT 응답에는 `simulatorStatus`를 우선 필드로 둔다. Flutter 호환 필요 시 구 boolean `simulator`는 목업 응답에 한시 포함할 수 있지만, 제거 예정 필드임을 테스트명이나 주석에 남긴다.
- F-15는 Today 응답의 `availableActions.aiQuestion` 진입 가능 여부 표시만 포함한다. 실제 Q&A 요청/응답 구현은 `/api/v1/ai/qa-requests` 계열 작업으로 분리한다.
- 목업 데이터는 실제 구절 본문이 아닌 `본문 예시 문장 A`, `본문 예시 문장 B` 같은 더미 문자열만 사용한다.
- OpenAPI에는 목업 구현과 동일한 경로, 파라미터, 응답 schema, 오류 응답을 반영한다.

## 제외 범위

- 실제 QT 관리자 등록, 수정, 게시, 숨김 API 구현은 제외한다.
- `qt_passages`, `qt_passage_verses` DB 조회 기반 실제 조립은 제외한다.
- Bible 도메인의 실제 본문 조회, Study 도메인의 승인 해설 조회, Note 도메인의 DRAFT 조회, AI 도메인의 Q&A 호출은 제외한다.
- `GET /api/v1/bible/**` 구현이나 성경 번들 다운로드 API 구현은 제외한다.
- F-15 실제 Q&A 요청 생성, 결과 조회, DeepSeek 호출, prompt/provider 원문 저장은 제외한다.
- 실제 시뮬레이터 scene script JSON 생성과 검수 상태 전환은 제외한다.
- 관리자 웹 `/api/v1/admin/**` 경로는 제외한다.
- 개역개정, ESV, NIV, 성서유니온, 두란노 본문 텍스트를 seed/test/fixture/response에 추가하지 않는다.

## 계약 결정 지점

| 항목 | 결정 |
| --- | --- |
| Today QT 경로 | `04_API_명세서.md` 기준 `GET /api/v1/qt/today`를 사용하고 `domain.qt`가 소유한다. |
| QT 상세 경로 | `GET /api/v1/qt/{qtPassageId}`를 사용하고 `domain.qt`가 소유한다. |
| 학습 콘텐츠 경로 | `GET /api/v1/qt/{qtPassageId}/study-content`를 사용하고 `domain.study`가 소유한다. |
| 시뮬레이터 경로 | `GET /api/v1/qt/{qtPassageId}/simulator-clips/{clipId}`를 사용하고 `domain.study`가 소유한다. 현재 코드 주석의 `/simulator` 단축 경로는 Lead 승인 전까지 새 공개 API로 만들지 않는다. |
| QT와 Study 연결 | `domain.qt`는 해설/시뮬레이터 세부 로직을 직접 구현하지 않는다. 필요하면 `domain.study.api` UseCase 또는 `domain.qt.client.study.*UseCaseMock`으로 진입 가능 여부만 조회한다. |
| 실제 통합 전환 | Bible/Study/Note/AI UseCase가 준비되면 호출자 도메인의 `client/{타도메인}/...UseCaseMock` 의존을 제거하고 상대 도메인의 실제 `api/UseCase` 구현으로 교체한다. |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/web/QtController.java` | `/today`, `/{qtPassageId}` endpoint를 노출하고 `ApiResponse` envelope를 반환한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/internal/QtService.java` | Today/Detail read-only 목업 응답을 조립하고 QT 조회 UseCase를 구현한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/api/GetTodayQtUseCase.java` | `getToday(memberId, date)` 형태의 계약을 확정한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java` | `getQt(memberId, qtPassageId)` 형태의 계약을 확정한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/qt/api/dto/**` | Today/Detail 목업 응답 DTO를 실제 필드로 채운다. |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/qt/client/study/**` | 통합 전 QT가 Study 진입 가능 여부를 확인해야 할 경우 호출자 쪽 Mock adapter를 둔다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/study/web/StudyController.java` 또는 전용 QT Study Controller | `/api/v1/qt/{qtPassageId}/study-content`, `/api/v1/qt/{qtPassageId}/simulator-clips/{clipId}` endpoint를 노출한다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/study/internal/VerseExplanationService.java` 또는 전용 Study Service | 학습 콘텐츠/시뮬레이터 read-only 목업 응답을 조립한다. |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/study/api/**` | 다른 도메인이 필요한 경우 사용할 Study 공개 UseCase와 DTO를 둔다. |
| Create | `qtai-server/src/test/java/com/qtai/domain/qt/web/QtControllerTest.java` | Today/Detail endpoint, envelope, 인증 전제, 목업 응답 필드명을 검증한다. |
| Create | `qtai-server/src/test/java/com/qtai/domain/study/web/StudyQtContentControllerTest.java` | study-content/simulator-clips endpoint, envelope, 인증 전제, 목업 응답 필드명을 검증한다. |
| Create | `qtai-server/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java` | Today/Detail 목업 조립 규칙과 금지 데이터 미포함을 검증한다. |
| Create | `qtai-server/src/test/java/com/qtai/domain/study/internal/StudyQtContentServiceTest.java` | 학습 콘텐츠/시뮬레이터 목업 조립 규칙과 금지 데이터 미포함을 검증한다. |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | QT 조회 및 Study 소유 QT 하위 경로의 목업 API 계약을 반영한다. |

## 구현 순서

1. `04_API_명세서.md` §4.2.3~§4.2.6과 `25_기능_명세서.md` F-01/F-08/F-12/F-15를 다시 확인한다.
2. `03_아키텍처_정의서.md`의 도메인 소유권을 기준으로 `/today`, `/{qtPassageId}`는 `domain.qt`, `/study-content`, `/simulator-clips/{clipId}`는 `domain.study`에 배치한다.
3. `QtController`, `QtService`, `domain.qt.api`, `domain.qt.api.dto`의 현재 미구현 주석과 실제 컴파일 상태를 확인한다.
4. Study 쪽에 기존 Controller/Service가 있으면 그 관례를 따르고, 없으면 `domain.study.web` 아래에 QT 학습 콘텐츠 조회용 Controller를 둔다.
5. `TodayQtResponse` DTO를 Today 화면이 필요한 필드로 확정한다: `qtPassageId`, `qtDate`, `title`, `rangeLabel`, `status`, `verses`, `myMeditationNote`, `availableActions`, `cache`.
6. `availableActions`에는 `studyContent`, `aiQuestion`, `tts`, `simulatorStatus`, deprecation 기간용 `simulator`를 포함한다.
7. `aiQuestion`은 F-15 Q&A API 호출 결과가 아니라 앱 버튼 진입 가능 여부만 뜻하도록 주석과 테스트명을 명확히 한다.
8. `QtDetailResponse` 또는 기존 `QtResponse`를 API 명세의 QT 상세 응답 구조에 맞게 정리한다.
9. `domain.study`의 `QtStudyContentResponse`에 `summary`, `explanations`, `glossaryTerms`를 포함하되 모든 문자열은 더미 문구로 둔다.
10. `domain.study`의 `QtSimulatorClipResponse`에는 `clipId`, `qtPassageId`, `title`, `componentLibraryVersion`, `sceneScriptJson`, `status`를 반환한다.
11. `QtService`와 Study read service에 `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`를 적용하고 read path만 구현한다.
12. Controller는 인증 Principal 추출, 요청 검증, UseCase/Service 호출, Response 매핑만 수행한다.
13. `qtPassageId` 또는 `clipId`가 1 미만이면 `400 VALIDATION_ERROR`로 거절한다.
14. 목업에서 지원하지 않는 `qtPassageId`는 `404 NOT_FOUND` 또는 프로젝트에 등록된 QT/Study 전용 not found code로 반환한다. 새 ErrorCode 추가가 필요하면 기존 명명 규칙을 따른다.
15. `openapi.yaml`에 `/api/v1/qt/today`, `/api/v1/qt/{qtPassageId}`, `/api/v1/qt/{qtPassageId}/study-content`, `/api/v1/qt/{qtPassageId}/simulator-clips/{clipId}`를 반영한다.
16. Controller test를 먼저 작성하고 최소 구현으로 통과시킨다.
17. Service test에서 `simulatorStatus` enum, `cache.cacheStatus`, `studyContent` 가능 여부, `aiQuestion` 의미, 금지 번역본 문자열 미포함을 검증한다.
18. ArchitectureBoundaryTest 또는 `rg`로 `domain.qt`가 `domain.study.internal`/`web`을 직접 import하지 않는지 확인한다.
19. 실제 Bible API 작업 중인 변경과 충돌하지 않도록 `domain.bible` 파일은 수정하지 않는다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/qt/web/QtControllerTest.java` | `/today`, `/{qtPassageId}` 성공 응답과 envelope |
| `qtai-server/src/test/java/com/qtai/domain/qt/web/QtControllerTest.java` | `qtPassageId < 1` 요청이 `400 VALIDATION_ERROR`로 거절됨 |
| `qtai-server/src/test/java/com/qtai/domain/study/web/StudyQtContentControllerTest.java` | `/{qtPassageId}/study-content`, `/{qtPassageId}/simulator-clips/{clipId}` 성공 응답과 envelope |
| `qtai-server/src/test/java/com/qtai/domain/study/web/StudyQtContentControllerTest.java` | `qtPassageId < 1`, `clipId < 1` 요청이 `400 VALIDATION_ERROR`로 거절됨 |
| `qtai-server/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java` | Today 응답의 `simulatorStatus`가 4상태 enum 중 하나이고 `READY`일 때만 앱 버튼 활성 조건을 만족함 |
| `qtai-server/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java` | `availableActions.aiQuestion`은 F-15 API 구현이 아니라 진입 가능 여부 표시임 |
| `qtai-server/src/test/java/com/qtai/domain/study/internal/StudyQtContentServiceTest.java` | Study/Simulator 목업 응답과 fixture에 금지 번역본 이름 및 실제 성경 본문이 포함되지 않음 |
| `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | `domain.qt`가 `domain.study.internal`, `domain.study.web`, `domain.ai.internal`, `domain.note.internal`을 직접 import하지 않음 |

## 수용 기준

- [ ] `GET /api/v1/qt/today`가 인증된 사용자에게 공통 envelope로 Today QT 목업 응답을 반환한다.
- [ ] `GET /api/v1/qt/{qtPassageId}`가 Today QT와 호환 가능한 상세 목업 응답을 반환한다.
- [ ] `GET /api/v1/qt/{qtPassageId}/study-content`가 `domain.study` 소유 endpoint로 승인 콘텐츠처럼 보이는 더미 summary/explanations/glossaryTerms를 반환한다.
- [ ] `GET /api/v1/qt/{qtPassageId}/simulator-clips/{clipId}`가 `domain.study` 소유 endpoint로 시뮬레이터 목업 응답을 반환한다.
- [ ] `domain.qt`는 해설/노트/시뮬레이터 세부 로직을 직접 구현하지 않는다.
- [ ] `domain.qt`가 Study 정보를 참조해야 할 경우 `domain.study.api` UseCase 또는 `domain.qt.client.study.*UseCaseMock`만 사용한다.
- [ ] `availableActions.simulatorStatus`는 `READY`, `MISSING`, `FAILED`, `DISABLED` 중 하나로만 반환된다.
- [ ] `availableActions.aiQuestion`은 F-15 Q&A API 구현이 아니라 버튼 진입 가능 여부 표시로만 동작한다.
- [ ] 응답 예시, 테스트 fixture, OpenAPI 예시에 실제 성경 본문과 금지 번역본 데이터가 없다.
- [ ] Controller가 Repository를 직접 호출하지 않는다.
- [ ] `domain.qt`와 `domain.study`는 다른 도메인의 `api` 또는 자기 도메인의 `client` 계약만 참조하고, 다른 도메인의 `internal`/`web`을 직접 import하지 않는다.
- [ ] OpenAPI와 Controller 경로가 일치한다.
- [ ] `SecurityConfig`에 `/api/v1/qt/**` 공개 예외를 추가하지 않는다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- QT Today/Detail, Study 콘텐츠/시뮬레이터, OpenAPI, 테스트 보강이 서로 다른 파일 경로에 있어 병렬 작업 이점이 있다.
- 단, 응답 필드명과 도메인 소유권은 한 번 어긋나면 Flutter 연동 비용과 경계 테스트 비용이 커지므로 메인 에이전트가 먼저 계약을 고정해야 한다.
- 목업 데이터의 금지 본문 포함 여부와 도메인 경계 import 검증은 최종 통합 단계에서 한 번 더 직접 확인해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | QT Today/Detail Controller/Service 목업 구현 | `qtai-server/src/main/java/com/qtai/domain/qt/**` |
| Worker 2 | Study 학습 콘텐츠/시뮬레이터 Controller/Service 목업 구현 | `qtai-server/src/main/java/com/qtai/domain/study/**` |
| Worker 3 | QT/Study 테스트 작성 | `qtai-server/src/test/java/com/qtai/domain/qt/**`, `qtai-server/src/test/java/com/qtai/domain/study/**` |
| Worker 4 | OpenAPI 계약 반영 | `qtai-server/apis/api-v1/openapi.yaml` |
| Worker 5 | 도메인 경계와 금지 데이터 검증 보강 | `qtai-server/src/test/java/com/qtai/common/**`, `qtai-server/src/test/java/com/qtai/domain/**` |

### 직접 실행 판단

메인 에이전트는 API 경로, DTO 필드명, 도메인 소유권, ErrorCode 사용 여부, OpenAPI와 테스트의 최종 정합성을 직접 확인한다. 특히 `/simulator` 단축 경로는 API 명세와 충돌하므로 Lead 승인 없이 공개 endpoint로 추가하지 않는다.

## 검증 계획

- `git diff --check`
- `./gradlew -p qtai-server test --tests "*Qt*"`
- `./gradlew -p qtai-server test --tests "*Study*"`
- `./gradlew -p qtai-server test --tests "*ArchitectureBoundaryTest"`
- `./gradlew -p qtai-server build`
- `./gradlew -p qtai-server test jacocoTestReport`
- `./gradlew -p qtai-server jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "com\\.qtai\\.domain\\.(bible|study|note|ai)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/qt --glob "*.java"`
- `rg -n "개역개정|ESV|NIV|성서유니온|두란노" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml`

`spectral` 또는 `gitleaks`가 로컬에 설치되어 있지 않으면 설치 상태와 실패 사유를 PR 본문에 남기고 CI에서 같은 명령을 다시 실행한다.

## 후속 작업으로 넘길 항목

- 실제 `qt_passages`/`qt_passage_verses` DB 조회 기반 Today QT 조립
- Bible UseCase 연동으로 실제 절 범위 조회
- Study UseCase 연동으로 승인 해설/용어/요약 조회
- Note UseCase 연동으로 사용자 DRAFT/SAVED 묵상 노트 진입점 표시
- AI UseCase 연동으로 F-15 Q&A 요청 생성과 결과 조회
- 관리자 웹의 QT 범위 등록/수정/게시 API
