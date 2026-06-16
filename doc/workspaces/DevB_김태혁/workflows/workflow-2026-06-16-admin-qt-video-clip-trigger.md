# Workflow — 2026-06-16 admin-qt-video-clip-trigger

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/admin-qt-video-clip-trigger |
| 작업 패널 | QT영상 — 관리자 등록 QT 연동(절 매핑 + 클립 준비 트리거) |
| 기능 ID | QT-AI / qtvideo(QT영상), qt(오늘 QT 관리) |
| 기준 문서 | 03_아키텍처_정의서, 04_API_명세서, 07_요구사항_정의서, doc/admin-server-sync-rules.md |

## 작업 목표
- 외부 import(성서유니온)를 쓰지 않고 **관리자페이지에서 등록한 오늘 QT**만으로 QT영상이 동작하게 만든다.
- 끊김 ① 해결: 관리자 QT 등록/수정 시 절 매핑(`qt_passage_verses`)을 자동수집과 동등하게 채운다. (현재는 본문 행만 저장돼 verseIds가 비어 클립이 MISSING)
- 끊김 ② 해결: service-bible가 관리자 등록을 인지해 클립을 준비하게 한다. admin-server는 standalone(외부호출 인프라 없음)이라, 2a(HTTP push) 대신 **2b(service-bible 주기 스케줄러)**로 간다.
- 출처(자동수집/관리자 등록) 무관하게 동작하고, 클립 준비는 멱등(본문당 락 + 활성 1개)이라 반복 호출에 안전해야 한다.

## 수정 예정 경로
- (신규) `qtai-server/admin-server/.../domain/qt/internal/AdminQtPassageVerseMapper.java` — 등록/수정 커밋 후 절 매핑 채우기(QtPassageWriter 재사용, bible 조회는 내부 도메인)
- (수정) `qtai-server/admin-server/.../domain/qt/internal/AdminQtPassageService.java` — create/update에서 매퍼 호출(커밋 후)
- (신규) `qtai-server/service-bible/.../domain/qtvideo/internal/QtVideoDailyPreparationScheduler.java` — 주기적으로 `prepareToday()` 호출(이벤트 의존 제거, 2b)
- (수정/신규 테스트) `AdminQtPassageVerseMapperTest`, `QtVideoDailyPreparationSchedulerTest`, `AdminQtPassageServiceTest`, `AdminQtPassageCacheEvictTest`

## 검증 계획
- `./gradlew -p qtai-server :admin-server:test :service-bible:test`
- `./gradlew -p qtai-server build`
- `git diff --check` (단, 워킹트리에 EOL 정규화 노이즈가 있어 변경 파일만 좁혀 확인)
- 금지 패턴 검색(SSE/Kafka/javax/RAG/번역본 등) — 신규 파일 통과 확인
- 정적 점검(중괄호 균형, DTO 시그니처 일치) — 완료

## 막힌 점
- 작업 샌드박스에서 빌드 검증 불가(Java 11 + gradle 배포 다운로드 네트워크 차단) → **로컬 빌드/테스트 필요**.
- 워킹트리에 줄바꿈(CRLF/LF) 정규화로 2,800여 파일이 M으로 표시됨 → 커밋 시 본 작업 7개 파일만 `git add` 해야 함.
- 영상 세그먼트(`bible_verse_video_segments`)는 현재 고린도전서만 존재 → 다른 권 QT는 매핑은 되지만 영상은 MISSING(정상 동작, 정책상 별개).

## 구현 변경 (2026-06-16, dev pull 이후)
- DevE #686(admin QT영상 관리)이 dev에 머지되며 admin-server에 `qtvideo` 도메인(`prepareClip`)이 생김.
- ②를 **service-bible 주기 스케줄러(2b) → admin-server 인-프로세스 자동 준비**로 전환:
  - 등록/게시 커밋 후 ①절 매핑 → DevE `prepareClip`(인-프로세스, REQUIRES_NEW) 호출.
  - `qtvideo/api/PrepareQtVideoClipUseCase`로 도메인 경계 노출, `AdminQtVideoAutoPreparer`가 오케스트레이션.
  - 트리거: **게시 + 수정(공개 본문)**. 폴링 스케줄러 제거(수동 클립 클로버·폴링비용 해소).
- E2E: 수정→매핑7+클립70초 즉시, 복원→매핑21+클립210초 즉시 (스케줄러/재시작 불필요) 확인.
