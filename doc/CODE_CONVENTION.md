# CODE_CONVENTION.md — QT-AI 구현 저장소 코드 컨벤션

이 문서는 실제 구현 저장소의 `qtai-server`와 `flutter-app`에서 공통으로 적용할 코드 작성 기준이다. 요구사항·아키텍처·API 기준은 문서 저장소의 `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`를 우선한다.

## 1. 공통 원칙

- 구현은 요구사항의 F-ID와 연결한다. 기능 PR에는 관련 F-ID를 남긴다.
- 사용자-facing 메시지, PR 설명, 도메인 주석은 한국어를 우선한다.
- 파일 인코딩은 UTF-8을 사용한다.
- secret, token, password, private key, 민감 개인정보를 코드·예시·로그에 남기지 않는다.
- 관련 없는 리팩터링, 대규모 포맷팅, 파일 이동은 같은 PR에 섞지 않는다.
- 생성 파일, build output, coverage HTML, 임시 파일은 커밋하지 않는다.
- "저작권 문제 없음" 대신 "저작권 리스크를 낮춘다"를 사용한다.
- "유실률 0%" 대신 "핸들러 실패 로그와 재처리 가능 상태를 남긴다"를 사용한다.

## 2. Backend 패키지 구조

루트 패키지는 `com.qtai`로 고정한다.

```text
qtai-server/src/main/java/com/qtai
├── common
├── config
├── security
├── external
├── batch
└── domain
    ├── member
    ├── bible
    ├── qt
    ├── study
    ├── note
    ├── sharing
    ├── report
    ├── notification
    ├── praise
    ├── mission
    ├── ai
    ├── admin
    └── audit
```

각 도메인은 기본적으로 아래 하위 패키지를 사용한다.

```text
domain/<name>
├── api       # 다른 도메인이 호출 가능한 UseCase interface와 DTO
├── internal  # Entity, Enum, Service, Repository, QueryRepository 등 내부 구현
└── web       # /api/v1/** Controller, Request/Response DTO
```

- 다른 도메인이 import할 수 있는 타입은 `api/` 하위 UseCase/DTO로 제한한다.
- 다른 도메인의 `internal/`, `web/` 타입을 직접 import하지 않는다.
- `note`, `sharing`, `praise`는 `bible` 하위가 아니라 최상위 도메인이다.
- Controller가 Repository를 직접 호출하지 않는다.
- 내부 도메인 호출을 HTTP 경로로 우회하지 않는다.
- `audit.api.WriteAuditLogUseCase`처럼 문서에서 허용한 횡단 UseCase만 예외로 둔다.

## 3. Backend 네이밍

| 대상 | 형식 | 예시 |
| --- | --- | --- |
| Controller | `{Domain}Controller`, `Admin{Domain}Controller`, `System{Domain}Controller` | `TodayQtController`, `AdminAiAssetController` |
| UseCase interface | 동사구 + `UseCase` | `CreateNoteUseCase`, `WriteAuditLogUseCase` |
| Service | `{Domain}{Action}Service` 또는 `{Domain}Service` | `NoteService`, `AiAssetReviewService` |
| Entity | 단수 명사 | `Member`, `QtPassage`, `AiGeneratedAsset` |
| Repository | `{Entity}Repository` | `MemberRepository` |
| 조회 전용 Repository | `{Domain}QueryRepository` | `TodayQtQueryRepository` |
| Request DTO | `{Action}Request` | `CreateNoteRequest` |
| Response DTO | `{Resource}Response` | `TodayQtResponse` |
| Command DTO | `{Action}Command` | `CreateNoteCommand` |
| Result DTO | `{Action}Result` | `CreateNoteResult` |
| Enum | 의미 있는 상태명 | `SimulatorStatus`, `AiAssetStatus` |
| 테스트 | `{Target}Test` | `NoteServiceTest`, `TodayQtControllerTest` |

