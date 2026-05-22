-- V8__create_member_praise_songs.sql
CREATE TABLE member_praise_songs (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT          NOT NULL,
    praise_song_id  BIGINT,
    device_song_key VARCHAR(200),
    display_title   VARCHAR(100)    NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member_praise_member (member_id, created_at DESC),
    UNIQUE KEY uk_member_praise_curated (member_id, praise_song_id),
    CONSTRAINT fk_mps_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mps_praise_song FOREIGN KEY (praise_song_id) REFERENCES praise_songs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
