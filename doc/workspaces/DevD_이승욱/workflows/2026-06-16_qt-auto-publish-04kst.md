# 2026-06-16 미게시 자동수집 본문 04:00 자동게시 + 기동 catch-up (feature/qt-auto-publish-04kst)

## 목표·배경
QT 공개 정책(CLAUDE.md §6): 공개 00:00 KST, 사용자 노출/cache refresh 04:00 KST(00:00~04:00은 이전 캐시).
자동수집(00:02) 본문이 게시 시각(04:00)에 맞춰 게시되고, 서버가 04:00에 안 돌아 누락되면 기동 시 보강(catch-up)하도록 한다.

**모델 A 선택**: 수집 본문을 수집 즉시 노출하지 않고 '미게시'로 두고, 04:00에 자동게시한다(정책에 정확히 부합).
이 브랜치는 `feature/admin-qt-management-ux`(수집 시각 `collected_at` 컬럼) 위에서 작업하며, 그 브랜치의
"수집 즉시 ACTIVE·게시"를 "수집=미게시 → 04:00 게시"로 조정한다.

## 작업 내용
### ① 수집 → 미게시(PENDING) 전환 (양 모듈)
- `QtPassage.scheduleForAutoPublish()`: status=PENDING_REVIEW, publishedAt=null.
- `QtPassageWriter.upsert`: 신규 수집 본문은 `scheduleForAutoPublish()`로 미게시, 수집 시각만 기록(게시 시각은 게시 때). 재수집(updateExisting)은 상태 보존.

### ② 04:00 자동게시 + catch-up (service-bible)
- `QtPassageRepository.findAutoPublishTargets(status, cutoff)`: status=PENDING_REVIEW + `collected_at` 있음(=자동수집) + qtDate ≤ cutoff. 관리자 수동 등록(collectedAt null)은 제외(검토 게이트 유지).
- `QtPassageAutoPublishService.publishDue()`: cutoff = (now < 04:00 ? 어제 : 오늘). 대상 본문을 `publish(qtDate 04:00)`로 게시(ACTIVE + 게시 시각=그 날짜 04:00). 누락분(과거 날짜)도 함께 처리.
- `QtPassageAutoPublishScheduler`: `@Scheduled(0 0 4 * * *, Asia/Seoul)` + 기동 `@EventListener(ApplicationReadyEvent)` catch-up이 같은 `publishDue()` 공유. `@ConditionalOnProperty(qt.auto-publish.enabled, matchIfMissing=true)`.

### ③ 클립 준비 재트리거
- 클립 준비(`QtVideoClipPreparationService`)는 미게시 본문을 skip한다. 모델 A에선 수집 시점엔 미게시라 클립이 안 만들어지므로, **자동게시 시 `QtPassageVerseMappingsChangedEvent`를 재발행**해 게시된 본문의 클립 준비를 트리거한다.

## 범위/주의
- **노출 동작**: `getToday`는 캐시(`passageLookup.findTodayPassage()`, STALE/EMPTY 폴백)를 거치므로 04:00 전 미게시여도 정상 폴백(이전 캐시) — §6에 오히려 더 부합. `isVisibleToUsers`(status==ACTIVE)는 불변.
- **상태 표기**: 자동수집 미게시 본문은 관리자 목록에 '검토 대기'(PENDING_REVIEW)로 보이다가 04:00 게시 후 '게시됨'. 04:00 자동게시는 자동수집(collectedAt 있음)만 대상이라 관리자 수동 등록의 검토 게이트는 유지.
- **브랜치 스택**: 본 브랜치는 `feature/admin-qt-management-ux` 기반. 그 PR이 먼저 머지되면 dev로 rebase 필요(또는 본 PR이 포함).
- **테스트 게이트**: `qt.auto-publish.enabled=false`(테스트 application.yml)로 기동 시 자동게시가 데이터를 건드리지 않게 차단.

## 검증
- `./gradlew :service-bible:test :admin-server:test` 전부 BUILD SUCCESSFUL.
- `QtPassageAutoPublishServiceTest`: 04:00 이후 오늘까지 게시+게시시각=그날짜04:00+이벤트 발행, 04:00 이전 어제까지만 대상.
- `QtPassageWriterTest`(양 모듈): 수집 본문이 PENDING·게시 시각 null로 저장.
- `QtVideoClipPreparationEventIntegrationTest`: 수집(미게시)→`publishDue()`→클립 준비 흐름으로 갱신.
- push 전 .github 게이트 점검: 브랜치명 `feature/…`, 금지 패턴·시크릿 없음(CLAUDE.md §13).

담당: DevD 이승욱
