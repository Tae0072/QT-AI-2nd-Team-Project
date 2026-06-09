# 작업 리포트 — MSA Day3 QT 오브젝트 스토리지

- **일자**: 2026-06-10
- **작업 폴더(worktree)**: `D:\workspace\QT-AI-ai` (브랜치 feature/msa-qt-object-storage, dev-msa 최신 기준)
- **작업자**: 강태오(Lead, AI 보조)
- **근거**: Notion 회의록 2026-06-09 §2, 계획 `2026-06-09_msa-restart-plan.md`

## 1. 배경
Day2까지 4서비스(user/bible/note/ai) 전부 dev-msa 머지 완료. Day3 진입. 효율·정확성 우선으로 Notion §2의 QT 오브젝트 스토리지(자체 완결·모호함 없음)를 첫 산출물로 선택. (k8s=CI가드 충돌, admin-server=구조결정 필요, RestClient=서비스간 런타임 필요 → 후순위)

## 2. 수행 내용 (service-bible / qt 도메인)
- **스토리지 포트**: `QtSnapshotStore.store(objectKey, json)` (internal).
- **어댑터 2종**(client/objectstorage, `qt.snapshot.storage`로 1개만 활성):
  - 로컬파일(`local`, 기본, matchIfMissing) — 의존성 0, 테스트·로컬에서 항상 동작.
  - S3 호환(`s3`) — AWS SDK v2 S3Client, endpoint override + path-style로 MinIO·R2·S3 동일 코드. 자격증명 env, 로그 금지.
- **스냅샷 생성**: `TodayQtSnapshotService.exportSnapshot(date)` — `findContentContextByDate`(노출정책 STALE_FALLBACK 우회, 내부배치가 어제 본문 스냅샷하는 버그 방지) → member-agnostic `QtDailySnapshot`(date·qtPassageId·title·verseIds·published·generatedAt) → JSON → 포트 저장(키 `yyyy-MM-dd.json`). 본문 없으면 저장 안 함(Optional.empty, 배치 미동작 신호).
- **스케줄러**: `QtDailySnapshotScheduler` 00:00 KST cron, `@ConditionalOnProperty(qt.snapshot.enabled 기본 false)` — 테스트·기동 비활성(service-bible 앱 `@EnableScheduling`은 기존 존재).
- **설정**: application.yml `qt.snapshot.{enabled,storage,local.dir,s3.*}` 모두 env 주입.

## 3. 검증 결과
- `:service-bible:build` BUILD SUCCESSFUL, **65 tests**(신규 3 포함):
  - `TodayQtSnapshotServiceTest`: 본문 존재 시 키/JSON(member-agnostic, draftNoteId·cacheStatus 미포함) / 본문 없음 시 미저장.
  - `QtSnapshotLocalFileStoreTest`(@TempDir): 파일 기록·경로 반환.
  - `QtDailySnapshotSchedulerTest`: KST '오늘'(경계 케이스 15:30Z→익일 KST)로 익스포트 호출.
- AWS SDK v2(s3) BOM 의존 해석·S3 어댑터 컴파일 확인. S3 어댑터는 `storage=s3`일 때만 활성이라 테스트·기동에서 외부 접속 없음.

## 4. 설계 판단·이슈
- **payload v1 = 콘텐츠 컨텍스트**(절 본문 텍스트·시뮬레이터 상태 미포함). 절 본문 enrich는 번역본 저작권 정책(CLAUDE.md §8: 성서유니온/두란노/금지번역본) 검토가 필요해 후속으로 분리. 스냅샷에 검증 참조자료·원문은 담지 않음(§7).
- **member-agnostic 강제**: 공유 정적 파일에 per-user(draftNoteId)·런타임(cacheStatus)이 섞이면 안 됨 → 테스트로 `doesNotContain` 검증.
- **광범위 catch 회피**: 직렬화=JsonProcessingException, 로컬=IOException, S3=S3Exception만 좁게 잡아 공통 예외로 변환.
- 빌드 중 1건: 테스트 `new ObjectMapper()`의 LocalDate 직렬화 실패 → JavaTimeModule+ISO 설정으로 운영(Spring ObjectMapper)과 동일하게 맞춤.

## 5. 다음 단계 (Day3 남은 트랙 — Lead 결정 필요)
- **admin-server 분리**: 구조 결정(모놀리식 통째 복사 vs lib-common 슬라이스) 후 진행. admin 컨트롤러=ai/audit/report/admin 도메인 교차.
- **RestClient 통합**: 각 서비스 cross-domain Mock → RestClient 어댑터(대상 서비스 내부 HTTP 엔드포인트 필요). admin-server 이후.
- **로컬 배포**: CI Requirements Guard가 k8s/helm BLOCK → Docker Compose 채택 또는 가드 완화 결정 필요.
- QT 스냅샷 payload enrich(절 본문/시뮬레이터 상태) — 번역본 정책 검토 후.
