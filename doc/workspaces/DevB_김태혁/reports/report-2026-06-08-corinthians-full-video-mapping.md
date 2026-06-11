# Report — 2026-06-08 corinthians-full-video-mapping

| Item | Value |
| --- | --- |
| Project | QT-AI-2nd-Team-Project |
| Scope | 1 Corinthians full-video mapping and QT video playback |
| Video | `bible-engine/public/videos/corinthians_full.mp4` |
| Storage | GitHub Release |

## Summary

- Created public GitHub repository: `xogurrh012/qtai-bible-videos`.
- Completed GitHub device approval for account `xogurrh012`.
- Published release: `1co-v1`.
- Uploaded release asset: `corinthians_full.mp4`.
- Asset size: `1,556,453,208` bytes.
- SHA-256: `06dc769590331c6da92e664fcd8e384b1184e9b5f66c1b3a691a3353c1cba276`.
- MP4 duration from `mvhd`: `4,370,560ms`.
- Canonical 1 Corinthians mapping regenerated for all `437` verses.

## Video URL

Stable app URL:

```text
https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4?raw=1
```

Note: the same URL without `?raw=1` intermittently returned `504 Gateway Timeout` during verification. The `?raw=1` URL returned the expected release asset redirect and byte-range response.

## Data Updates

- Regenerated `QT-AI-2nd-Team-Project/qtai-server/data/qt-video/corinthians_full_local_import.mysql.sql`.
- Regenerated `QT-AI-2nd-Team-Project/qtai-server/data/qt-video/corinthians_full_h2_local_seed.sql`.
- Updated `source_videos` provider metadata:
  - `provider = GITHUB_RELEASE`
  - `bucket = xogurrh012/qtai-bible-videos`
  - `object_key = releases/download/1co-v1/corinthians_full.mp4`
  - `public_url = GitHub release URL with ?raw=1`
- Updated verse segments:
  - count: `437`
  - first segment: `1 Corinthians 1:1`, `0-10000ms`
  - last segment: `1 Corinthians 16:24`, `4360000-4370000ms`

## Local H2 Verification

- Applied H2 seed to `qtai-server/build/qtai-local`.
- On 2026-06-09, changed the H2 local seed date from a hard-coded `2026-06-08` to `CURRENT_DATE`.
- Re-applied the H2 seed so the local `/api/v1/qt/today` endpoint keeps working on the current test day.
- Restarted the local backend to clear the `todayQt` cache.
- `GET /api/v1/qt/today` now returns:
  - `qtPassageId = 6` on 2026-06-09 local check
  - range `1CO 3:1-15`
- `GET /api/v1/qt/6/video` returns:
  - `status = READY`
  - `videoUrl = GitHub release URL with ?raw=1`
  - `startMs = 470000`
  - `endMs = 620000`
  - `clipStatus = APPROVED`

## MySQL Verification

- Existing Windows `MySQL80` service owns `localhost:3306`; it was not modified.
- Started project MySQL container `qtai-mysql-video` on `localhost:3307`.
- Connection:
  - database: `qtai`
  - username: `qtai`
  - password: `qtai`
- Started `qtai-server` with `SPRING_PROFILES_ACTIVE=dev` and `DB_URL=jdbc:mysql://localhost:3307/qtai?...`.
- Flyway applied and validated through V25:
  - V24 `create qt video assets`
  - V25 `create bible verse video segments`
- Imported `corinthians_full_local_import.mysql.sql`.
- Verification counts:
  - `source_videos = 1`
  - `1CO bible_verse_video_segments = 437`
  - first segment: `1CO 1:1`, `0-10000ms`
  - last segment: `1CO 16:24`, `4360000-4370000ms`
- Seeded today's MySQL test QT:
  - date: `2026-06-09`
  - `qtPassageId = 4`
  - range: `1 Corinthians 3:1-15`
  - approved clip: `470000-620000ms`
- MySQL-backed API verification:
  - `GET /api/v1/qt/today` returns `qtPassageId = 4`
  - `GET /api/v1/qt/4/video` returns `status = READY`

## Range Verification

Command:

```bash
curl -L -r 470000-471023 -o NUL -D - "https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4?raw=1"
```

Result:

```text
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Type: application/octet-stream
Content-Range: bytes 470000-471023/1556453208
```

## Flutter Emulator Verification

- Started Flutter app on `emulator-5554`.
- Today QT screen loaded `1 Corinthians 3:1-15`.
- QT video section rendered `1 Corinthians 3:1-15 Video`.
- Video initialized from the GitHub Release URL.
- Displayed playback length: `02:30`, matching the 15-verse range at 10 seconds per verse.
- Screenshot: `QT-AI-2nd-Team-Project/flutter-app/build/qt-video-after-load.png`.
- 2026-06-09 screenshot: `QT-AI-2nd-Team-Project/flutter-app/build/qt-video-2026-06-09-playback.png`.

## Handoff Notes

- This work is QT video playback, not simulator playback.
- `simulatorStatus` can still appear as `MISSING`; that is separate from the QT video endpoint.
- The app should use `GET /api/v1/qt/{qtPassageId}/video` for the QT video section.
- For 1 Corinthians v1, each verse segment is currently fixed at `10,000ms`.
- The current test passage `1 Corinthians 3:1-15` maps to `470000ms-620000ms`.
- Production DB setup requires running Flyway migrations V24/V25 first, then importing `corinthians_full_local_import.mysql.sql`.

## Current Caveat

GitHub Release is acceptable for the one-video free test, but it is not a production video CDN. If traffic grows or reliability becomes important, move the same asset to R2/S3/object storage and only update `source_videos.public_url`.
