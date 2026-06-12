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
