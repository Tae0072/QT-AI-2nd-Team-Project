# Workflow - 2026-05-27 sermon-note-link-and-create

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevA_이지윤 |
| 브랜치 | `master` 기준 작업 후 작업 브랜치 생성 권장: `codex/dev-a-sermon-note-create` |
| PR 대상 | `dev` |
| 관련 F-ID | F-16 자유 노트, F-03 QT 노트 작성·관리, F-10 닉네임 나눔 정책 연계 |
| 트리거 | 설교 노트 선택 구절 연결 테이블과 설교 노트 생성 기능 구현 전 명세 정리 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `qtai-server/02_ERD_문서.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/domain/note/**` |

## 작업 목표

설교 노트(`category=SERMON`)를 저장할 때 사용자가 선택한 성경 절 목록을 `note_verses` 연결 테이블로 함께 저장한다. 설교 노트는 자유 노트이므로 `qt_passage_id`와 `active_unique_key`를 사용하지 않고, 한 날짜에 여러 건 생성할 수 있어야 한다.

이번 작업은 설교 노트 생성 API의 서버 구현 범위를 명확히 하는 명세서다. 구현 시 기존 문서의 단일 `notes` 테이블 정책을 유지하고, 설교 노트의 자유 구절 선택만 `note_verses`로 연결한다.

## 범위

- `POST /api/v1/notes`에서 `category=SERMON` 생성 요청 처리
- `notes` Entity에 설교 노트 저장에 필요한 공통 컬럼 매핑
- `note_verses` 연결 Entity와 Repository 추가
- 설교 노트 필수 입력 검증: 제목 또는 본문 중 하나, `verseIds` 1개 이상
- `verseIds`에 중복 ID가 포함되면 첫 등장 순서만 보존하고 중복 값은 저장하지 않는다.
- 선택 구절 존재 여부는 Bible 도메인의 공개 UseCase를 `domain.note.client.bible` 어댑터 또는 mock으로 연결해 검증한다.
- 생성 성공 시 `201 Created`와 `NoteResponse` 반환
- 설교 노트는 `visibility=PRIVATE`, `activeUniqueKey=null`, `qtPassageId=null`로 생성
- 설교 노트 생성 성공 시 `note_verses.display_order`는 요청 배열 순서대로 저장

## 제외 범위

- QT 노트(`MEDITATION`) 생성·수정 전체 구현
- 기도제목, 회개, 감사일기 생성 구현
- 노트 수정, 삭제, 목록, 상세 조회 API 구현
- 노트 공유 및 `sharing_posts` 스냅샷 생성
- 자유 노트 화면 UI와 Flutter 앱 연동
- Flyway/Liquibase 도입. 현재 저장소에 DB migration 체계가 없으므로 이 작업에서는 JPA Entity와 테스트 기준으로 명세화한다.
- 시뮬레이터, TTS, AI 생성/평가 기능 연동

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/api/CreateNoteUseCase.java` | 설교 노트 생성 UseCase 시그니처 정의 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/api/dto/NoteCreateRequest.java` | `category`, `title`, `body`, `verseIds`, `status` 요청 필드와 Bean Validation 정의 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/api/dto/NoteResponse.java` | 생성 결과 `id`, `category`, `status`, `visibility`, `title`, `body`, `verseIds`, `createdAt`, `savedAt` 반환 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/Note.java` | `notes` Entity 매핑, 설교 노트 factory, 소유자/상태 도메인 메서드 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerse.java` | `note_verses` 연결 Entity, `noteId`/`bibleVerseId`/`displayOrder` 매핑 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteRepository.java` | `notes` 저장과 설교 노트 조회에 필요한 JPA Repository |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerseRepository.java` | 연결 구절 일괄 저장, `noteId` 기준 조회/삭제 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java` | 설교 노트 생성 트랜잭션, 입력 검증, 구절 검증, 저장 순서 제어 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/web/NoteController.java` | `POST /api/v1/notes` 엔드포인트와 공통 응답 매핑 |
| Create | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 설교 노트 생성, 구절 연결, 예외 조건 단위/서비스 테스트 |
| Create | `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java` | HTTP status, envelope, 검증 실패 응답 테스트 |

## 구현 순서

1. `NoteCategory`, `NoteStatus`, `NoteVisibility` enum 필요 여부를 확인하고 없으면 `domain.note.internal`에 추가한다.
2. `NoteCreateRequest`를 API 명세 기준으로 구성한다.
   - `category`: `SERMON` 필수
   - `title`: 선택
   - `body`: 선택
   - `verseIds`: 설교 노트에서는 1개 이상
   - `status`: `DRAFT` 또는 `SAVED`, 미전달 시 `SAVED`로 처리
3. `Note` Entity를 `notes` 테이블에 맞춰 매핑한다.
   - 설교 노트 생성 시 `qtPassageId=null`
   - 설교 노트 생성 시 `activeUniqueKey=null`
   - 기본 공개 범위는 `PRIVATE`
   - `status=SAVED`이면 `savedAt=now`
4. `NoteVerse` Entity를 생성한다.
   - `note_id`와 `bible_verse_id`는 필수
   - `display_order`는 요청 순서 기준 1부터 저장
   - 동일 노트 안의 동일 구절 중복은 저장하지 않고 첫 등장 순서만 유지한다.
5. `NoteService#createSermonNote` 또는 `createNote`에서 `SERMON` 분기를 구현한다.
   - 회원 ID는 현재 인증 Principal에서 받은 값을 사용한다.
   - `title`과 `body`가 모두 비어 있으면 `422 INVALID_INPUT`으로 실패한다.
   - `verseIds`가 비어 있으면 `INVALID_INPUT`으로 실패한다.
   - 존재하지 않는 구절 ID가 포함되면 전체 생성 트랜잭션을 롤백한다.