- 메서드는 동사로 시작한다: `create`, `update`, `delete`, `find`, `get`, `list`, `approve`, `reject`, `publish`, `markAsRead`.
- boolean 메서드는 `is`, `has`, `can`, `should`로 시작한다.
- 약어는 프로젝트 표준 약어만 사용한다: `QT`, `AI`, `DTO`, `API`, `URL`, `ID`.
- DB 컬럼은 `snake_case`를 사용한다. API JSON 필드는 `04_API_명세서.md` 기준을 따르고, Java/Dart 코드에서는 `camelCase`를 사용한다.

## 4. Backend DTO 규칙

- Request/Response DTO는 가능하면 Java `record`를 사용한다.
- Request DTO에는 `@NotNull`, `@NotBlank`, `@Size`, `@Pattern` 등 Bean Validation을 명시한다.
- Entity를 Controller 응답으로 직접 반환하지 않는다.
- Page 응답은 공통 `PageResponse<T>`로 감싼다.
- 날짜/시간 API 응답은 ISO-8601 문자열을 사용한다.
- 서버 기준 시간대는 `Asia/Seoul`이며, 정책 판단에는 KST를 명확히 사용한다.
- 관리자 응답 DTO에는 필요한 필드만 포함한다. token, private key, 내부 검증 원문은 노출하지 않는다.

```java
public record UpdateProfileRequest(
    @NotBlank
    @Size(min = 2, max = 30)
    String nickname
) {
}
```

## 5. Entity 규칙

- Entity는 해당 도메인의 `internal/`에 둔다.
- 운영 테이블 Entity는 공통 `BaseEntity`를 상속한다.
- `@Data`를 사용하지 않는다. 필요한 getter만 열고 setter는 지양한다.
- 생성은 constructor 또는 static factory로 제한한다.
- 상태 변경은 `approve()`, `hide()`, `markAsRead()` 같은 도메인 메서드로 표현한다.
- 상태 값은 문자열 상수 대신 enum을 사용한다.
- `@ManyToOne(fetch = FetchType.LAZY)`를 기본으로 한다.
- 양방향 연관관계는 필요한 경우에만 만든다.
- 컬렉션은 외부에서 직접 수정하지 않는다.
- 소프트 딜리트는 도메인 메서드에서 상태, `deletedAt`, `activeUniqueKey` 등을 함께 처리한다.

## 6. Repository 규칙

- Repository는 해당 도메인의 `internal/`에 둔다.
- 기본 CRUD는 `JpaRepository<Entity, Long>`를 사용한다.
- 복잡한 목록, 검색, 통계는 `*QueryRepository`로 분리한다.
- Repository 메서드명 쿼리가 길어지면 JPQL 또는 QueryDSL로 전환한다.
- Repository는 Entity, primitive, projection만 반환한다.
- API Response DTO를 Repository에서 직접 반환하지 않는다.
- 다형 참조(`targetType`, `targetId`) 무결성은 Service에서 검증한다.

## 7. Service와 Transaction

- Service는 해당 도메인의 `internal/`에 둔다.
- 다른 도메인에 공개할 기능은 `api/`의 UseCase interface로 정의하고, 구현은 `internal/` Service가 담당한다.
- 쓰기 public 메서드에는 `@Transactional`을 붙인다.
- 조회 public 메서드에는 `@Transactional(readOnly = true)`를 붙인다.
- 권한 검증, 소유자 검증, 상태 전이는 Service에서 수행한다.
- 외부 API 호출을 DB 트랜잭션 안에서 오래 잡지 않는다.
- 외부 호출 실패 가능성이 있으면 실패 상태와 재처리 가능 정보를 저장한다.
- `jakarta.*`를 사용하고 `javax.*`를 사용하지 않는다.

## 8. Controller 규칙

- Controller는 해당 도메인의 `web/`에 둔다.
- URL은 `04_API_명세서.md`의 `/api/v1/**` 경로와 일치해야 한다.
- Controller는 인증 Principal 추출, Request 검증, Service/UseCase 호출, Response 매핑만 수행한다.
- 비즈니스 조건문을 Controller에 두지 않는다.
- 성공 응답은 공통 응답 포맷으로 감싼다.
- 생성은 `201 Created`, 삭제·로그아웃·완료 처리는 필요한 경우 `204 No Content`를 사용한다.
- 관리자 API는 적절한 admin role을 확인한다.

