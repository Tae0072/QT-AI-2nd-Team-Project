# Report - 2026-06-12 qt-video-ready-public-cache

## 요약

QT 영상 데이터가 `qt_video_clips`에 존재해도 Today QT 버튼이 비활성화되는 문제를 수정했다. 동시에 고린도전서 영상 URL을 GitHub Release public URL로 seed하고, 앱의 QT 영상 캐시가 0초 이후 구간에서도 동작하도록 보정했다.

## 변경 내용

1. Today QT READY 판정 보정
   - `GetQtVideoAvailabilityUseCase` 추가
   - `QtVideoAvailabilityService` 추가
   - `QtStudyAvailabilityService`가 legacy `simulator_clips` 대신 `qt_video_clips(APPROVED)`를 기준으로 `READY`를 판단하도록 변경

2. public video URL seed 추가
   - `V37__seed_corinthians_public_video.sql` 추가
   - 고린도전서 원본 영상 URL을 GitHub Release URL로 upsert
   - 고린도전서 437절에 대한 10초 단위 segment upsert
   - 기존 QT 영상 clip URL이 로컬 개발 주소면 GitHub Release URL로 갱신

3. Flutter QT 영상 캐시 보정
   - `startTimeSec > 0`인 QT 구간도 캐시를 확인하도록 변경
   - 캐시가 없으면 네트워크 스트리밍을 유지하면서 백그라운드 다운로드 수행
   - 캐시 파일은 24시간 이후 만료되도록 처리
   - 캐시 재사용/만료 테스트 추가

## 로컬 DB 확인

- `source_videos.video_url`이 GitHub Release URL로 갱신됨
- `qt_video_clips.video_url` 4건 모두 GitHub Release URL로 갱신됨
- `bible_verse_video_segments` 고린도전서 segment 437건 확인
- `qt_video_clips` 내 `http://10.0.2.2:8787` 로컬 URL 0건 확인

## 검증

- `.\gradlew.bat :service-bible:test` 통과
- `.\gradlew.bat :admin-server:test` 통과
- `flutter test test\features\bible\widgets\qt_video_player_test.dart` 통과
- `flutter analyze` 통과
- `git diff --check --ignore-cr-at-eol` 통과
- Android emulator 앱 재실행 완료

## 운영 메모

- 로컬 Docker Compose의 admin-server는 `SPRING_FLYWAY_ENABLED=false`로 떠 있으므로, 로컬 Docker DB에는 SQL을 수동 적용했다.
- 운영 또는 공유 DB에서 Flyway가 활성화된 경로라면 `V37` migration으로 public URL과 segment seed가 반영된다.
- GitHub Release는 1차 무료 테스트용 public file host로 사용한다. 실제 트래픽이 커지면 같은 URL 필드를 R2/S3/CDN URL로 교체하면 된다.
