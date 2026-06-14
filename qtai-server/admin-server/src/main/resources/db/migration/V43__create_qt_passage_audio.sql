-- V43__create_qt_passage_audio.sql
-- 오늘 QT 본문 TTS 음성 캐시. 첫 재생 요청 때 서버가 생성해 DB에 저장(지연 캐시),
-- 이후 같은 (QT 본문, 목소리) 조합은 즉시 제공 → 무료 호스팅 콜드스타트/재생성 제거.
-- 본문(한글 절 범위)만 읽으므로 절별이 아니라 본문 단위로 1개 음성을 보관한다.
CREATE TABLE qt_passage_audio (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    qt_passage_id   BIGINT          NOT NULL,
    voice           VARCHAR(60)     NOT NULL,             -- 목소리 식별(예: '선희 (여성)')
    mime_type       VARCHAR(60)     NOT NULL DEFAULT 'audio/mpeg',
    byte_size       BIGINT          NOT NULL,
    audio_data      LONGBLOB        NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_qt_passage_audio UNIQUE (qt_passage_id, voice),
    CONSTRAINT fk_qpa_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id)
);

CREATE INDEX idx_qt_passage_audio_passage ON qt_passage_audio (qt_passage_id);