## 9. Exception과 Logging

- 도메인별 예외는 도메인 내부에 두고, 최종 응답 변환은 `GlobalExceptionHandler`에서 처리한다.
- ErrorCode는 API 명세와 맞춘다: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `INVALID_STATUS_TRANSITION`, `AI_QUESTION_BLOCKED` 등.
- 로그에는 password, token, private key, 주민등록번호, 전화번호 등 민감 정보를 남기지 않는다.
- event handler 실패 로그에는 `eventId`, event type, handler name, error message를 남긴다.
- 빈 `catch` 블록은 금지한다.
- 외부 API 오류는 공통 예외로 감싸고, 원문 응답 전체를 사용자에게 노출하지 않는다.

## 10. AI와 콘텐츠 구현 규칙

- 허용 AI 흐름은 사전 생성/검증과 F-15 단발성 사실 기반 Q&A뿐이다.
- AI 자유 챗봇, 다중 턴 대화, SSE, `/ai/sessions/**`는 금지한다.
- 사용자 요청 경로에서 해설·시뮬레이터를 즉시 생성하지 않는다.
- F-15 Q&A는 단어, 시대상, 역사적 배경 질문으로 제한한다.
- AI 응답은 검증 전 사용자에게 노출하지 않는다.
- AI 작업은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`에 기록한다.
- 찬양은 운영자 큐레이션 메타데이터만 다룬다. AI 찬양 추천, 가사 저장, 음원 저장, 직접 YouTube URL 입력은 금지한다.
- 개역개정, ESV, NIV seed/test/fixture/response 데이터는 금지한다.

## 11. Backend 테스트 규칙

테스트 패키지는 운영 코드와 같은 구조를 따른다.

```text
qtai-server/src/test/java/com/qtai
├── architecture
├── common
├── security
├── domain
└── support
```

- 테스트 도구는 JUnit 5, AssertJ, Mockito, MockMvc, DataJpaTest, MockWebServer를 기준으로 한다.
- 테스트 메서드명은 한국어 또는 명확한 영어 문장형을 허용하되 한 파일 안에서는 스타일을 섞지 않는다.
- 권장 형식: `상황_결과` 또는 `given_when_then`.
- Fixture는 `support/fixture`, factory는 `support/factory`에 둔다.
- Controller 테스트는 HTTP status, 공통 envelope, error code를 검증한다.
- Service 테스트는 권한, 소유자, 상태 전이, 트랜잭션 결과를 검증한다.
- Repository 테스트는 쿼리 조건, 정렬, 페이징, 중복 제약을 검증한다.
- ArchUnit/Spring Modulith 테스트로 도메인 경계와 금지 import를 검증한다.

필수 테스트 영역:

- 00:00/04:00 Today QT cache 동작
- `READY/MISSING/FAILED/DISABLED` simulator status
- AI 자유 챗봇/SSE 부재
- F-15 Q&A 차단·검증·실패 처리
- 승인되지 않은 AI 산출물 미노출
- admin authorization
- Bible source metadata와 금지 번역본 차단
- event handler 실패 로그와 재처리 가능 상태
- Controller에서 Repository 직접 호출 금지

## 12. Flutter 구조와 네이밍

Flutter 앱은 feature-first 구조를 우선한다.

```text
flutter-app/lib
├── app
├── core
├── shared
└── features
    ├── auth
    ├── today_qt
    ├── bible
    ├── note
    ├── sharing
    ├── praise
    ├── mission
    ├── ai_qa
    ├── my_page
    └── admin
