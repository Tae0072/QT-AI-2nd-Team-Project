# 2026-06-16 미게시 자동수집 본문 04:00 자동게시 + 기동 catch-up — 결과 보고

## 요약
자동수집(00:02) 본문을 수집 즉시 노출하지 않고 '미게시(PENDING_REVIEW)'로 저장하고, 게시 시각(QT 날짜 04:00 KST)에
`QtPassageAutoPublishService`가 ACTIVE로 게시하도록 했다(모델 A, §6 부합). 04:00 정기 실행과 기동 시 catch-up이
같은 로직을 공유해, 서버가 04:00에 안 돌아 누락돼도 기동 시 그 날짜 04:00으로 게시한다. 자동수집(collectedAt 있음)만
대상이며 관리자 수동 등록은 검토 게이트를 유지한다. 게시 시 매핑 변경 이벤트를 재발행해 QT영상 클립 준비를 트리거한다.

## 산출물
| 파일 | 설명 |
|------|------|
| `service-bible·admin-server …/qt/internal/QtPassage.java` | `scheduleForAutoPublish()`(미게시 전환) |
| `service-bible·admin-server …/qt/internal/QtPassageWriter.java` | 수집=미게시·수집 시각만(게시 시각은 게시 때) |
| `service-bible …/qt/internal/QtPassageRepository.java` | `findAutoPublishTargets`(PENDING+collectedAt+qtDate≤cutoff) |
| `service-bible …/qt/internal/QtPassageAutoPublishService.java` | `publishDue()` — 게시 시각 도래분 게시 + 매핑 이벤트 재발행 |
| `service-bible …/qt/internal/QtPassageAutoPublishScheduler.java` | 04:00 @Scheduled + 기동 catch-up, config 게이트 |
| `service-bible test/.../QtPassageAutoPublishServiceTest.java` | cutoff·게시·이벤트 단위 테스트 |
| `service-bible test/.../QtVideoClipPreparationEventIntegrationTest.java` | 수집→publishDue→클립 흐름 갱신 |
| `service-bible test/resources/application.yml` | `qt.auto-publish.enabled=false` 게이트 |

## 검증
- `./gradlew :service-bible:test :admin-server:test` 전부 BUILD SUCCESSFUL.
- 단위: 04:00 이후 오늘까지 게시(게시 시각=그 날짜 04:00)+이벤트, 04:00 이전 어제까지만 대상, writer 수집=PENDING·게시 시각 null.
- 통합: 클립 준비가 게시 시점에 트리거됨(미게시 skip → 게시 후 준비).

## 리뷰 보강(머지 전)
- 자동게시 대상은 `collected_at` 있는 자동수집 본문만 — 관리자 수동 등록(검토 대기)은 자동게시하지 않아 검토 게이트 보존.
- 정기 실행과 catch-up이 단일 `publishDue()` 공유 — cutoff 한 줄로 "now<04:00이면 어제까지"를 처리해 누락분도 일관 게시.
- 게시 시 매핑 변경 이벤트 재발행으로 클립 준비를 게시 시점으로 자연 이동(미게시 본문 클립 미준비 정책 유지).

## 미해결 / 후속
- **브랜치 스택**: `feature/admin-qt-management-ux`(collected_at) 기반. 그 PR 머지 후 dev로 rebase 또는 본 PR이 포함.
- 자동수집 미게시 본문이 관리자 목록에 '검토 대기'로 표기됨 — 필요 시 '예약' 등 별도 라벨은 후속(enum/UI 확장).

담당: DevD 이승욱
