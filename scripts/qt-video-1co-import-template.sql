-- QT video seed/import template for 1 Corinthians.
-- Run after V30__create_qt_video_clips.sql has been applied.
-- Replace @source_video_url, @clip_video_url, and @qt_date before running.

SET @book_code = '1CO';
SET @seconds_per_verse = 10.000;
SET @source_video_title = '1 Corinthians full video';
SET @source_video_url = 'https://cdn.example.com/videos/corinthians_full.mp4';

-- If the app should play a pre-cut QT clip, set @clip_video_url to that clip URL.
-- If it should play a segment from the full book video, set this to @source_video_url.
SET @clip_video_url = @source_video_url;
SET @qt_date = '2026-06-17';

SELECT id INTO @book_id
FROM bible_books
WHERE code = @book_code
LIMIT 1;

INSERT INTO source_videos (
    bible_book_id,
    title,
    storage_provider,
    video_url,
    status,
    active_unique_key
)
VALUES (
    @book_id,
    @source_video_title,
    'EXTERNAL_URL',
    @source_video_url,
    'ACTIVE',
    'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    storage_provider = VALUES(storage_provider),
    video_url = VALUES(video_url),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(6);

SELECT id INTO @source_video_id
FROM source_videos
WHERE bible_book_id = @book_id
  AND active_unique_key = 'ACTIVE'
LIMIT 1;

INSERT INTO bible_verse_video_segments (
    bible_verse_id,
    source_video_id,
    start_time_sec,
    end_time_sec
)
SELECT
    verse_id,
    @source_video_id,
    (verse_order - 1) * @seconds_per_verse,
    verse_order * @seconds_per_verse
FROM (
    SELECT
        id AS verse_id,
        ROW_NUMBER() OVER (ORDER BY chapter_no, verse_no) AS verse_order
    FROM bible_verses
    WHERE book_id = @book_id
) ordered_verses
ON DUPLICATE KEY UPDATE
    start_time_sec = VALUES(start_time_sec),
    end_time_sec = VALUES(end_time_sec),
    updated_at = CURRENT_TIMESTAMP(6);

SELECT id INTO @qt_passage_id
FROM qt_passages
WHERE qt_date = @qt_date
LIMIT 1;

SELECT
    MIN(segment.start_time_sec),
    MAX(segment.end_time_sec)
INTO
    @clip_start_time_sec,
    @clip_end_time_sec
FROM qt_passage_verses passage_verse
JOIN bible_verse_video_segments segment
  ON segment.bible_verse_id = passage_verse.bible_verse_id
WHERE passage_verse.qt_passage_id = @qt_passage_id
  AND segment.source_video_id = @source_video_id;

INSERT INTO qt_video_clips (
    qt_passage_id,
    title,
    source_video_id,
    composition_type,
    video_url,
    start_time_sec,
    end_time_sec,
    status,
    active_unique_key,
    approved_at
)
VALUES (
    @qt_passage_id,
    CONCAT('QT video ', @qt_date),
    @source_video_id,
    'SINGLE_CUT',
    @clip_video_url,
    @clip_start_time_sec,
    @clip_end_time_sec,
    'APPROVED',
    'ACTIVE',
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    source_video_id = VALUES(source_video_id),
    composition_type = VALUES(composition_type),
    video_url = VALUES(video_url),
    start_time_sec = VALUES(start_time_sec),
    end_time_sec = VALUES(end_time_sec),
    status = VALUES(status),
    approved_at = VALUES(approved_at),
    updated_at = CURRENT_TIMESTAMP(6);