```

| 대상 | 형식 | 예시 |
| --- | --- | --- |
| 파일명 | `snake_case.dart` | `today_qt_screen.dart` |
| 클래스명 | `PascalCase` | `TodayQtScreen` |
| 변수/메서드 | `camelCase` | `loadTodayQt()` |
| Widget | 목적 + `Widget/Tile/Card/Button/Screen` | `SimulatorStatusButton` |
| State/Controller | 목적 + `State/Controller/Notifier` | `TodayQtController` |
| Model/DTO | 서버 응답 의미 + `Dto/Model` | `TodayQtResponseDto` |
| Test file | 대상 + `_test.dart` | `today_qt_screen_test.dart` |

- 한 파일에는 주된 public Widget 하나를 둔다.
- 재사용되는 UI는 `shared/` 또는 feature 내부 `widgets/`로 분리한다.
- API DTO와 화면 표시용 Model을 구분한다.
- 서버 JSON 필드는 DTO에서 명확히 매핑한다.
- 로그인 전에는 콘텐츠 화면으로 진입하지 않는다.
- Today QT, 성경 본문, 해설, 찬양, 묵상 저장, 묵상 달력은 인증 후 접근한다.

## 13. Flutter 코드 스타일

- `flutter_lints` 기준을 사용한다.
- `prefer_const_constructors`, `prefer_final_locals`, `require_trailing_commas`, `avoid_print`를 지킨다.
- 디버그 출력은 `print` 대신 프로젝트 로거를 사용한다.
- 비동기 함수는 실패 상태를 명시적으로 처리한다.
- loading, empty, error, success 상태를 UI에서 구분한다.
- 긴 `build()` 메서드는 private widget method 또는 별도 Widget으로 분리한다.
- 화면 텍스트는 가능하면 상수 또는 localization 확장 가능 구조로 둔다.
- 버튼 활성화 조건은 서버 상태값과 동일하게 둔다. 시뮬레이터 버튼은 `READY`일 때만 활성화한다.
- AI Q&A 화면은 single-turn 입력/응답만 지원한다. 채팅방·대화 목록·streaming UI를 만들지 않는다.

## 14. Flutter 테스트 규칙

- 주요 화면은 Widget test를 작성한다.
- 순수 Dart 로직은 unit test로 검증한다.
- Repository/API client는 mock 또는 fake client로 성공·실패를 모두 검증한다.
- 인증 전 접근, 관리자 권한 없음, offline/error 상태를 테스트한다.
- Today QT 화면은 본문, 해설 진입점, 노트 진입점, simulator status 표시를 검증한다.
- AI Q&A 화면은 차단 응답, 검증 실패, 외부 AI 장애 안내를 검증한다.

## 15. API 연동 규칙

- 서버와 앱 모두 API 경로는 `/api/v1/**`를 기준으로 한다.
- 앱은 서버 ErrorCode를 임의 문자열로 재해석하지 않는다.
- 서버 enum 값은 앱에서 동일 문자열로 매핑한다.
- `READY`, `MISSING`, `FAILED`, `DISABLED` 외 simulator status를 만들지 않는다.
- 사용자 응답 DTO에 검증 참조 원문, prompt 원문, provider secret을 포함하지 않는다.
- API 변경 시 서버 테스트, OpenAPI 문서, Flutter DTO/parser 테스트를 함께 갱신한다.

## 16. 검증 명령

Backend:

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test jacocoTestReport
./gradlew -p qtai-server jacocoTestCoverageVerification
```

OpenAPI/secret:

```bash
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
```

Flutter:

```bash
cd flutter-app
flutter pub get
flutter analyze
flutter test --coverage
```

## 17. PR 전 체크리스트

- 관련 F-ID와 기준 문서를 PR에 적었는가
- 도메인 경계를 깨지 않았는가
- Controller가 Repository를 직접 호출하지 않는가
- Entity를 API 응답으로 직접 반환하지 않는가
- 쓰기 Service에 `@Transactional`이 있는가
- AI 자유 챗봇/SSE/RAG/AI 찬양 추천을 추가하지 않았는가
- secret과 민감 정보가 코드·로그·예시에 없는가
- 변경 범위에 맞는 서버/Flutter 테스트를 추가했는가
- 실행한 검증 명령과 실패/미실행 사유를 PR에 남겼는가
