# Report — 2026-06-16 admin-qt-video-clip-trigger

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/admin-qt-video-clip-trigger |
| 작업 패널 | QT영상 — 관리자 등록 QT 연동(절 매핑 + 클립 자동 준비) |

## 배경
QT영상 클립은 오늘 QT의 절 매핑(`qt_passage_verses`) → 절별 타임코드(`bible_verse_video_segments`)로 `[min,max]` 구간을 잘라 만든다(`qt_video_clips`). 그런데 **관리자페이지 등록 경로**에 두 군데가 끊겨 있었다.
1. **절 매핑 미생성:** 자동수집(`QtTodayPassageImportService`)만 `qt_passage_verses`를 채우고, `AdminQtPassageService`는 본문 행만 저장 → verseIds가 비어 클립이 MISSING.
2. **트리거 부재:** 관리자 등록을 클립 준비로 잇는 경로가 없었다.

## 설계 결정 (DevE #686 위에서 재설계)
처음엔 service-bible 주기 스케줄러(2b)로 풀었으나, **DevE의 #686(admin QT영상 관리 AD-20)** 이 dev에 머지되며 admin-server에 `qtvideo` 도메인(`prepareClip` 포함)이 생겼다. 이에 맞춰 **더 나은 방식인 "관리자 등록/게시 시 인-프로세스 자동 준비"** 로 전환했다.
- admin-server가 같은 프로세스·공유 DB에서 `prepareClip`을 직접 호출 → **폴링 스케줄러 불필요, 크로스서비스 호출 불필요, 즉시 반영**.
- 폐기한 스케줄러는 수동 클립(DevE 방식 B)을 5분마다 덮어쓰는 충돌이 있었음 → 인-프로세스 트리거는 등록/게시 시에만 동작해 수동 클립과 공존.

## 변경 내용
**① 절 매핑 (admin-server)**
- (신규) `domain/qt/internal/AdminQtPassageVerseMapper.java`: 본문 커밋 후 범위(장 교차 포함)로 `bible_verses` 조회 → `qt_passage_verses` 교체 저장. 절 조회는 read 트랜잭션(예외 격리), 쓰기는 **프로그래매틱 REQUIRES_NEW**(afterCommit 커밋 함정 회피). bible 조회는 admin 내부 도메인이라 외부 호출 없음.

**② 인-프로세스 자동 준비 (admin-server)**
- (신규) `domain/qt/internal/AdminQtVideoAutoPreparer.java`: 본문 커밋 후 ①매핑 → (요청 시)클립 준비를 순서대로 실행. 클립 준비는 REQUIRES_NEW로 커밋, best-effort.
- (신규) `domain/qtvideo/api/PrepareQtVideoClipUseCase.java`: DevE `AdminQtVideoService.prepareClip`을 도메인 경계(api)로 노출 — `domain.qt`가 `domain.qtvideo.internal`을 직접 import하지 않도록.
- (수정) `domain/qtvideo/internal/AdminQtVideoService.java`: `implements PrepareQtVideoClipUseCase`(+`@Override`). 로직 변경 없음(DevE prepareClip 재사용).
- (수정) `domain/qt/internal/AdminQtPassageService.java`: create=절매핑만(미공개), update/publish=절매핑+클립 자동 준비. 트리거 시점 = **게시 + 수정(공개 본문)**.

**제거 (service-bible)**
- (삭제) `domain/qtvideo/internal/QtVideoDailyPreparationScheduler.java`(+test): 인-프로세스 트리거로 대체. import 경로는 service-bible 기존 이벤트 리스너가 이미 자동 준비하므로 영향 없음.

**테스트**
- (신규) `AdminQtPassageVerseMapperTest`, `AdminQtVideoAutoPreparerTest`.
- (수정) `AdminQtPassageServiceTest`, `AdminQtPassageCacheEvictTest`: autoPreparer 주입 반영.

## 검증 결과
- **gradle 빌드(로컬 docker)**: `:admin-server:clean :service-bible:clean :*:bootJar` → **BUILD SUCCESSFUL**(compileJava 실제 실행). 변경 코드 전부 컴파일 통과.
- **배포**: 신선 jar로 admin-server·service-bible 이미지 재빌드 후 재기동(healthy).
- **엔드투엔드(실데이터, 인-프로세스 즉시)**: 오늘 QT(passage 9, 고린도전서).
  - 관리자 PATCH `10:14-11:1 → 9:1-7` → `qt_passage_verses` **7개로 갱신** + `qt_video_clips` **[1770,1840]=70초로 즉시 생성**(스케줄러/재시작 없이, updated_at=수정 직후). ① + ② PASS.
  - 원복 PATCH `→ 10:14-11:1` → 매핑 **21개(장 교차)** + 클립 **[2170,2380]=210초** 즉시 복원. 장 교차 라운드트립 PASS.
  - service-bible 로그에 옛 스케줄러 호출 **0건**(제거 확인).
- 금지 패턴(신규 파일): SSE/Kafka/javax/RAG/번역본/가사·음원/Anthropic 등 **없음**.
- 미실행: 단위 테스트(`:*:test`)는 미실행 → 머지 전 로컬 실행 권장.

## 확인한 금지선
- AI 자유 챗봇/SSE/RAG 없음 / Kafka·K8s·Helm 없음 / 금지 번역본·가사·음원·YouTube URL 저장 없음 / `javax.*` 미사용 — 모두 해당 없음.
- 담당 범위: QT영상 파트(본인 담당). DevE #686과 중복되던 부분은 **DevE 구현을 재사용**(prepareClip)하고 충돌 스케줄러는 제거해 정리.

## 남은 리스크
- **단위 테스트 미실행.** 빌드·E2E는 통과했으나 `:admin-server:test :service-bible:test`는 머지 전 로컬 실행 필요.
- **커밋 위생:** `git add -A` 금지. 본 작업 파일만 add(추가 5 / 수정 4 / 삭제 2). 워크플로·리포트 md 동반.
- **트리거 범위:** update는 공개 본문일 때만 클립 준비(prepareClip 게이트). 미래 날짜 본문은 게시일에 준비됨(제품 일배치 모델과 일치).
- **세그먼트 범위:** `bible_verse_video_segments`가 고린도전서만 등록 → 다른 권 QT는 MISSING(정책상 정상, 별개 작업).
- **수동 클립 공존:** 인-프로세스 자동 준비는 등록/게시 시에만 동작하므로 평상시 DevE 방식 B 수동 클립을 덮어쓰지 않음. 단, 같은 본문을 다시 update/publish하면 자동 준비가 재생성(의도된 동작).
