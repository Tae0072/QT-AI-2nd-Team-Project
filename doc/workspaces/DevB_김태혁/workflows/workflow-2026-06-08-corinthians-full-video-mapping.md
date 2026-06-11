# Workflow — 2026-06-08 corinthians-full-video-mapping

| Item | Value |
| --- | --- |
| Project | QT-AI-2nd-Team-Project |
| Scope | 1 Corinthians QT video asset storage and mapping |
| Storage target | GitHub Release, one-video free test |
| Local test DB | H2 file DB `qtai-server/build/qtai-local` |

## Goal

Use the completed `corinthians_full.mp4` as the source video for QT video playback, without connecting it to the simulator feature.

The app should receive:

- today's QT passage,
- a QT-video-specific API response,
- the full source video URL,
- and the playback `startMs/endMs` for the QT passage.

## Final Flow

```text
GitHub Release asset
-> source_videos.public_url
-> bible_verse_video_segments per verse
-> qt_passage_video_clips approved range
-> GET /api/v1/qt/{qtPassageId}/video
-> Flutter QT video section
```

## Completed Steps

1. Created public GitHub repository `xogurrh012/qtai-bible-videos`.
2. Installed and authenticated GitHub CLI.
3. Completed GitHub device approval for account `xogurrh012`.
4. Created release `1co-v1`.
5. Uploaded `corinthians_full.mp4`.
6. Verified uploaded asset metadata and SHA-256.
7. Parsed MP4 `mvhd` duration as `4,370,560ms`.
8. Regenerated MySQL import SQL for all 437 verses.
9. Regenerated H2 seed SQL for all 437 verses.
10. Applied H2 seed locally.
11. Restarted backend to clear cached today QT response.
12. Verified API playback range for `1 Corinthians 3:1-15`.
13. Verified HTTP byte range response on the final URL with `?raw=1`.
14. Verified Flutter emulator playback from the GitHub Release URL.
15. Updated the H2 local seed to use `CURRENT_DATE` so `/api/v1/qt/today` works on the next test day.
16. Re-applied the H2 seed on 2026-06-09 and verified the new local `qtPassageId = 6`.
17. Started a project MySQL 8.0 container on `localhost:3307` because local Windows `MySQL80` already owned `3306`.
18. Ran Spring/Flyway against MySQL `localhost:3307/qtai` and applied migrations through V25.
19. Imported `corinthians_full_local_import.mysql.sql` into MySQL.
20. Seeded today's MySQL QT test passage and approved QT video clip for `1 Corinthians 3:1-15`.
21. Verified MySQL-backed APIs: `/api/v1/qt/today` and `/api/v1/qt/4/video`.

## Mapping Rule

For 1 Corinthians v1:

```text
verseDurationMs = 10000
startMs = zeroBasedVerseIndex * 10000
endMs = startMs + 10000
```

Examples:

```text
1 Corinthians 1:1  -> 0-10000
1 Corinthians 3:1  -> 470000-480000
1 Corinthians 3:15 -> 610000-620000
1 Corinthians 16:24 -> 4360000-4370000
```

## Current Test Passage

```text
Date: CURRENT_DATE in local H2 seed
Passage: 1 Corinthians 3:1-15
video startMs: 470000
video endMs: 620000
```

## Files Updated

- `QT-AI-2nd-Team-Project/qtai-server/data/qt-video/corinthians_full_local_import.mysql.sql`
- `QT-AI-2nd-Team-Project/qtai-server/data/qt-video/corinthians_full_h2_local_seed.sql`
- `bible-engine/report-2026-06-08-corinthians-full-video-mapping.md`
- `bible-engine/workflow-2026-06-08-corinthians-full-video-mapping.md`

## Verification Snapshot

```text
GitHub account: xogurrh012
Repository: xogurrh012/qtai-bible-videos
Release tag: 1co-v1
Asset: corinthians_full.mp4
Asset size: 1,556,453,208 bytes
SHA-256: 06dc769590331c6da92e664fcd8e384b1184e9b5f66c1b3a691a3353c1cba276
App URL: https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4?raw=1
Flutter result: video rendered and played on emulator-5554
2026-06-09 local check: qtPassageId=6, video status=READY, Flutter screen rendered QT video section
2026-06-09 MySQL check: localhost:3307, Flyway V25, source_videos=1, 1CO segments=437, qtPassageId=4, clip status=READY
```

## Next Steps

- Keep GitHub Release for the first free one-video test.
- If GitHub Release proves unstable under real traffic, move the same object to Cloudflare R2 or S3 and update only `source_videos.public_url`.
- For production, run the MySQL import SQL after Flyway migrations V24/V25 on the real database.
