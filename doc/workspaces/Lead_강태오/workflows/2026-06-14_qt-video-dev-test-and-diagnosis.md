# QT영상(시뮬레이터) 비활성 진단 + 개발자 모드 테스트 (2026-06-14)

## 1. 배경 / 요청
- "QT영상 버튼이 비활성화되어 있다. DB에 저장해서 불러온다고 했는데 확인해 달라."
- "QT영상도 개발자 모드에서 테스트할 수 있게 해 달라."

## 2. 비활성 원인 (진단 결과)
코드는 정상이며, **데이터(영상 클립)가 DB에 하나도 없어서** 버튼이 꺼져 있었다.

흐름:
1. FE 버튼 활성 조건: `simulatorReady = qtPassageId != null && data.simulatorStatus == 'READY'`
   (`today_qt_screen.dart`)
2. `simulatorStatus`는 서버 `GetTodayQtUseCase` → `QtStudyAvailabilityService.getAvailability()`
   → `GetQtVideoAvailabilityUseCase.hasReadyVideo(qtPassageId)` 결과.
3. `hasReadyVideo` = `qt_video_clips`에 해당 본문의 **APPROVED** 클립이 존재하면 true.
4. 로컬 DB 확인 결과 `source_videos=0, bible_verse_video_segments=0, qt_video_clips=0` → 전부 비어 있음
   → status = MISSING → 버튼 비활성.

즉 "DB에서 불러오는" 구조는 구현돼 있으나 **승인 클립 데이터가 시딩되지 않은 상태**였다.

## 3. 조치

### 3-1. 개발자 모드 QT영상 테스트 화면 (FE, 커밋 대상)
`flutter-app/lib/features/dev/dev_qt_video_test_screen.dart` 추가, 개발자 모드 허브에 `QT영상 테스트` 버튼 연결.
- (1) **샘플 영상 플레이어**: 공개 샘플 mp4(ForBiggerBlazes, 0~10초)를 실제 `QtVideoPlayer` 위젯으로 재생.
  DB 데이터와 무관하게 재생/일시정지/배속/구간/전체화면 동작을 검증.
- (2) **서버·DB 경로**: QT 본문 ID 입력 → `qtVideoClipProvider`로 `/qt/{id}/video` 호출 →
  status(READY/MISSING/…)·videoUrl·구간을 표시하고, READY면 실제 `QtVideoSection`을 렌더.
  "오늘의 QT 영상" 버튼과 완전히 동일한 경로라 DB→서버→앱 흐름을 그대로 확인.

### 3-2. 로컬 DB 샘플 클립 시딩 (로컬 전용, 운영 데이터 아님)
`scripts/dev-seed-qt-video.sql` — 오늘 QT 본문(id=5, 고전 9:1-23)에
`source_videos` 1건 + `qt_video_clips`(APPROVED, ACTIVE) 1건을 시딩(영상=공개 샘플 mp4).
- 적용 후 `hasReadyVideo(5)=true` → 오늘 QT의 시뮬레이터 버튼이 활성화됨.
- 멱등(본문당 ACTIVE 1건 보장: 기존 ACTIVE 삭제 후 삽입).
- 운영 시드가 아니라 로컬에서 버튼/플레이어를 켜 보기 위한 임시 데이터.

## 4. 검증
- `flutter analyze` (dev 화면 2개) — 무이슈.
- `flutter test` 전체 — GREEN.
- DB 시딩 후 `qt_video_clips` APPROVED 1건 확인, `hasReadyVideo` 로직상 READY 보장.

## 5. 사용법(로컬)
```powershell
# 시딩
docker cp scripts/dev-seed-qt-video.sql qtai-mysql:/tmp/seed.sql
docker exec qtai-mysql sh -lc "mysql -uqtai -p<DB_PASSWORD> qtai < /tmp/seed.sql"
```
앱에서: 개발자 모드 → `QT영상 테스트` → 본문 ID(예: 5) 입력 후 불러오기, 또는 오늘의 QT에서 시뮬레이터 버튼.

## 6. 메모
- 영상 본 콘텐츠(실제 QT 영상)는 별도 제작/시딩 대상. 본 작업은 "경로/플레이어 동작 검증"과
  "버튼 활성 조건 확인"까지를 다룬다.
- 개발 종료 시 `[DEV_MODE]` 검색으로 dev 화면 일괄 제거.
