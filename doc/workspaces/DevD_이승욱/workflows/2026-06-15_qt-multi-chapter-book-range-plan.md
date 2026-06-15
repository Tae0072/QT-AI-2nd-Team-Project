# [계획 + Lead 결정요청] QT 본문 수집 — 권/장 교차 범위 지원

작성: 2026-06-15 / DevD 이승욱 / 대상: Lead 강태오 (+ DevA 이지윤)
상태: **구현 보류, Lead 결정 대기** (스키마 변경 + 의도적 제약 제거 포함 → CLAUDE.md §2)

---

## 1. 배경 / 문제

매일 00:02 KST `SuTodayPassageImportScheduler`(service-bible)가 성서유니온 오늘 본문을
HTTP GET + 정규식으로 수집한다. **오늘(2026-06-15) 수집이 실패**했고, 1회용 cron으로
재발사해 원인을 재현했다.

```
2026-06-15T03:45:00.731Z WARN  c.q.d.q.i.SuTodayPassageImportScheduler :
성서유니온 오늘 QT 본문 반영 실패. qtDate=2026-06-15, errorType=BusinessException,
errorMessage=QT 본문 범위는 같은 장 안에서만 저장할 수 있습니다.
```

스케줄러 발사·네트워크·파싱(정규식 매치)까지는 정상이며, 실패는 **"같은 장만 저장"
가드**에서 발생한다. 성서유니온 매일성경은 장을 넘기는 범위(예: `9:1-10:5`)를 자주 내보내며,
그런 날은 본문이 통째로 저장되지 않아 관리자 QT 관리·Today QT·AI 시딩이 모두 비게 된다.

## 2. 근본 원인

현재 데이터 모델과 코드가 **"한 권·한 장 안의 연속 절"** 만 표현하도록 설계돼 있다.

- `SuTodayPassageParser` — `startChapter != endChapter` 이면 `INVALID_INPUT`으로 거부.
- `SuTodayPassage` DTO / `QtPassage` 엔티티 / `qt_passages` 스키마 — 장이 단일 `chapter` 컬럼.
- bible API `GetBibleVerseUseCase.getVerses(bookCode, **chapter**, verseFrom, verseTo)` — 단일 장 전용.
- `TodayQtRangeMapper` — 단일 장 가정으로 사용자 노출 범위를 만든다.

## 3. Lead 결정요청 (먼저 합의 필요)

1. **"같은 장만" 제약 제거 승인** — 의도적으로 넣은 가드라 제거는 제품/요구사항 성격.
   `07_요구사항_정의서.md` / `25_기능_명세서.md`의 QT 본문 정의에 "장 교차 허용"을 명시할지.
2. **`qt_passages` 스키마 변경 승인 및 소유** — Flyway는 admin-server 단독 소유(CLAUDE.md).
   변경안(§4.1)과 마이그레이션 주체/일정 합의.
3. **권(book) 교차 범위 처리 방침** — 본 작업은 "권+장 교차까지"로 요청됐으나, **성서유니온
   매일성경 본문 표기는 권 교차를 표현하지 않는다**(한 권 안에서만 장이 넘어간다). 즉 권 교차는
   현재 수집 소스에 데이터 경로가 없다. → 스키마는 권 교차까지 대비해 두되(start/end book),
   파서·검증은 우선 "같은 권 내 장 교차"만 활성화하고 권 교차는 **방어적 거부 + 경고 로그**로
   둘지(권장), 아니면 완전 구현할지 결정 요청.

## 4. 제안 설계 (권+장 교차 대비, 장 교차 우선 활성화)

### 4.1 스키마 — admin-server, 신규 `V45__qt_passages_multi_chapter_range.sql`

`qt_passages`를 범위 모델로 확장한다. 호환을 위해 기존 컬럼을 시작값으로 승계한다.