6. `NoteController`는 요청 검증, 인증 사용자 ID 추출, Service 호출, `ApiResponse.success(data)` 매핑만 수행한다.
7. `CreateNoteUseCase`는 다른 도메인이 노트 생성을 호출할 수 있는 최소 시그니처만 노출하고, `internal` 타입을 외부로 내보내지 않는다.
8. 테스트를 먼저 추가하고 실패를 확인한 뒤 구현한다.
9. API 명세와 충돌하는 필드명 또는 ErrorCode가 발견되면 코드보다 문서 기준을 우선하고, 필요한 변경은 별도 문서 보정 항목으로 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `NoteServiceTest` | `SERMON` 생성 시 `notes.category=SERMON`, `qtPassageId=null`, `activeUniqueKey=null`, `visibility=PRIVATE` |
| `NoteServiceTest` | `verseIds=[3,5,8]` 요청 시 `note_verses`가 `displayOrder=1,2,3`으로 저장 |
| `NoteServiceTest` | 제목과 본문이 모두 비어 있으면 저장하지 않고 예외 발생 |
| `NoteServiceTest` | `verseIds`가 비어 있으면 저장하지 않고 예외 발생 |
| `NoteServiceTest` | 존재하지 않는 `bibleVerseId`가 있으면 `notes`와 `note_verses` 모두 롤백 |
| `NoteServiceTest` | 같은 날짜에 설교 노트를 2건 생성할 수 있음 |
| `NoteControllerTest` | `POST /api/v1/notes` 성공 시 `201 Created`와 공통 envelope 반환 |
| `NoteControllerTest` | 작성자 인증이 없으면 `401 UNAUTHORIZED` |

## 수용 기준

- [ ] `POST /api/v1/notes`로 `category=SERMON` 요청을 보내면 설교 노트가 생성된다.
- [ ] 생성된 설교 노트는 `notes.qt_passage_id`가 `NULL`이다.
- [ ] 생성된 설교 노트는 `notes.active_unique_key`가 `NULL`이다.
- [ ] 생성된 설교 노트는 기본 `visibility=PRIVATE`이다.
- [ ] 선택한 성경 절은 `note_verses`에 노트 ID와 함께 저장된다.
- [ ] `note_verses.display_order`는 요청한 구절 순서를 보존한다.
- [ ] 설교 노트는 동일 사용자·동일 날짜 중복 제한을 받지 않는다.
- [ ] 제목과 본문이 모두 없는 설교 노트는 생성되지 않는다.
- [ ] 구절이 없는 설교 노트는 생성되지 않는다.
- [ ] 존재하지 않는 성경 절 ID가 있으면 노트 본문도 저장되지 않는다.
- [ ] Controller는 Repository를 직접 호출하지 않는다.
- [ ] `note` 도메인은 다른 도메인의 `internal`, `web` 타입을 import하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 설교 노트 생성은 `Note`, `NoteVerse`, `NoteService`, `NoteController`가 한 트랜잭션으로 강하게 연결되어 있다.
- 테스트와 구현을 같은 흐름에서 순서대로 조정해야 해서 병렬 작업보다 직접 실행이 충돌을 줄인다.
- 현재 `note` 도메인은 스켈레톤 상태라 파일 간 계약을 한 번에 맞추는 편이 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 테스트 작성, 구현, 검증을 순서대로 직접 수행한다.

## 검증 계획

- `.\gradlew.bat test`
- 필요한 경우 `.\gradlew.bat test --tests "*NoteServiceTest"`
- 필요한 경우 `.\gradlew.bat test --tests "*NoteControllerTest"`
- `rg "domain\\.[^.]+\\.internal" qtai-server/src/main/java/com/qtai/domain/note`로 note 도메인의 금지 import 여부 확인
- `rg "Repository" qtai-server/src/main/java/com/qtai/domain/note/web`로 Controller의 Repository 직접 호출 여부 확인

## 후속 작업으로 남길 항목

- `PATCH /api/v1/notes/{noteId}`에서 설교 노트 구절 교체 정책 구현
- `GET /api/v1/notes/{noteId}`에서 `note_verses`를 성경 좌표 응답으로 확장
- `GET /api/v1/notes` 목록 필터의 `category=SERMON` 조회 구현
- 노트 공유 시 설교 노트 구절 스냅샷을 `sharing_posts.verse_snapshot_json`에 저장
- 프로젝트 차원의 DB migration 도구 도입 여부 결정
