-- QT video playback metadata.
-- Source videos are book-level full videos. Verse segments map each Bible verse
-- to a time range in the source video. qt_video_clips stores the final approved
-- clip URL that the app can play for a QT passage.

CREATE TABLE source_videos (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    bible_book_id       SMALLINT      NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    storage_provider    VARCHAR(30)   NOT NULL DEFAULT 'EXTERNAL_URL',
    video_url           VARCHAR(2048) NOT NULL,
    duration_sec        DECIMAL(10,3),
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    active_unique_key   VARCHAR(20),
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6),
    CONSTRAINT fk_source_videos_book FOREIGN KEY (bible_book_id) REFERENCES bible_books(id),
    CONSTRAINT uk_source_videos_book_active UNIQUE (bible_book_id, active_unique_key),
    CONSTRAINT ck_source_videos_duration CHECK (duration_sec IS NULL OR duration_sec > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_source_videos_book_status ON source_videos (bible_book_id, status);

CREATE TABLE bible_verse_video_segments (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    bible_verse_id      BIGINT        NOT NULL,
    source_video_id     BIGINT        NOT NULL,
    start_time_sec      DECIMAL(10,3) NOT NULL,
    end_time_sec        DECIMAL(10,3) NOT NULL,
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6),
    CONSTRAINT fk_bv_video_segments_verse FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id),
    CONSTRAINT fk_bv_video_segments_source FOREIGN KEY (source_video_id) REFERENCES source_videos(id),
    CONSTRAINT uk_bv_video_segments_verse_source UNIQUE (bible_verse_id, source_video_id),
    CONSTRAINT ck_bv_video_segments_time CHECK (start_time_sec >= 0 AND end_time_sec > start_time_sec)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bv_video_segments_source_time ON bible_verse_video_segments (source_video_id, start_time_sec);

CREATE TABLE qt_video_clips (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    qt_passage_id       BIGINT        NOT NULL,
    title               VARCHAR(200)  NOT NULL,
    source_video_id     BIGINT        NOT NULL,
    composition_type    VARCHAR(30)   NOT NULL DEFAULT 'SINGLE_CUT',
    video_url           VARCHAR(2048) NOT NULL,
    start_time_sec      DECIMAL(10,3) NOT NULL,
    end_time_sec        DECIMAL(10,3) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'APPROVED',
    active_unique_key   VARCHAR(20),
    approved_at         DATETIME(6),
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6),
    CONSTRAINT fk_qt_video_clips_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id),
    CONSTRAINT fk_qt_video_clips_source FOREIGN KEY (source_video_id) REFERENCES source_videos(id),
    CONSTRAINT uk_qt_video_clips_passage_active UNIQUE (qt_passage_id, active_unique_key),
    CONSTRAINT ck_qt_video_clips_time CHECK (start_time_sec >= 0 AND end_time_sec > start_time_sec)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_qt_video_clips_passage_status ON qt_video_clips (qt_passage_id, status);
CREATE INDEX idx_qt_video_clips_status ON qt_video_clips (status);