| 변경 | 내용 |
|---|---|
| ADD | `start_book_id SMALLINT`, `end_book_id SMALLINT` (둘 다 `bible_books(id)` FK) |
| ADD | `start_chapter SMALLINT`, `end_chapter SMALLINT` |
| 백필 | `start_book_id=book_id`, `end_book_id=book_id`, `start_chapter=chapter`, `end_chapter=chapter` |
| 유지 | `start_verse`, `end_verse`(권/장 교차 시 "시작 권·장의 시작 절", "종료 권·장의 종료 절" 의미) |
| 정리 | 기존 `chapter`, `book_id`, FK `fk_qt_passages_book`은 백필·코드 전환 후 후속 마이그레이션에서 제거(2단계, 무중단) |

`qt_passage_verses`(실제 절 id 목록 + display_order)는 **이미 장/권과 무관하게 임의 절 집합을
표현**할 수 있어 스키마 변경이 필요 없다. AI·시뮬레이터·학습 콘텐츠는 이 매핑(verseIds)을
소비하므로(아래 §4.4) 핵심은 매핑을 교차 범위로 정확히 채우는 것이다.

### 4.2 파서 / 정규식 — `SuTodayPassageParser` (service-bible 원본, admin 복사본 동기화)

- `startChapter != endChapter` 거부 로직 제거.
- 기존 정규식은 이미 종료 장 그룹을 캡처한다:
  `([가-힣0-9ⅠⅡⅢ]+)\s*\(([^)]+)\)\s*(\d{1,3})\s*:\s*(\d{1,3})\s*-\s*(?:(\d{1,3})\s*:\s*)?(\d{1,3})`
  → 같은 권 내 장 교차(`9:1 - 10:5`)는 그대로 파싱 가능.
- 권 교차는 소스 표기가 없으므로 **기본 미지원**. 만일 두 권 표기를 만나면(향후) 방어적으로
  명시 거부하거나 별도 패턴을 신설(Lead 결정 §3).

### 4.3 DTO / 엔티티

- `SuTodayPassage`: `chapter` → `startChapter` + `endChapter` (+ 향후 `startBookName`/`endBookName` 대비).
- `QtPassage`(service-bible 원본, admin 복사본): `chapter` → `startChapter`/`endChapter`,
  `bookId` → `startBookId`/`endBookId`. `create`/`updateRange` 시그니처 변경.

### 4.4 절 매핑 — `QtTodayPassageImportService.replaceVerseMappings`

bible API가 단일 장 전용이므로 **장(권) 단위로 분할 호출**해 verse id를 모은다.

```
for (chapter = startChapter; chapter <= endChapter; chapter++):
    from = (chapter == startChapter) ? startVerse : 1
    to   = (chapter == endChapter)   ? endVerse   : <해당 장 마지막 절>
    verses += getBibleVerseUseCase.getVerses(bookCode, chapter, from, to)
```

- "해당 장 마지막 절"이 필요하므로, bible API에 **장 전체 절을 주는 메서드 또는 장 교차 range
  메서드 신설**이 더 깔끔(타 도메인 `api/UseCase` 변경 → DevA 협의). 대안으로 `verseTo=null`을
  "장 끝까지"로 해석하는 기존 동작 확인 후 활용.
- 모은 verseIds로 `qt_passage_verses` 교체 저장(기존 로직 재사용). **AI 시딩은 verseIds만
  소비**(`CommentaryMaterialService.findPromptContextByVerseIds`)하므로 매핑만 맞으면 AI 영향 없음.

### 4.5 referenceText 포맷

- 같은 장: `고린도전서(1 Corinthians) 9:1-23` (현행 유지)
- 장 교차: `고린도전서(1 Corinthians) 9:1-10:5`
- (권 교차 활성화 시) `유다서(Jude) 1:20 - 요한계시록(Revelation) 1:3` 형태 규칙 별도 합의.

### 4.6 사용자 노출 범위 — `TodayQtRangeMapper` / `TodayQtRangeResolver` / `TodayQtRangeResponse`

