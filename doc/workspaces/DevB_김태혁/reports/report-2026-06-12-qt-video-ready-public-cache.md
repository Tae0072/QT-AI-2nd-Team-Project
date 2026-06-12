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
- 확인 명령: `gh pr diff 569 --name-only` 결과에 `qtai-server/admin-server/src/main/resources/db/migration/V37__seed_corinthians_video_timecodes.sql`가 포함된다. 자동 리뷰의 코드 diff 입력에서 SQL이 제외될 수 있어 PR 본문에도 이 사실을 명시했다.
- 영상 파일 URL은 운영 Flyway가 아니라 팀 소유 저장소(R2/S3/CDN 등) 확정 후 별도 import/update로 넣어야 한다.
- 요구사항 근거는 F-01/F-02/F-12다. `simulatorStatus`는 기존 API 호환 필드이고, READY 판정 소스는 현재 사용자 노출 영상 도메인인 `qt_video_clips(APPROVED)`다.
- service-bible은 Spring Modulith `@NamedInterface`를 사용하지 않고 ArchUnit `DomainBoundaryTest`로 도메인 경계를 검증한다. 이번 변경은 `study.internal`이 `qtvideo.api` public port에만 의존하도록 유지했다.
- `hasPlayableUrl`의 null/blank/unconfigured URL skip 분기를 테스트로 보강했고, Flutter 캐시 정리는 `Directory.list(followLinks: false)` 스트리밍 순회로 바꿔 전체 파일 목록 materialize 비용을 줄였다.

## V37 SQL 리뷰 부록

자동 리뷰 입력에서 `.sql` 파일이 제외될 수 있어, 금지 데이터와 평문 URL 검증을 위해 V37 원문을 문서 diff에도 포함한다.

```sql
-- Seed 1 Corinthians video timecode metadata.
-- Do not seed a personal or test file-host URL in the operational Flyway path.
-- The source row stays INACTIVE with an internal placeholder URL until an
-- approved storage URL is imported/updated by operations.

INSERT INTO source_videos (
    bible_book_id,
    title,
    storage_provider,
    video_url,
    duration_sec,
    status,
    active_unique_key
) VALUES (
    46,
    '1 Corinthians full video timecode source',
    'UNCONFIGURED',
    'qt-video://unconfigured/1-corinthians-full',
    NULL,
    'INACTIVE',
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    id = id;

INSERT INTO bible_verse_video_segments (
    bible_verse_id,
    source_video_id,
    start_time_sec,
    end_time_sec
)
SELECT
    ordered_verses.bible_verse_id,
    source_video.id,
    CAST((ordered_verses.verse_index - 1) * 10.000 AS DECIMAL(10, 3)),
    CAST(ordered_verses.verse_index * 10.000 AS DECIMAL(10, 3))
FROM (
    SELECT
        id AS bible_verse_id,
        ROW_NUMBER() OVER (ORDER BY chapter_no, verse_no) AS verse_index
    FROM bible_verses
    WHERE book_id = 46
) ordered_verses
JOIN source_videos source_video
  ON source_video.bible_book_id = 46
 AND source_video.active_unique_key = 'ACTIVE'
 AND source_video.video_url = 'qt-video://unconfigured/1-corinthians-full'
ON DUPLICATE KEY UPDATE
    start_time_sec = VALUES(start_time_sec),
    end_time_sec = VALUES(end_time_sec),
    deleted_at = NULL;
```
