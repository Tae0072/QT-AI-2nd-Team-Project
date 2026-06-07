# 2026-06-05 전체 코드베이스 정밀 리뷰 결과 보고 (오류·중복·충돌·MSA 분리 위험)

> Lead 요청: "작업 프로젝트 전부, 글자 하나하나 읽고 오류·중복·충돌·수정 필요 사항과 MSA 분리 시 문제를 확인."
> 기준 커밋: `be88e9e` (= origin/dev 최신 `76e9b9f`과 **트리 해시 동일** — 같은 내용)

## 검토 범위·방법

- 서버 main Java **629파일 전부**(23,465 LOC), 테스트 Java 주요 전수(아키텍처·도메인·인프라), Flyway 마이그레이션 **V1~V23 전부**, application yml 4종, build.gradle.kts, Dockerfile/compose 2종, CI 워크플로우 전부, OpenAPI(`qtai-server/apis/api-v1/openapi.yaml`), ERD 문서, Flutter **lib 66 + test 22 전 dart 파일**(~10k LOC), Android/iOS 설정.
- 방법: ① 영역별 10개 정밀 리뷰 패스(도메인 단위 전 라인 정독) → ② 발견된 핵심 주장 17건을 코드 원문으로 **교차 재검증**(grep+원문 대조) → ③ 리뷰 패스 간 상충 주장 해소(예: "ArchUnit 부재" 주장은 오판으로 폐기 — `DomainBoundaryArchTest`는 실제 ArchUnit임). 총 2~3회 검토 원칙 준수.
- 본 리뷰는 읽기 전용이며 **소스 코드는 일절 수정하지 않음**.

## 0. 사전 발견: 로컬 Git 상태 (작업 전 조치 완료)

