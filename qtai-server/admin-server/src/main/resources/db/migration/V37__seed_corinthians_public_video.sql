-- Seed public 1 Corinthians video metadata.
-- The app plays the video_url returned by service-bible, so this must be a
-- public URL instead of the Android-emulator-only http://10.0.2.2 address.

SET @corinthians_video_url := 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4?raw=1';

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
    '1 Corinthians full video',
    'EXTERNAL_URL',
    @corinthians_video_url,
    NULL,
    'ACTIVE',
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    storage_provider = VALUES(storage_provider),
    video_url = VALUES(video_url),
    duration_sec = VALUES(duration_sec),
    status = VALUES(status),
    deleted_at = NULL,
    id = LAST_INSERT_ID(id);

SET @corinthians_source_video_id := LAST_INSERT_ID();

INSERT INTO bible_verse_video_segments (
    bible_verse_id,
    source_video_id,
    start_time_sec,
    end_time_sec
)
SELECT
    ordered_verses.bible_verse_id,
    @corinthians_source_video_id,
    CAST((ordered_verses.verse_index - 1) * 10.000 AS DECIMAL(10, 3)),
    CAST(ordered_verses.verse_index * 10.000 AS DECIMAL(10, 3))
FROM (
    SELECT
        id AS bible_verse_id,
        ROW_NUMBER() OVER (ORDER BY chapter_no, verse_no) AS verse_index
    FROM bible_verses
    WHERE book_id = 46
) ordered_verses
ON DUPLICATE KEY UPDATE
    start_time_sec = VALUES(start_time_sec),
    end_time_sec = VALUES(end_time_sec),
    deleted_at = NULL;

UPDATE qt_video_clips
SET
    video_url = @corinthians_video_url,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE source_video_id = @corinthians_source_video_id
  AND video_url <> @corinthians_video_url;
