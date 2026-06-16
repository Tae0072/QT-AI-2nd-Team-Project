# 2026-06-16 미게시 자동수집 본문 04:00 자동게시 + 기동 catch-up 결과 보고

## 요약
자동수집(00:02) 본문을 수집 즉시 노출하지 않고 `PENDING_REVIEW`로 저장한 뒤, 게시 시각(QT 날짜 04:00 KST)이 도래하면 `QtPassageAutoPublishService.publishDue()`가 `ACTIVE`로 전환한다. 서버가 04:00에 동작하지 않아 누락된 본문은 기동 catch-up이 같은 `publishDue()`를 호출해 해당 QT 날짜 04:00으로 일관 게시한다.

## 리뷰 보완
- admin-server 복사본도 신규 자동수집 본문 저장 시 `scheduleForAutoPublish()`를 실제 호출하도록 정합성을 맞췄다.
- 자동게시 대상 repository 조회는 `status=PENDING_REVIEW`, `collected_at is not null`, `qtDate <= cutoff`를 만족하는 본문만 반환하며, slice 테스트로 `collected_at is not null` 게이트를 고정했다.
- 다중 인스턴스 중복 게시·이벤트 방지를 위해 `findAutoPublishTargets`에 `PESSIMISTIC_WRITE` lock을 적용하고, 서비스 루프에서 `PENDING_REVIEW + collectedAt != null`을 재검증한다. stale target이 들어와도 게시 수와 이벤트를 증가시키지 않는다.
- scheduler의 예외 흡수 경로는 `QtPassageAutoPublishSchedulerTest`로 고정했다. 04:00 scheduled trigger와 startup catch-up 모두 `publishDue()` 예외를 외부로 전파하지 않는다.
- 자동게시는 사용자 요청이 아닌 내부 배치 동작이므로 `actorType=SYSTEM_BATCH`를 로그에 남긴다. service-bible은 audit 도메인을 소유하지 않으므로 admin-server audit write use case를 직접 import하지 않는다.
- QT 영상 클립 준비 리스너는 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`이며, 단위 테스트로 phase를 고정했다.
- `QtPassage` Javadoc 오배치와 `QtPassageWriter` 고아 주석을 정리했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `service-bible/admin-server .../QtPassage.java` | `scheduleForAutoPublish()`로 자동수집 미게시 예약 |
| `service-bible/admin-server .../QtPassageWriter.java` | 신규 자동수집 본문은 PENDING_REVIEW로 저장, 재수집은 상태 보존 |
| `service-bible .../QtPassageRepository.java` | 자동게시 대상 게이트 + pessimistic write lock |
| `service-bible .../QtPassageAutoPublishService.java` | cutoff 계산, 게시, stale target skip, SYSTEM_BATCH 로그 |
| `service-bible .../QtPassageAutoPublishScheduler.java` | 04:00 scheduled trigger + startup catch-up |
| `service-bible test/.../QtPassageAutoPublishServiceTest.java` | cutoff, 게시 시각, stale target skip, 이벤트 발행 검증 |
| `service-bible test/.../QtPassageRepositoryTest.java` | collectedAt 게이트와 lock annotation 검증 |
| `service-bible test/.../QtPassageAutoPublishSchedulerTest.java` | scheduler 예외 흡수 검증 |
| `service-bible test/.../QtVideoClipPreparationListenerTest.java` | AFTER_COMMIT listener phase 검증 |

## 검증
- `./gradlew.bat --no-daemon :service-bible:test :admin-server:test`
- `git diff --check`
- `python C:\Users\G\.codex\skills\qtai-pr-guard\scripts\qtai_pr_preflight.py --repo . --base origin/dev`

담당: DevD 이승욱
