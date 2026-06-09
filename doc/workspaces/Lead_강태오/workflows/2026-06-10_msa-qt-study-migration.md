# 2026-06-10 MSA service-bible qt·study 도메인 이전 워크플로우 (PR #2)

> 작업 브랜치: `feature/msa-qt-study` (origin/dev-msa 기준)
> 기준 PR: `2026-06-09_msa-multimodule-foundation.md` (PR #1, mergeCommit `2bac476`)
> 목표: 모놀리식의 `qt`·`study` 도메인을 `service-bible` 모듈로 Strangler 이전 (모놀리식 원본 유지)

## 목표

PR #1에서 골격(lib-common + service-bible)과 `bible`·`music`·`praise`를 이전했다. PR #2는
읽기전용 콘텐츠 서비스의 핵심인 `qt`(오늘의 QT)와 `study`(해설·시뮬레이터)를 이전한다.

- `qt`: `api`/`internal`/`web`/`client` 이전
  - `bible.api` 의존 → service-bible 안에 있으므로 in-process
  - `note.api` 의존 → 다른 서비스(service-note)라 **계약 타입만 가져오고 호출자 client에 Mock**
  - 외부 성서유니온 클라이언트(`SuTodayBibleClient`)
  - 스케줄러(`SuTodayPassageImportScheduler`)·시더(`LocalTodayQtSeeder`)는 테스트에서 비활성
- `study`: `api`/`internal`/`web` 이전. 의존은 `qt.api`(in-process)뿐.

## TODO (순서대로 진행)

- [x] **0. 분기** — origin/dev-msa(2bac476)에서 `feature/msa-qt-study` 생성 (prefix는 `feature/` 필수)
- [x] **1. 분석** — qt/study 의존성 전수 조사. 실제 cross-service 의존은 `note.api` 하나뿐임을 확인
      (qt↔study는 둘 다 같이 이전돼 in-process, `ai`/`member`는 빈 placeholder stub)
- [x] **2. qt 이전** — qt 트리 전체 + `note.api` 최소 계약(7파일) + `client/note/GetNoteUseCaseMock`
- [x] **3. study 이전** — study 트리 전체 (simulator/glossary/verse-explanation 포함)
- [x] **4. 게이팅** — `SuTodayPassageImportScheduler`에 `@ConditionalOnProperty`(테스트 비활성),
      앱에 `@EnableScheduling`, `LocalTodayQtSeeder`는 `@Profile("local")`로 이미 비활성
- [x] **5. PR #1 이월 품질**
  - [x] 표준 페이징 envelope `PageResponse<T>` (lib-common) → `PraiseController`가 `Page<T>` 대신 사용
  - [x] 인증 헬퍼 `AuthenticationSupport.requireMemberId` (lib-common) → qt/study 컨트롤러 공통화
  - [x] `SecurityConfig`에 인증 실패 401 / 인가 실패·denyAll 403 표준 응답 연결
- [x] **6. 테스트**
  - [x] Controller MockMvc 통합테스트(qt/study + bible/music/praise): 미인증 401, 인증 200, admin denyAll 403
  - [x] 도메인 단위테스트: `QtServiceTest`(7), `QtStudyAvailabilityServiceTest`(3)
  - [x] ArchUnit 경계테스트: 타 도메인 `internal` import 금지 규칙으로 교체
- [x] **7. 빌드 검증** — 호스트 gradlew `:service-bible:build` GREEN (33 tests, 0 fail)
- [x] **8. 문서** — 워크플로/리포트/스터디노트 작성
- [x] **9. 커밋·푸시·PR** — base=dev-msa, head=feature/msa-qt-study (PR #424)
- [x] **10. 리뷰 대응** — claude-review REQUEST_CHANGES BLOCK 3건 수정 후 재리뷰

## PR #2 범위

service-bible로 `qt`·`study` 이전 + lib-common 공통 유틸 2종(PageResponse, AuthenticationSupport)
+ SecurityConfig 보강 + 테스트 3종. base `dev-msa` 대상 PR.

## 진행 메모

- **샌드박스 마운트 truncation 함정(중요)**: 샌드박스(Linux) 마운트가 일부 모놀리식 원본 파일을
  잘린(stale) 상태로 보여줬다. 그 상태로 `cp` 하면 잘린 사본이 디스크에 생겨 컴파일이
  "reached end of file" 로 깨진다. → **파일 복사는 반드시 호스트(PowerShell `robocopy`)로**
  수행해 실제 디스크 바이트를 복사한다. 내가 직접 작성하는 새 파일(heredoc/Write)은 정상.
- **Edit 도구 NUL 함정**: 파일을 더 짧게 덮어쓰면 끝에 NUL 바이트가 남는다 → 새 파일/재작성은
  bash heredoc(`cat >`)이나 충분히 긴 내용으로 처리하고, 작업 후 NUL 개수를 점검.
- **빌드 락**: Cursor IDE가 `build/test-results`를 잠가 `Unable to delete directory`가 뜨면
  `--no-daemon` 빌드에서는 `--stop` 없이 `service-bible/build/test-results`만 삭제 후 재시도
  (다른 세션 빌드에 영향 없음). 다른 세션이 데몬 빌드 중일 수 있어 `--stop`은 피한다.
- **순환 없음**: `qt → study.api(QtStudyAvailabilityService, 리포지토리만 의존)`, `study(Content/Simulator) → qt.api`.
  Availability가 qt를 역호출하지 않아 빈 순환이 없다(모놀리식 구조 그대로 보존).

## 검증 결과

호스트 `:service-bible:build` 통과. 테스트 32개(0 실패/0 에러/0 스킵):
BibleServiceApplicationTest 1 · ControllerSecurityIntegrationTest 4 · BibleServiceTest 6 ·
MusicTrackServiceTest 2 · PraiseServiceTest 6 · QtServiceTest 5 · QtStudyAvailabilityServiceTest 3 ·
DomainBoundaryTest(ArchUnit) 1.

## 리뷰 대응 (claude-review v3.1 REQUEST_CHANGES → 수정)

첫 리뷰가 BLOCK 3건으로 REQUEST_CHANGES. 모두 수정 후 재리뷰 요청:

1. **PraiseController `@PreAuthorize("hasRole('ADMIN')")` 단독** → `@PreAuthorize("denyAll()")`.
   admin_role 이중검증 없이 ADMIN만으로 열리는 회귀 위험 제거(SecurityConfig denyAll + 메서드보안 이중 차단).
2. **`QtService.resolveDraftNoteId` `catch (Exception e)`** → `catch (RuntimeException e)`.
   CLAUDE.md §9 광범위 catch 금지. note 호출은 검사 예외 없음.
3. **`QtService.getPassage` `cacheStatus="HIT"` 하드코딩** → `"DIRECT"`.
   ID 직접 조회는 todayQt 캐시 미경유라 HIT가 거짓. `TodayQtResponse` javadoc에 DIRECT 문서화.
   `QtServiceTest`에 미래 본문 차단 + DIRECT 라벨 테스트 추가(5→7건).

WARN/INFO 지적(QtPassageLookup 4상태 테스트, 파서 테스트 등)은 비차단이라 후속 PR 과제로 남김.

## 커밋 / PR

- PR: #424 (https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/424)
- 커밋: `d6a368f`(이전 본체) + 리뷰 대응 커밋(아래 push)
