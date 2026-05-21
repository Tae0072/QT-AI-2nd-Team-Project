# 진행 중 Todo — 자유 노트 API 구현 (W2)

> **현재 브랜치**: `feature/note-freenote-impl`
> **목표**: `POST /api/v1/notes` (개인 노트 생성 API) 완성
> **최종 업데이트**: 2026-05-21

---

## 진행 상황 한눈에

```
[████████████░░░░░░░░░░░░░░] 3 / 12 완료 (25%)

✅ 완료     ▶ 진행 중     ⏳ 대기 (의존성)     ⬜ 예정
```

---

## Todo 리스트

### 사전 준비 (인프라·임시 코드) — 완료 ✅

- [x] **사전 준비 1**: ApiResponse 헬퍼 확인 — 이미 `common/dto/ApiResponse.java` 존재. 새로 만들 필요 없음.
- [x] **사전 준비 2**: dev 프로파일 SecurityConfig — `security/DevSecurityConfig.java` 새로 작성. `@Profile("dev")` 로 운영 환경과 분리.
- [x] **사전 준비 3**: NoteCategory enum — 이미 `note/internal/NoteCategory.java` 존재 (5종 상수 + 카테고리별 정책 주석). 그대로 사용.

### 본 작업 — 대기 중 ⏳

- [ ] ⏳ **(이지윤 작업 대기) Entity: Note.java 합의 후 채우기**
  - 막힌 이유: Note Entity는 이지윤 소유 (notes 테이블 공유)
  - 필요한 합의 항목: `qtId` nullable 여부, `title` 필드 존재 여부, 추가 필드
  - 의존성: 이 task 완료 전엔 아래 6~10번 진행 불가

- [ ] ⬜ **노트 도메인 ErrorCode 추가** — `N0001 NOTE_NOT_FOUND`, `N0002 NOTE_FORBIDDEN`, `N0003 DUPLICATE_NOTE` 등 (Service 짤 때 함께 추가)

- [ ] ⬜ **Repository**: `NoteRepository.java` JpaRepository 상속 + save 메서드
- [ ] ⬜ **DTO (Request)**: `NoteCreateRequest` record 필드 채우기 (`title`, `content`, `category`)
- [ ] ⬜ **DTO (Response)**: `NoteResponse` record 신규 생성
- [ ] ⬜ **UseCase**: `CreateNoteUseCase` 인터페이스 시그니처 — `NoteResponse create(Long memberId, NoteCreateRequest)`
- [ ] ⬜ **Service**: `NoteService` implements `CreateNoteUseCase` + `@Service` + `@Transactional` + create() 메서드 본문
- [ ] ⬜ **Controller**: `NoteController` 에 `@PostMapping` 추가 — `POST /api/v1/notes`
- [ ] ⬜ **검증**: postman 으로 `POST /api/v1/notes` 호출 테스트 (X-User-Id 헤더 추가)

---

## 🔴 팀원 합의·확인 (5/22 금까지 — 가장 시급)

> W2 시작(5/25 월) 전에 아래 5개 중 하나라도 미완성이면 → 그 부분은 **더미 데이터·임시 객체로 먼저 만들고** 나중에 연결. 일정 조율 필요시 팀장(이승욱)에게 공유.

### 이지윤에게 물어볼 것 2가지

- [ ] **① Note 테이블 구조 합의**
  - 질문: "Note 테이블에 category 필드 있어? PRAYER, REPENTANCE, GRATITUDE 다 들어가?"
  - 왜: 자유 노트도 같은 테이블 사용. 구조 확정 안 되면 나중에 뜯어고쳐야 함.
  - 답 받으면: Repository → DTO → Service → Controller 진행 가능
- [ ] **② 두 API 완성 시점 확인**
  - 질문: "노트 목록 조회(`GET /api/v1/notes`)랑 달력 API(`GET /api/v1/me/meditation-calendar`) 언제 완성돼?"
  - 왜: 내 Flutter N-01 화면이 이걸 호출. 없으면 화면 연결 못 함.
  - 추가 요청: "달력 응답에 PRAYER·REPENTANCE·GRATITUDE 카테고리도 포함시켜줘"

