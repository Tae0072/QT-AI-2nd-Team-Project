# 2026-06-10 QT video clips on MSA report

## Summary

Implemented QT video as a separate feature from the existing simulator flow.

- Backend owner: `service-bible`
- New API: `GET /api/v1/qt/{qtPassageId}/video`
- Today QT now includes `videoStatus`
- Flutter Today QT shows a bottom `QT 영상` section only when `videoStatus == READY`
- Existing simulator API/code remains in place, but the Today QT screen no longer shows the simulator button

## Backend Changes

- Added schema SQL:
  - `source_videos`
  - `bible_verse_video_segments`
  - `qt_video_clips`
- Added `qtvideo` domain:
  - `api`: `GetQtVideoUseCase`, `GetQtVideoAvailabilityUseCase`, response DTOs
  - `internal`: entities, repository, `QtVideoService`, `QtVideoAvailabilityService`
  - `web`: `QtVideoController`
- Added `videoStatus` enrichment in `QtService`.
- Avoided a circular dependency by separating video availability lookup from video detail lookup.
- Updated OpenAPI for the new QT video endpoint and response schemas.

## Flutter Changes

- Added `video_player`.
- Added `QtVideoClip` model and `qtVideoClipProvider`.
- Added `QtVideoSection`/`QtVideoPlayer`.
- Player supports:
  - play/pause
  - seek bar
  - playback speed
  - fullscreen without an X button
  - segment playback using `startTimeSec` and `endTimeSec`

## Operations Notes

1. Apply `V30__create_qt_video_clips.sql` to the shared DB.
2. Insert or upsert the 1 Corinthians source video row.
3. Insert verse timecodes into `bible_verse_video_segments`.
4. Insert or upsert the selected QT passage into `qt_video_clips` with `APPROVED` and `active_unique_key='ACTIVE'`.
5. If `qt_video_clips.video_url` points to the full book video, the app uses `startTimeSec/endTimeSec` to play only the QT range.
6. If `video_url` points to a pre-cut clip, keep `startTimeSec/endTimeSec` as source metadata.

Template:

- `scripts/qt-video-1co-import-template.sql`

## Verification

- `./gradlew.bat :service-bible:test` — passed
- `flutter pub get` — passed
- `flutter analyze` — passed
- `git diff --check` — no whitespace errors; line-ending warnings only

## Remaining Work

- Replace template URLs with the real hosted video URL.
- Apply/import data to the actual shared MySQL DB.
- Add automated cutting/import later only if the team decides to move beyond manual approved clips.