| 항목 | 상태 |
|------|------|
| `.git/index` | **손상되어 있었음**(bad signature). 작업 트리가 HEAD와 완전 일치함을 확인한 뒤 `git read-tree HEAD`로 인덱스 재구성 → `git status` 깨끗함(변경 0건). 데이터 손실 없음 |
| `.git/index.lock` | 잔존 lock 파일 발견·제거 완료 |
| 브랜치 | 현재 `chore/sharing-remove-unused-client-mocks` = dev+2커밋. 해당 2커밋은 PR #254로 squash 머지되어 **origin/dev와 내용 동일** |
| 로컬 `master` | origin/master 대비 **7 ahead / 14 behind로 어긋남** → `git fetch` 후 `git branch -f master origin/master` 권장 (master 직접 작업 금지 규칙상 로컬 master는 추적용으로만) |
| 로컬 `dev` | 1커밋 뒤처짐(#254 머지 전) → `git pull` 필요 |

---

## 1. Critical — 기능이 실제로 동작하지 않는 결함 (전부 코드 원문 재검증 완료)

### C-1. 관리자 2차 권한(`ADMIN_ROLE_*`)을 발급하는 코드가 없음 → 운영에서 관리자 API 전멸(403)
- 소비처: `ai/web/AdminAiAssetController:284`, `ai/web/AdminAiAuthentication:56`, `audit/web/AdminAuditAuthentication:48`, `report/web/AdminReportController:109` — 모두 `ADMIN_ROLE_OPERATOR/REVIEWER/SUPER_ADMIN` authority 요구.
- 발급처: **0곳.** `JwtAuthenticationFilter:60`은 `ROLE_USER`/`ROLE_ADMIN` 하나만 부여. Dev 필터도 `ROLE_USER` 고정.
- 결과: 실제 JWT로는 신고 처리, 감사 조회, AI 자산 승인/거절/숨김 전부 403. 테스트가 authority를 수동 주입해서 전부 통과 → CI가 못 잡음.
- 추가: CLAUDE.md §5 "admin_users.admin_role **DB 확인**"을 수행하는 곳도 admin/me 외엔 없음. admin 도메인에 이미 있는 `VerifyAdminRoleUseCase`(DB 조회·DISABLED 즉시 반영)를 각 admin API에서 호출하도록 통일하는 것을 권장(토큰 claim 방식보다 CLAUDE.md 부합·계정 비활성 즉시 반영).

### C-2. 00:05 AI 해설 시딩이 항상 "어제" 본문을 시딩
- `AiDailyQtVerseExplanationSeedScheduler` cron `0 5 0 * * *`(00:05) → `getTodayQtUseCase.getToday(null)` 호출.
- 그런데 `QtPassageLookup.findTodayPassage()`는 00:00~04:00 구간에 **무조건 어제 본문(STALE_FALLBACK)** 반환 (오늘 본문이 DB에 있어도).
- 결과: 어제 본문은 이미 시딩돼 전부 skip → **오늘 본문 해설 job은 영영 그날 안 만들어짐**(다음날 00:05에야 생성 → 04:00 노출 사이클 항상 미스). §6 "오늘 QT passage 전제에서 00:05 시딩"과 정면 충돌.
- 부가: SU 본문 수집 스케줄러(`SuTodayPassageImportScheduler`)도 **같은 00:05 cron** — 실행 순서 보장 없음(수집 전 시딩 가능). 시딩은 캐시 정책을 우회하는 내부용 "특정 날짜 본문 조회" 경로를 따로 가져야 함.

### C-3. `qt_passage_verses`를 채우는 코드가 어디에도 없음 → 자동 수집 본문의 AI/학습 파이프라인 단절
- `QtPassageVerse` 참조처: 엔티티·리포지토리·`QtService`(읽기)뿐. SU import(`QtTodayPassageImportService`)는 `qt_passages` 행만 만든다.
- 그런데 해설 시딩·학습 콘텐츠·시뮬레이터가 쓰는 `verseIds`는 전부 `qt_passage_verses` 조인에서 나옴 → SU로 수집된 본문은 **해설 job 0건, 학습 콘텐츠 빈 목록**. (현재 동작하는 건 V8 수동 시드 3건 + 테스트 직접 INSERT뿐.)
- 수정: import 시 `start_verse~end_verse` 범위를 bible api로 verse id 매핑해 `qt_passage_verses`를 채우는 단계 추가. "범위 컬럼 vs 매핑 테이블" 중 진실의 원천도 명문화 필요.

### C-4. Today QT의 해설·시뮬레이터가 서버·클라 양쪽에서 끊겨 있음 ("Today QT 100%" 미충족)
- 서버: `QtPassageLookup.toResponse`/`QtService.getPassage` 둘 다 `simulatorStatus="MISSING"`, `hasExplanation=false` **하드코딩**. study 도메인에는 `QtSimulatorService`(APPROVED→READY 판정)와 승인 해설 조회가 **이미 구현돼 있는데 qt가 호출하지 않음**.
- Flutter: `TodayQtSummary.fromJson`이 `simulatorStatus`/`hasExplanation`/`draftNoteId`를 **파싱조차 안 함**(grep 0건), 해설·시뮬레이터 버튼은 `qtPassageId: null` 고정으로 영구 비활성, 노트 버튼 핸들러는 빈 함수.
- 결과: 콘텐츠가 승인돼도 사용자 진입점이 전부 닫혀 있음. §6 "READY일 때만 버튼 활성화" 규칙이 양방향 모두 미이행.

### C-5. 댓글 목록: 탈퇴 회원 1명이면 해당 글 댓글 API 전체가 404 (+ N+1)
- `sharing/internal/CommentService.list`가 댓글마다 `getMemberPublic()` 호출(페이지당 최대 20회 — N+1). `MemberService.getMemberPublic`은 탈퇴 회원에 `MEMBER_NOT_FOUND` **throw** → 목록 전체 실패.
- 탈퇴자 글·댓글은 2년 보존이므로 시간이 갈수록 깨지는 글이 누적. 수정: 댓글 작성 시점 닉네임 비정규화(스냅샷) 또는 벌크 조회+탈퇴자 placeholder 폴백. (MSA 분리 시엔 원격 N+1이라 더 치명.)

### C-6. 노트 삭제 ↔ 나눔글 연동(`source_note_unshared_at`) 전체 미구현
- 쓰기 코드 0건(엔티티 선언 + 읽기 3곳뿐). 명세 §4.3.7의 "원본 삭제 안내" 분기·`HIDDEN 자동 전환`(주석 명시)이 전부 미동작 — 노트를 지워도 나눔글은 영구 PUBLISHED.

### C-7. 신고 처리(resolve)가 빈 껍데기 + 처리자 ID 의미 오류
- `AdminReportService.process`: 상태 전이만 하고 **대상 숨김(HIDE_TARGET)·신고자 알림·감사 기록 전부 TODO**. RESOLVED 처리해도 신고당한 글이 계속 노출, 신고자는 결과를 모름.
- `processedByAdminId`에 JWT principal(=**members.id**)을 저장하는데 DDL FK는 **admin_users.id** → 운영 MySQL에서 FK 위반 500 또는 데이터 오염. audit의 actor_id도 동일 계열(memberId 기록). C-1과 같은 축 — `VerifyAdminRoleUseCase`가 주는 `adminUserId`를 쓰면 함께 해결.

### C-8. qt 단건 조회에 공개 게이트 없음 — 미래 본문 선노출
- `QtService.getPassage`/`getContentContext`는 qtDate·04:00 검사 없이 row만 있으면 반환(`published=true` 하드코딩). 선등록된 내일/모레 본문을 id 순회로 미리 열람 가능 — §6 "00:00 KST 공개" 우회. 승인 해설·클립도 미래 본문 것이 study 경로로 새어 나감.

### C-9. Flutter 치명 결함 3종
- **iOS 카카오 로그인 불가**: Info.plist에 `CFBundleURLTypes`(kakao{KEY})·`LSApplicationQueriesSchemes` 전무.
- **Android dev API 호출 전면 차단**: base URL이 `http://10.0.2.2:8080`인데 cleartext 허용 설정이 없음(API 28+ 기본 차단). Manifest의 redirect scheme도 카카오 키 하드코딩(dart-define과 분리됨).
- **잘못된 본문 노출**: 서버 range가 null이면 하드코딩된 "고린도전서 1:10-17"을 오늘 QT처럼 표시(`bible_repository.dart:78-92`) — 말씀 앱에서 가장 위험한 류의 fallback. 제거하고 "준비 중" 표시가 맞음.

---

## 2. High — 데이터 정합·보안·계약 결함

| # | 이슈 | 위치 | 요지 |
|---|------|------|------|
| H-1 | 동시성 → 500 누출 | sharing publish/like, note update, member settings, study publish | `existsBy` 사전검사 후 UNIQUE 위반 시 `DataIntegrityViolationException`을 안 잡아 S0003/S0004/N0002 대신 **500**. praise만 catch함(도메인 간 불일치). 전역 핸들러에 DIVE 매핑 추가 권장 |
| H-2 | 좋아요/댓글 카운터 lost update | `SharingPostService.like/unlike` | COUNT 재계산+dirty checking — 동시 요청 시 카운터 어긋남 영구 누적. purge도 타인 글 카운터 미보정. 원자 UPDATE로 교체 필요 |
| H-3 | AI job RUNNING 고착 복구 부재 | `AiGenerationJobRunner` | 워커 크래시 시 RUNNING 영구 고착 + active_unique_key 때문에 재생성도 차단. started_at 기반 타임아웃 스윕 필요 |
| H-4 | 검증 LLM 호출이 DB 트랜잭션 내부 | `AiGenerationJobRunner`→`AiReviewValidationService` | 30초 LLM 호출이 커넥션 점유. 검증 호출을 tx 밖으로 |
| H-5 | AI job 수행 주체 미기록 | `AiGenerationJob` | DDL `requested_by_admin_id` 컬럼 미매핑, `requestedBy`(SYSTEM_BATCH/admin) 검증 후 **버림** — §7 기록 의무 위반 |
| H-6 | 임시 닉네임 fallback 24자 > VARCHAR(20) | `AuthService:241` | `user_{10자리kakaoId}_{UUID8}` → INSERT 실패 → 잘못된 KAKAO_AUTH_FAILED(401) → 닉네임 선점 시 해당 사용자 가입 불가 |
| H-7 | RSA 개인키 평문 커밋 + 시연 승격 | `src/test/resources/application*.yml` ×2, `.env.example:5` | §8 위반. .env.example이 "시연/로컬은 테스트 키 그대로 사용"이라 안내해 커밋된 키가 런타임 키로 승격. 키 회전 + 시연용 키 생성 스크립트 분리 필요 |
| H-8 | 시연 스택이 무인증 기동 | 루트 `docker-compose.yml:57` + `application-dev.yml` | `SPRING_PROFILES_ACTIVE: dev` → `dev-bypass: true` 기본 → 8080 전체 permitAll + `X-Dev-User-Id` 위장 가능 |
| H-9 | CORS 미설정 — 관리자 웹 차단 | `SecurityConfig` | `.cors()` 미호출 — preflight OPTIONS가 401. `WebConfig`(MVC CORS)는 시큐리티 필터 뒤라 무용. 관리자 웹(별도 오리진) 연동 시 전부 막힘 |
| H-10 | 시큐리티 프로파일 갭 | `SecurityConfig(@Profile("!dev"))` | dev+bypass=false 조합 → 시큐리티 체인 0개 → Boot 기본 폼로그인 폴백(= dev에서 정식 JWT 테스트 불가). local 프로파일은 SecurityConfig 적용인데 `/h2-console` permitAll 없음 → H2 콘솔 401(주석의 TODO 미이행) |
| H-11 | 전역 예외 핸들러 공백 | `GlobalExceptionHandler` | `NoResourceFoundException`(미존재 URL→500), `HttpMessageNotReadableException`(JSON 오류→500), `ConstraintViolationException`(@RequestParam 검증→500), `AccessDeniedException`(@PreAuthorize→500, praise admin에서 실제 발생) 미처리 |
| H-12 | 미션/달력 집계 결함 | `MeditationCalendarService`, `MissionProgressCalculator` | streak이 "오늘 미저장=0"+월경계 절단인데 배치가 04:00 실행이라 STREAK 미션이 사실상 항상 0. 매월 1일 04:00 배치는 새 달 계산 → 전월 말일 활동 영구 미반영. target 변경 시 snapshot/rate 모순. SAVED→SAVED 수정 시 savedAt 재도장(과거 묵상일 소실) |
| H-13 | journal_events 이벤트 기반 미완 | `JournalEventHandler` | AFTER_COMMIT+REQUIRES_NEW(outbox 아님 — 커밋·기록 사이 크래시 시 무흔적 유실 창), FAILED/PENDING **재처리기 0줄**, 이벤트에 previous qtPassageId 없어 증분 집계 불가, 기록 실패가 사용자 500으로 전파 |
| H-14 | study 승인 경로 비대칭 | study/ai | SIMULATOR/GLOSSARY는 APPROVED row를 만드는 use case 자체가 없음(해설만 publish/hide 완비). SIMULATOR 자산 HIDE해도 simulator_clips는 APPROVED 잔존(긴급 차단 불가). DISABLED 상태는 반환 경로 없는 죽은 enum |
| H-15 | CI 게이트 무력화 | `.github/workflows/*` | spectral lint가 루트 `apis/` 검사라 **항상 skip**(실제는 `qtai-server/apis/`), `.spectral.yaml` 부재, **JaCoCo 플러그인 미설정**(§11 명령 자체가 실행 불가, CI는 continue-on-error로 은폐), 금지 번역본 grep이 같은 줄 "KRV" 포함 시 통과, 브랜치명 규칙이 Conventional type(feat/fix)과 불일치 |
| H-16 | Flutter 인증 흐름 잔결함 | `auth_interceptor`, `login_screen` | 신규회원: 토큰 저장 후 authStatus 미전환 → 재시작 시 닉네임 미설정 채 홈 진입. `/auth/**` 401에도 refresh 시도+전역 로그아웃(로그인 실패 안내 소실). refresh 실패 직후 중복 refresh 허용 |

## 3. Medium/Low 대표 (요약)

- **qt**: `@Cacheable` 캐시 key가 주입 Clock 무시(시스템 시계 SpEL) — 테스트·운영 어긋남 위험. 00:00~04:00 구간 캐시 0%(매 요청 DB 3쿼리 — §6 "이전 캐시 제공" 취지와 다름). SU 파서: 단일 절(`3:16`) 파싱 불가, 장 걸침 거부 시 그날 QT 누락(재시도 없음), 영문 권명 정확일치 의존. `@SpringBootTest`가 운영 SU 서버로 실 HTTP 호출 가능(test에서 `QT_TODAY_SUM_ENABLED=false` 미설정 — flaky). SU의 **묵상 제목 문구 저장은 저작권 그레이존 — Lead 판단 필요**(본문 텍스트 미저장은 확인, §8 준수).
- **member**: SUSPENDED에 MEMBER_ALREADY_WITHDRAWN 반환(코드 오용), 닉네임 trim 정책 이중화, 7일 잠금 경계 off-by-one, 재활성화 시 email null 덮어쓰기, purge 대상 조회 인덱스 없음, member_auth_providers CASCADE 미테스트.
- **study**: 사용자 응답에 `aiAssetId`(내부 PK) 노출, FAILED 의미가 "승인본 JSON 파싱 실패"로 왜곡, scene_script_json 승인 시점 검증·크기 제한 0, `columnDefinition TEXT` vs DDL LONGTEXT.
- **ai**: layer2 NEEDS_REVIEW면 재검증 수단이 없어 승인 영구 불가(참조 PDF 미등록 초기 상태에서 전 해설이 막힘) + 시스템 토큰으로 PASSED 로그 위조 가능, `activateForTarget=false` 승인은 publish 경로 영구 상실, checklist `createdByAdminId` null 저장, `ai_prompt_versions` 시드 부재(첫 배포 시 매일 시딩 FAILED), 모니터링 created_at 타임존, 스케줄러 단일 스레드(LLM 지연이 04:00 배치까지 지연), SIMULATOR job 생성 허용 후 즉시 FAILED.
- **notification**: `SendNotificationUseCase` 호출자 0(좋아요/댓글/신고 알림 미연동), `notification_enabled` 설정 미반영, FK 위반과 eventKey 중복을 같은 catch로 침묵.
- **audit**: 조회 화이트리스트가 AI 자산만(체크리스트/참조작업 감사는 기록돼도 조회 불가), before/after_json PII 가드 없음, member purge 무감사.
- **공통 설정**: `JacksonConfig`가 Boot ObjectMapper를 대체해 `spring.jackson.*` 무효(중복·죽은 설정), `RedisConfig`의 `RedisTemplate<String,Object>` 빈 미사용(실사용은 StringRedisTemplate), spring-boot-starter-batch 미사용 의존성, JwtProvider javadoc "refresh 30일" vs 실제 14일, CacheConfig "bibleBooks" 등록만 되고 `@Cacheable` 미부착(66권 매번 DB).
- **Flutter**: success=false 2xx 응답 미처리(Null cast 노출), fontSize 설정 미적용, GowunDodum 폰트 미로딩, 로컬 타임존으로 날짜 계산(해외에서 달력/캐시 키 어긋남 — KST 기준 서버와 불일치), 페이지네이션 미구현(피드/노트/알림), 묵상 4섹션 라벨 의미 뒤바뀜 의심, release 빌드 debug 서명.
- **문서 drift**: ERD가 qt_passages 구조(절 FK/status/published_at)와 전혀 다름, ERD에만 있는 테이블 8종/마이그레이션에만 있는 5종, OpenAPI에 auth/kakao·qt/today 등 핵심 경로 다수 미등재, V9·V10·V11·V23 파일 헤더 주석의 버전 번호 오기.

## 4. 중복·죽은 코드 (삭제/정리 대상)

1. `external/kakao/` 3파일 — **미구현 `@Component`(UnsupportedOperationException)인데 빈 등록됨.** 실사용은 `member/client/kakao` — 주입 실수 시 런타임 폭발. 삭제 권장.
2. `admin/internal/AdminActionLog`+`Repository` — 호출 0건. **audit_logs와 이중 감사 체계** — audit_logs로 단일화 결정 필요.
3. Mock/스캐폴딩 잔존: `ai/client/qt/GetQtUseCaseMock`(@Component로 **프로덕션 빈 등록된 더미**), study `client/bible·member` Mock 스켈레톤+미구현 Study 큐레이션 7파일, praise `Praise/PraiseRepository/Mock` 3파일(주석에 "유튜브 링크" 설계 제안 — §8 지뢰), report/notification의 빈 Mock 2파일.
4. qt의 `QtSimulatorResponse`/`QtStudyContentResponse` TODO 스켈레톤 — study가 같은 엔드포인트 구현 완료. 방치 시 URL 핸들러 충돌 위험.
5. member `MemberSettingsResponse/UpdateRequest`(verseSelectionMode 등) — 실구현 DTO와 **같은 명세 절을 다르게 주장하는** 죽은 계약.
6. `AiLogService` 4개 메서드(락 없는 전이 — 오용 위험), `AiCallLog` 스텁, `requireAuthorizedReviewer` 4중 복붙, admin 인증 헬퍼 3중 복붙, sharing `isCommentsEnabled()` 죽은 메서드, note `findByEventId`/`findByStatus...`(재처리기 부재 방증).
7. Flutter: 카테고리 라벨 매핑 3중, 절 타일 위젯 2중, 미사용 provider 3종, riverpod codegen 의존성 미사용.

## 5. MSA 분리 위험 분석 (핵심)

### 5.1 현재 상태 평가
**코드 레벨 경계는 우수**: 타 도메인 internal import 0건, 크로스 도메인 `@ManyToOne` 0건(전부 Long id), 호출은 api/UseCase 경유 일관, ArchUnit 가드 동작, RS256(공개키 배포로 서비스별 검증) — 분리 친화적 토대는 갖춰져 있음.
**그러나 DB·트랜잭션·이벤트 레벨은 모놀리스 전제에 깊이 결합** — 아래가 실제 분리 차단 요소.

### 5.2 분리 차단 요소 (우선순위순)

1. **SQL 레벨 경계 침범 (즉시 깨짐)** — `qt/internal/QtPassageRepository`의 native SQL 2건이 bible 소유 `bible_books`를 직접 JOIN. ArchUnit이 못 잡는 사각지대. DB 분리 순간 즉사 — **최우선 제거**(bible api에 book 매핑 메서드 추가로 대체).
2. **단일 트랜잭션에 묶인 크로스 도메인 흐름 3곳**
   - member purge: 회원 1명당 sharing→note→praise→mission→notification→report→member 7개 도메인 삭제를 **한 로컬 트랜잭션**으로 — 분리 시 saga(이벤트+멱등 삭제+완료 추적)로 전면 재설계 필요.
   - ai 승인: asset APPROVE + study `verse_explanations` publish가 원자성 묶임 — 분리 시 "승인됐는데 게시 실패" 보상 흐름이 없음.
   - ai 검증: LLM 호출이 tx 내부(H-4) — 분리 전이라도 수정 필요.
3. **이벤트 인프라 토대 부재** — journal_events가 outbox가 아니고(유실 창), 재처리기·외부 소비자 0, 이벤트 스키마에 previous 값 없음(증분 집계 불가). MemberWithdrawnEvent 등은 in-memory ApplicationEvent — 분리 시 브로커 필요한데 Kafka는 금지(§8) → **DB outbox + 폴링 릴레이(또는 Redis Stream)** 로 설계 확정 필요.
4. **동기 호출 그래프의 위험 지점**
   - sharing→member 댓글 닉네임 **N+1 실시간 조회**(C-5): 분리 시 최악 — 닉네임 비정규화 필수.
   - audit: 전 도메인이 동기 직접 호출하는 횡단 관심사 — 중앙 감사 서비스로 가면 전 서비스 가용성이 audit에 결합. **(a) 공통 라이브러리+서비스별 로컬 테이블 vs (b) 비동기 이벤트 수집** 중 결정 필요(현 트랜잭션 원자성 유지엔 (a)가 자연스러움).
   - admin `VerifyAdminRoleUseCase` 동기 검증(C-1 해법 채택 시): JWT claim으로 옮기면 호출은 사라지나 DISABLED 즉시 반영을 잃음 — 인증 서비스 분리 설계와 같은 축에서 결정.
   - member 대시보드가 notification/praise/mission 3도메인 집계(BFF 역할) — 분리 시 게이트웨이/BFF로 이전 후보(위젯별 try-catch 격리는 이미 갖춰져 있어 양호).
   - mission→note 동기 `getCalendar`, report→sharing `getDetail`(존재 확인에 상세 전체 로드 — 경량 exists 포트로 교체).
5. **물리 FK 맵 (분리 시 전부 드랍+애플리케이션 검증 전환 대상)**
   - members ← notes, sharing_posts, comments, post_likes, member_auth_providers(CASCADE), member_praise_songs, member_mission_progress, notifications, reports, admin_users, journal_events, member_settings (**12테이블**)
   - bible ← qt_passages, qt_passage_verses, note_verses, verse_explanations, glossary_terms
   - qt ← notes, journal_events, simulator_clips
   - note ← sharing_posts, journal_events
   - **ai ← glossary_terms.ai_asset_id, simulator_clips.ai_asset_id (FK 있음)** vs verse_explanations.ai_asset_id(FK 없음) — 동일 개념에 정책 비일관. 분리 대비라면 V15도 FK 제거 방향으로 통일.
   - admin ← reports.processed_by_admin_id (현재 잘못된 값이 들어가는 중 — C-7 선결).
6. **공유 식별자 고착**: `bible_verses.id`(Long PK)가 qt/note/study/ai 4개 도메인의 공용 키. 분리 후 bible이 키 체계를 바꾸면 전 서비스 파급 — (book,chapter,verse) 자연키 병기 또는 키 동결 결정 필요. 사용자 응답에 `aiAssetId` 노출(내부 PK 계약화)도 같은 문제.
7. **로컬 파일시스템 의존**: AI 검증 참조 인덱스가 `./restricted`(단일 인스턴스 로컬 경로) — 멀티 인스턴스/분리 시 인덱스 없는 노드는 검증 전부 NEEDS_REVIEW. 오브젝트 스토리지 추상화 필요.
8. **로컬 캐시**: todayQt가 Caffeine(인스턴스 로컬) — 스케일아웃 시 인스턴스 간 불일치. 분리/이중화 시 Redis 캐시 전환 검토(이미 Redis 보유).
9. **시간 정책 SSOT 부재**: 00:00/04:00 게이트가 `QtPassageLookup` 한 곳에만 있고 다른 진입점(getPassage 등)은 무방비(C-8) — 분리 시 게이트 누락이 그대로 외부 계약이 됨. "공개 여부 판정"을 qt의 명시적 단일 책임으로.
10. **Spring Modulith 부재**: CLAUDE.md §1 선언과 달리 의존성 자체가 없음. ArchUnit만으로도 import 경계는 가드되나, 모듈 이벤트/문서화/검증(`ApplicationModules.verify()`)이 없어 분리 준비 도구가 빈약. 도입 권장(분리 전 모듈 경계 리허설 효과).

### 5.3 도메인별 분리 적합도 (현재 코드 기준)

| 도메인 | 적합도 | 비고 |
|--------|--------|------|
| bible | ★★★★★ | import 그래프상 완전 독립(단방향 피참조만). 단 qt의 native SQL 제거 선결 |
| mission | ★★★★☆ | note api 1개만 소비. streak 계약 명문화+배치 커서화 필요 |
| notification | ★★★★☆ | 구조는 1순위인데 소비자 0이라 계약 미검증 — 연동 후 분리 |
| praise | ★★★★☆ | member FK뿐. 죽은 스캐폴딩 정리만 |
| ai | ★★★☆☆ | 워커+폴링은 이식 가능. study publish 원자성·참조 인덱스 파일·RUNNING 스윕 선결 |
| study | ★★★☆☆ | 컴파일 의존은 qt api 2개뿐(좁음). ai FK·승인/숨김 계약 공백 선결 |
| qt | ★★☆☆☆ | bible native SQL, qt_passage_verses 미작성, 공개 게이트 산재 |
| note | ★★☆☆☆ | 이벤트 토대(outbox/재처리기) 전무, 4방향 피호출 |
| sharing | ★★☆☆☆ | member N+1·note 콜백 미구현·카운터 패턴 — 비정규화 선행 필수 |
| member | ★★☆☆☆ | 12테이블 FK 허브 + purge 오케스트레이터(saga 전환 필요) |
| admin/audit | ★☆☆☆☆ | C-1/C-7 의미 결함 정리 + 감사 전략 결정 전에는 분리 불가 |

## 6. 권장 수정 우선순위

- **P0 (기능 동작 회복)**: C-1(ADMIN_ROLE — `VerifyAdminRoleUseCase` 호출 통일 권장), C-2(시딩 날짜), C-3(qt_passage_verses 채움), C-4(서버 연동+Flutter 파싱), C-5(댓글 닉네임 스냅샷/폴백), C-7(processedByAdminId), C-9(iOS 카카오/cleartext/fallback 본문 제거), C-8(공개 게이트).
- **P1 (정합·보안)**: H-1(DIVE→409 매핑), H-2(원자 UPDATE), H-3(RUNNING 스윕), H-5(주체 기록), H-6(닉네임 길이), H-7(키 회전), H-8(시연 보안 결정), H-9(CORS), H-10(프로파일 갭), H-11(예외 핸들러), C-6(노트삭제 연동), 신고 후속 3종 연결, H-16.
- **P2 (MSA 준비 — §5.2 순서대로)**: native SQL 제거 → outbox+재처리기 → 카운터/닉네임 비정규화 → purge saga 설계 → audit 전략 결정 → study↔ai 승인/숨김 계약 보완 → ./restricted 스토리지 추상화 → 캐시 전략 → Modulith 도입.
- **P3 (위생)**: §4 죽은 코드 일괄 삭제, H-15 CI 게이트 복구(spectral 경로·.spectral.yaml·JaCoCo), OpenAPI 미등재 경로 보완, ERD 갱신, H-12 집계 로직 재설계, 문서 오기 정리.

## 7. 한계·비고

- 테스트 일부(순수 단위 테스트의 세부 검증 라인)는 대표 표본 정독, 그 외 영역은 전수 정독.
- "미구현(계획된 WIP)"과 "결함"을 구분함 — F-15 Q&A 스텁, 묵상/설교 노트 작성 화면 부재, FCM 등은 일정 항목이지 버그로 보지 않음(단 §10 필수 테스트 매트릭스상 공백으로는 기록).
- 요구사항 자체 변경이 필요한 항목(SU 제목 저장 그레이존, 삭제 댓글 마스킹 vs 제외, 422 vs 400, 단일 세션 정책)은 임의 수정 없이 **Lead 결정 필요**로 표시.
