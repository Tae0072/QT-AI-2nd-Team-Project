-- Ensure environments that created music_tracks through Hibernate update can store managed audio files.
ALTER TABLE music_tracks MODIFY COLUMN audio_data LONGBLOB NOT NULL;
