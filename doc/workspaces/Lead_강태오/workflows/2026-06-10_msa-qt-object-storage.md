# 2026-06-10 MSA Day3 — QT 오브젝트 스토리지 워크플로우

> 작업 폴더(worktree): `D:\workspace\QT-AI-ai` (브랜치 `feature/msa-qt-object-storage`, dev-msa 최신 기준)
> 근거: Notion 회의록 2026-06-09 §2 "QT 콘텐츠를 날짜별 JSON으로 오브젝트 스토리지에 제공".
> 대상 모듈: `service-bible`(qt 도메인).

## 배경·Day3 트랙 판단
Day3 = ① RestClient 통합 ② 관리자 서버 분리 ③ 로컬 k8s + ④ QT 오브젝트 스토리지. 효율·정확성 우선으로 **자체 완결·모호함 없는 QT 오브젝트 스토리지를 먼저** 진행.
- ⚠️ **로컬 k8s는 현재 머지 불가**: CI Requirements Guard #6이 `k8s/helm 디렉터리`를 BLOCK("v1은 Docker Compose만"). Notion("로컬 k8s 마무리")와 충돌 → Lead가 Docker Compose 채택/가드 완화 중 결정 필요.
- RestClient 통합은 admin-server(audit/admin 호스트)와 대상 서비스의 내부 HTTP 엔드포인트가 필요 → 후순위.
- admin-server는 "모놀리식 통째 복사 vs lib-common 슬라이스" 구조 결정 필요(Lead 확인) → 후순위.
- Kafka는 회의록대로 **AI 영역만 유지**(이 트랙은 Kafka 없음).

## TODO
- [x] QT 스냅샷 대상 데이터·기존 스케줄링 파악 — `GetQtPassageContentContextUseCase.findContentContextByDate`(member-agnostic), service-bible 앱에 `@EnableScheduling` 이미 존재.
- [x] 스토리지 포트 + 어댑터 — `QtSnapshotStore`(internal) / 로컬파일(기본, 의존성 0) / S3(AWS SDK v2, MinIO·R2 호환). `qt.snapshot.storage`(local|s3) 프로퍼티로 정확히 1개 활성.
- [x] 스냅샷 생성 서비스 — `TodayQtSnapshotService`: 노출정책(STALE_FALLBACK) 우회 날짜 조회 → member-agnostic JSON(draftNoteId·cacheStatus 제외) → 포트 저장(키 `yyyy-MM-dd.json`).
- [x] 스케줄러 + 설정 — `QtDailySnapshotScheduler` 00:00 KST, `@ConditionalOnProperty(qt.snapshot.enabled 기본 false)`로 테스트·기동 비활성. application.yml에 storage/bucket/endpoint/keys(env).
- [x] 테스트 + 빌드 — 직렬화(포트 mock)·로컬기록(@TempDir)·스케줄러(KST 날짜). `:service-bible:build` 통과(65 tests).
- [ ] 문서 + PR — 워크플로우·리포트·스터디노트. feature/ → dev-msa PR, 첫푸시 APPROVE 체크리스트 적용, 자동머지.

## 핵심 원칙 (첫푸시 APPROVE)
- 푸시 전 origin/dev-msa 병합(병렬 트랙 충돌 회피). 브랜치 `feature/`.
- Requirements Guard 금지토큰 회피(이 트랙은 Kafka/SSE 무관). 광범위 catch 금지(JsonProcessingException/IOException/S3Exception 분기). 키/시크릿 env·로그 금지.
- @ConditionalOnMissingBean on @Component 금지(프로퍼티 게이트 사용). 스케줄러 게이트로 테스트 비활성.

## 진행 메모
- 2026-06-10: 기능·테스트 커밋. 막힌 점 1건 — 테스트의 `new ObjectMapper()`가 `LocalDate`(JavaTimeModule 미등록) 직렬화 실패. 운영은 Spring 주입 ObjectMapper(모듈 등록)라 정상이고, 테스트만 JavaTimeModule+WRITE_DATES_AS_TIMESTAMPS off로 맞춤.
- 스냅샷 payload v1 = 콘텐츠 컨텍스트(날짜·passage·title·verseIds·published). 절 본문 텍스트/시뮬레이터 상태 enrich는 후속(저작권·번역본 정책 검토 포함).