### 이승욱에게 물어볼 것 3가지

- [ ] **③ 공통 응답 형식 완성 시점**
  - 질문: "ApiResponse 공통 포맷 완성됐어? 내 API 응답 다 이걸로 감싸야 해서."
  - 추가 확인: 이미 `common/dto/ApiResponse.java` 존재. 다만 `{ success, data, error }` 3개 필드만 — CLAUDE.md 명시된 `timestamp`, `traceId` 빠짐. 추가할지 결정 필요.
- [ ] **④ 나눔 Entity 4개 완성 시점**
  - 질문: "SharingPost, Comment, PostLike, Report Entity 언제 완성돼?"
  - 왜: W2 나눔 목록·상세 조회 API 만들려면 필요.
- [ ] **⑤ Flutter 초기 세팅 머지 시점**
  - 질문: "Flutter 프로젝트 기본 세팅 (상태관리·HTTP·라우팅·공통 위젯·에러 인터셉터) 언제 dev에 올라와?"
  - 왜: 김지민 Flutter 화면 작업은 이 위에서 시작.

### 💬 슬랙 메시지 템플릿 (이지윤 — 가장 까다로운 합의)

```
지윤아, 내가 N-01 화면에서 5개 카테고리 통합으로 보여줘야 하는데,
GET /api/v1/notes 응답에 PRAYER·REPENTANCE·GRATITUDE 카테고리도
포함돼야 해. 회의록에는 네 담당으로 돼 있는데, 혹시 W2 안에 가능해?
응답 형식만 먼저 알려줘도 내가 Flutter 더미 데이터로 먼저 만들어놓을게.
```

> 이지윤이 "일이 너무 많아" 하면 → 팀 전체 회의에서 공유. 김지민 혼자 떠안지 말 것.

### 의존성 매트릭스 — 한 줄 요약

| 팀원 | 확인 내용 | 기다려야 하나? |
| --- | --- | --- |
| 이지윤 | Note 테이블 구조 확정 | ✅ Repository·DTO·Service 시작 전 |
| 이지윤 | 노트 목록 + 달력 API 완성 시점 | ✅ Flutter 연결 전 |
| 이승욱 | 공통 응답 형식 | ✅ Controller 응답 형식 결정 전 |
| 이승욱 | 나눔 Entity 4개 | ✅ W2 나눔 API 만들기 전 |
| 이승욱 | Flutter 초기 세팅 | ✅ Flutter 화면 작업 시작 전 |

---

## 막힌 부분

### 1. Note Entity 이지윤 작업 대기 (가장 시급)

- 본 작업 6번~10번 모두 Note Entity 클래스가 있어야 진행 가능
- 슬랙으로 이지윤에게 진행 상황 + 시그니처 합의 요청 필요 (5/22 금까지)

### 2. application.yml 부재 (인프라 이슈, 김지민 외)

- `qtai-server/src/main/resources/` 폴더 자체가 없음
- dev 프로파일 활성화하려면 IDE Run Configuration 에 `-Dspring.profiles.active=dev` 수동 추가 필요
- 별도 인프라 PR 필요 (강태오 Lead 또는 이승욱 영역)

### 3. gradle wrapper 부재 (인프라 이슈, 김지민 외)

- `qtai-server/gradlew*` 파일 없음
- 로컬에서 `./gradlew build` 명령 안 됨
- 별도 인프라 PR 필요

---

## 다음 세션 시작 시 할 일 (Quick Start)

1. **현재 브랜치 확인**: `git status` (feature/note-freenote-impl 인지)
2. **dev 최신화**: `git checkout dev && git pull && git checkout feature/note-freenote-impl && git merge dev`
3. **오늘 작업 리포트 재정독**: `doc/workspaces/DevE_김지민/reports/2026-05-21_자유노트-시작-준비_리포트.md`
4. **이지윤에게 슬랙 답 확인**: Note Entity 시그니처 합의됐는지
5. **합의됐으면**: Repository → DTO → UseCase → Service → Controller 순서로 진행
6. **합의 안 됐으면**: bible 도메인 학습 리포트 재정독 + Note Entity 모양 시뮬레이션
