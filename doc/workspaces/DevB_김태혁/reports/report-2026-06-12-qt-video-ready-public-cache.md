# Report - 2026-06-12 qt-video-ready-public-cache

## 요약

QT 영상 데이터가 `qt_video_clips`에 존재해도 Today QT 버튼이 비활성화되는 문제를 수정했다. 동시에 운영 Flyway에서 개인 테스트 파일 호스트 URL을 시드하지 않도록 정리하고, 앱의 QT 영상 캐시가 0초 이후 구간에서도 동작하도록 보정했다.

## 변경 내용

1. Today QT READY 판정 보정
   - `GetQtVideoAvailabilityUseCase` 추가
   - `QtVideoAvailabilityService` 추가
   - `QtStudyAvailabilityService`가 legacy `simulator_clips` 대신 `qt_video_clips(APPROVED)`를 기준으로 `READY`를 판단하도록 변경
   - `simulatorStatus` 응답 필드명은 기존 API 호환을 위해 유지하고, 내부 변수/문서에서는 QT 영상 READY 판정임을 명시

2. 고린도전서 timecode seed 보정
   - `V37__seed_corinthians_video_timecodes.sql` 추가
   - 운영 Flyway에는 개인 테스트 파일 호스트 URL을 넣지 않고 `UNCONFIGURED`/`INACTIVE` source placeholder만 저장
   - 고린도전서 437절에 대한 10초 단위 segment upsert
   - 기존 QT 영상 clip URL은 운영 Flyway에서 갱신하지 않음
   - source URL이 `qt-video://unconfigured/...`인 경우 자동 APPROVED clip 생성이 skip되도록 방어 로직 추가

3. Flutter QT 영상 캐시 보정
   - `startTimeSec > 0`인 QT 구간도 캐시를 확인하도록 변경
   - 캐시가 없으면 네트워크 스트리밍을 유지하면서 백그라운드 다운로드 수행
   - 캐시 파일은 24시간 이후 만료되도록 처리
   - 캐시 재사용/만료 테스트 추가

## 로컬 DB 확인

- 로컬 수동 테스트에서 사용한 개인 테스트 URL은 PR의 운영 Flyway 경로에서 제거함
- `source_videos` seed는 `UNCONFIGURED`/`INACTIVE` placeholder로 남기고, 실제 운영 URL은 승인된 R2/S3/CDN 또는 팀 소유 storage URL을 별도 import/update로 반영해야 함
- `bible_verse_video_segments` 고린도전서 segment 437건 확인

## 검증

- `.\gradlew.bat :service-bible:test` 통과
- `.\gradlew.bat :admin-server:test` 통과
- `flutter test test\features\bible\widgets\qt_video_player_test.dart` 통과
- `flutter analyze` 통과
- `git diff --check --ignore-cr-at-eol` 통과
- Android emulator 앱 재실행 완료

## 운영 메모

- 로컬 Docker Compose의 admin-server는 `SPRING_FLYWAY_ENABLED=false`로 떠 있으므로, 로컬 Docker DB에는 SQL을 수동 적용했다.
- 운영 또는 공유 DB에서 Flyway가 활성화된 경로라면 `V37` migration으로 고린도전서 source placeholder와 segment seed만 반영된다.
- `V37__seed_corinthians_video_timecodes.sql`은 PR diff에 포함되어 있으며, admin-server Flyway 경로에만 둬 스키마 단독 소유 규칙을 따른다.
- 영상 파일 URL은 운영 Flyway가 아니라 팀 소유 저장소(R2/S3/CDN 등) 확정 후 별도 import/update로 넣어야 한다.
- 요구사항 근거는 F-01/F-02/F-12다. `simulatorStatus`는 기존 API 호환 필드이고, READY 판정 소스는 현재 사용자 노출 영상 도메인인 `qt_video_clips(APPROVED)`다.
- service-bible은 Spring Modulith `@NamedInterface`를 사용하지 않고 ArchUnit `DomainBoundaryTest`로 도메인 경계를 검증한다. 이번 변경은 `study.internal`이 `qtvideo.api` public port에만 의존하도록 유지했다.