단일 `chapter` 가정 제거. 응답 DTO에 시작/종료 장(권)을 담도록 확장하고, Flutter 앱
범위 표기와의 계약(`04_API_명세서.md`) 변경분을 DevA와 동기화.

### 4.7 admin-server 동기화

도메인 로직(파서·엔티티·import·mapper)은 **service-bible이 원본**, admin-server 복사본이
따라간다. **Flyway 마이그레이션은 admin-server에만** 추가(V45). `doc/admin-server-sync-rules.md` 준수.

## 5. 영향도 (파일)

| 영역 | 파일 |
|---|---|
| 파서 | `service-bible|admin-server …/qt/client/sum/SuTodayPassageParser.java`, `SuTodayPassage.java` |
| 엔티티 | `…/qt/internal/QtPassage.java` (양쪽) |
| import | `…/qt/internal/QtTodayPassageImportService.java` (양쪽) |
| 범위 노출 | `…/qt/internal/TodayQtRangeMapper.java`, `TodayQtRangeResolver.java`, `…/qt/api/dto/TodayQtRangeResponse` |
| bible API | `…/bible/api/GetBibleVerseUseCase.java` (장 교차/장끝 메서드 — DevA 협의) |
| 스키마 | `admin-server …/db/migration/V45__qt_passages_multi_chapter_range.sql` (+ 2단계 정리 마이그레이션) |
| 테스트 | 파서/­import/­mapper/­ArchUnit·Modulith |

## 6. 테스트 계획 (CLAUDE.md §10)

- 파서: 같은 장 / 장 교차 정상 파싱, (권 교차) 정책대로 거부 또는 파싱.
- import: 장 교차 시 `qt_passage_verses`가 전 범위 절을 빠짐없이 채우는지(장 경계 포함).
- mapper: 장 교차 범위의 사용자 노출 응답.
- 회귀: 단일 장 본문 기존 동작 보존, `qt_date` UNIQUE/upsert 유지.
- 경계: 00:02 수집 → 00:05 AI 시딩 전제(오늘 본문 존재) 유지.

## 7. 마이그레이션 · 백필 · 롤백

- 신규 컬럼은 NULL 허용으로 추가 후 백필 → NOT NULL 승격(무중단 2단계).
- 기존 `chapter`/`book_id` 제거는 코드 전환 배포 후 별도 마이그레이션(롤백 안전).
- 롤백: V45는 컬럼 추가만이라 역호환. 코드 미배포 상태에서도 기존 컬럼으로 동작 유지.

## 8. 검증 명령 (CLAUDE.md §11)

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test jacocoTestReport
./gradlew -p qtai-server jacocoTestCoverageVerification
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
```

## 9. 단계별 실행 계획 (승인 후)

1. `dev` 최신화 → `feat/qt-multi-chapter-range` 브랜치 생성.
2. admin-server V45 마이그레이션(컬럼 추가 + 백필).
3. service-bible: 파서·DTO·엔티티·import·mapper 수정 + bible API 협의 변경.
4. admin-server 복사본 동기화.
5. 테스트 추가/갱신 → §8 검증.
6. PR(대상 `dev`, 10 files / 500 lines 가이드 고려 시 2개로 분할: ①스키마+엔티티 ②파서+import+매핑) + F-ID 명시.

## 10. 리스크 / 미해결

- **권 교차**: 수집 소스에 데이터 경로 없음 → 스키마만 대비, 활성화 여부 Lead 결정(§3).
- bible API 변경은 타 도메인(DevA) 계약 변경 → 사전 협의 필요.
- `04_API_명세서.md` Today QT 범위 응답 계약 변경 시 Flutter 동기화 필요.
- 현재 로컬 스택 컨테이너는 테스트 cron 이미지 상태 → 운영 00:02 복구 위해 service-bible 재배포 1회 필요(별도).

근거: CLAUDE.md §2(요구사항 변경 Lead 검토)·§3(도메인 경계)·§4·§5·§10·§11,
`doc/admin-server-sync-rules.md`, 2026-06-15 수집 실패 로그.
