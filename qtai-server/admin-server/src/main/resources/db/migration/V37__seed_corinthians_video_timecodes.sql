-- Seed 1 Corinthians video timecode metadata.
-- Do not seed a personal or test file-host URL in the operational Flyway path.
-- The source row stays INACTIVE with an internal placeholder URL until an
-- approved storage URL is imported/updated by operations.

SET @corinthians_placeholder_url := 'qt-video://unconfigured/1-corinthians-full';

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
    @corinthians_placeholder_url,
    NULL,
    'INACTIVE',
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE
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
