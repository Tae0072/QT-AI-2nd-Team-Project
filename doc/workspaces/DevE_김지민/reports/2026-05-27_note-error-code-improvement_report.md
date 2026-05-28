# 작업 리포트 — 2026-05-27 노트 에러코드 개선 + CRUD 학습

> **브랜치**: `bugfix/note-category-error-codes`
> **PR**: GitHub 500 에러로 대기 중 (push 완료, PR 생성만 남음)
> **담당**: 김지민

---

## 오늘 작업 요약

### 1. POST /api/v1/notes 자유 노트 생성 구현 (B2)

SKILL.md 소크라테스 모드로 진행. 선택지 제시 → 학습 → 코드 작성 순서.

- DTO 검증 위치 ③번 채택: `@Valid`(입력값 형식) + Service(비즈니스 규칙) 분리
- NoteCreateRequest, NoteResponse, CreateNoteUseCase 작성
- NoteService.create() + 카테고리별 switch 검증 구현
- NoteController POST 엔드포인트 (201 Created)
- Note Entity 보강: visibility, savedAt, confirmSave()
- NoteVerse 생성자 + NoteVerseRepository 생성
- ErrorCode N0002~N0005 추가
- 테스트 3종 (단위 8건 + 컨트롤러 2건 + 통합 1건)

**결과**: 빌드+테스트 전체 통과. 그러나 이지윤님이 노트 CRUD 전체(#97, #113, #116)를 먼저 dev에 머지한 상태여서 merge 충돌 발생.

### 2. 이지윤 코드 리딩 + 비교 분석

merge 충돌 해소를 위해 dev 코드를 리딩하고 내 코드와 비교.

| 항목 | 이지윤 | 김지민 | 판정 |
|---|---|---|---|
| 검증 에러코드 | INVALID_INPUT 하나 | 카테고리별 분리 | **김지민 우세** |
| @Valid 형식 검증 | @NotNull + @Size 있음 | @NotNull + @Size 있음 | 동일 |
| 응답 필드 | id + status 2개 | API 명세 6개 | **김지민 우세** (내일 PR) |
| 전체 CRUD | create/get/update/delete 전부 | create만 | 이지윤 포괄적 |

### 3. 수정(PATCH) / 삭제(DELETE) 학습

| 기능 | 핵심 학습 |
|---|---|
| PATCH | create + 앞 3단계(찾기→인가→삭제확인) 추가. delete-and-reinsert 패턴 |
| DELETE | 소프트 삭제 (deletedAt + activeUniqueKey=null). 멱등성 — 이미 삭제되면 그냥 return |

### 4. 개선 PR — 에러코드 카테고리별 분리

dev 코드 기반으로 `INVALID_INPUT`을 5개 구체적 에러코드로 분리.

| 에러코드 | 메시지 | 상황 |
|---|---|---|
| NOTE_BODY_REQUIRED (N0003) | 노트 본문을 입력해 주세요 | PRAYER/REPENTANCE/GRATITUDE body 누락 |
| NOTE_QT_PASSAGE_REQUIRED (N0004) | 묵상 노트에는 QT 본문 ID가 필요합니다 | MEDITATION qtPassageId 누락 |
| NOTE_CONTENT_REQUIRED (N0005) | 제목 또는 본문 중 하나를 입력해 주세요 | 전체 내용 누락 |
| NOTE_QT_PASSAGE_FORBIDDEN (N0006) | 자유 노트에는 QT 본문 ID를 지정할 수 없습니다 | 자유 노트에 qtPassageId 전달 |
| NOTE_VERSE_REQUIRED (N0007) | 설교 노트에는 성경 구절이 필요합니다 | SERMON verseIds 누락 |

변경 파일: ErrorCode.java, NoteService.java, NoteServiceTest.java (3파일)

---

## 오늘 배운 핵심 개념

| # | 개념 | 한 줄 정리 |
|---|---|---|
| 1 | 검증 책임 분리 | @Valid는 형식, Service는 비즈니스 — 책임 레벨이 다르니 나누는 게 역할 분담 |
| 2 | compact constructor | record 전용 편의 문법. 바꿀 필드만 쓰면 나머지는 자동 저장 |
| 3 | UseCase interface | "무엇을 받고 무엇을 돌려주는지"만 정의. Controller는 구현을 모름 |
| 4 | @Transactional 오버라이드 | 클래스 readOnly=true → 쓰기 메서드만 @Transactional로 상쇄 |
| 5 | 201 vs 200 | 200은 일반 성공, 201은 새 리소스 생성 |
| 6 | ResponseEntity | 200 이외 상태 코드가 필요할 때 감싸는 도구 |
| 7 | @Valid 예외 | MethodArgumentNotValidException — Spring이 자동으로 던짐 |
| 8 | 인증 vs 인가 | 인증 = "너 누구야?" / 인가 = "이거 할 수 있어?" |
| 9 | delete-and-reinsert | 비교해서 골라내기보다 전부 밀고 새로 넣는 게 안전하고 간단 |
| 10 | 멱등성 | 같은 요청을 여러 번 보내도 결과가 같다 |
| 11 | switch 표현식 | Java 21 문법. 같은 규칙인 case를 묶을 수 있고, 새 case 누락 시 컴파일러 경고 |

---

## 블로커 / 참고

- **GitHub 500 에러**: PR 생성 대기. push는 완료 (`bugfix/note-category-error-codes`)
- **이지윤 역할 겹침**: 노트 CRUD PR #97에서 김지민 담당 영역(PRAYER/REPENTANCE/GRATITUDE 생성 + 노트 관리) 포함. 슬랙으로 전달 완료
- **한글 경로 Gradle 이슈**: `C:\workspace\qtai-project` junction 링크로 우회 필요

---

## 내일 (2026-05-28 목요일) 작업

| # | 작업 | 상태 |
|---|---|---|
| 1 | PR 생성 (GitHub 500 해소 후) | 대기 |
| 2 | 개선 3: NoteSaveResponse → API 명세 응답 필드 확장 | 예정 |
| 3 | B3: GET /api/v1/sharing-posts 나눔 목록 조회 | 예정 |
