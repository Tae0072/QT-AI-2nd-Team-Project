# Workflow - 2026-06-12 qt-video-ready-public-cache

## 목표

Today QT 화면의 QT 영상 버튼이 비활성화되거나, 영상 URL이 로컬 개발 주소에 묶여 다른 환경에서 재생되지 않는 문제를 수정한다. 또한 QT 본문 구간이 0초 이후부터 시작해도 앱의 24시간 로컬 영상 캐시가 동작하도록 보정한다.

## 배경

- Today QT 버튼 활성화 여부는 `simulatorStatus == READY`에 의존한다.
- 기존 가용성 판정은 legacy `simulator_clips`를 조회하고 있었지만, 현재 QT 영상은 `qt_video_clips` 도메인으로 분리되어 있다.
- DB에 저장된 `video_url`이 `http://10.0.2.2:8787/...` 로컬 주소라서 다른 개발자 PC나 기기에서는 접근할 수 없다.
- Flutter 캐시는 존재하지만 `startTimeSec > 0`인 QT 구간에서는 캐시 확인과 다운로드가 생략되고 있었다.

## 작업 범위

1. `qtvideo` 도메인에 영상 가용성 조회용 public port를 추가한다.
2. `study` 도메인의 Today QT enrich가 새 port를 통해 `qt_video_clips(APPROVED)` 기준으로 READY를 판단하게 한다.
3. admin-server Flyway migration에 고린도전서 public GitHub Release URL seed를 추가한다.
4. 앱 QT 영상 플레이어가 start offset과 무관하게 캐시를 확인하고 백그라운드 다운로드를 수행하게 한다.
5. QT 영상 캐시 파일은 24시간이 지나면 만료되도록 한다.

## 설계

- `GetQtVideoAvailabilityUseCase`
  - `study` -> `qtvideo` 직접 internal import를 피하기 위한 port.
  - `QtVideoAvailabilityService`가 `qt_video_clips`의 `APPROVED` 존재 여부만 조회한다.

- `V37__seed_corinthians_public_video.sql`
  - `source_videos`의 고린도전서 원본 URL을 GitHub Release URL로 upsert한다.
  - 고린도전서 437절에 대해 10초 단위 `bible_verse_video_segments`를 upsert한다.
  - 기존 `qt_video_clips.video_url`이 로컬 주소면 public URL로 갱신한다.

- Flutter cache
  - 기존에는 `startTimeSec == 0`일 때만 캐시를 사용했다.
  - 변경 후에는 모든 QT 영상 구간에서 캐시 파일을 먼저 확인한다.
  - 캐시가 없으면 네트워크 스트리밍으로 재생하면서 백그라운드 다운로드를 시작한다.
  - 캐시 파일의 modified time이 24시간을 넘으면 삭제한다.

## 검증 계획

- `./gradlew.bat :service-bible:test`
- `./gradlew.bat :admin-server:test`
- `flutter test test/features/bible/widgets/qt_video_player_test.dart`
- `flutter analyze`
- Docker MySQL에서 `qt_video_clips.video_url`이 GitHub Release URL로 갱신됐는지 확인
- Android emulator에서 앱 재실행 후 QT 영상 플레이어 초기화 확인
