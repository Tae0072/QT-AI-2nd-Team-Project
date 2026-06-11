CREATE TABLE commentary_sources (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    source_key          VARCHAR(80)   NOT NULL,
    name                VARCHAR(150)  NOT NULL,
    source_label        VARCHAR(200)  NOT NULL,
    product             VARCHAR(100),
    language            VARCHAR(10)   NOT NULL,
    usage_type          VARCHAR(30)   NOT NULL DEFAULT 'GENERATION_INPUT',
    license_label       VARCHAR(100),
    copyright_notice    TEXT,
    attribution         TEXT,
    access_level        VARCHAR(30)   NOT NULL DEFAULT 'INTERNAL',
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6),
    CONSTRAINT uk_commentary_sources_key UNIQUE (source_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_commentary_sources_usage_status ON commentary_sources (usage_type, status);

CREATE TABLE commentary_materials (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    source_id           BIGINT        NOT NULL,
    external_id         VARCHAR(200)  NOT NULL,
    material_type       VARCHAR(30)   NOT NULL,
    refs                VARCHAR(100)  NOT NULL,
    book_code           VARCHAR(20)   NOT NULL,
    chapter_start       SMALLINT      NOT NULL,
    verse_start         SMALLINT      NOT NULL,
    chapter_end         SMALLINT      NOT NULL,
    verse_end           SMALLINT      NOT NULL,
    title               VARCHAR(200),
    keywords_json       JSON,
    content_text        TEXT          NOT NULL,
    content_html        TEXT,
    content_hash        VARCHAR(100)  NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6),
    CONSTRAINT fk_commentary_materials_source FOREIGN KEY (source_id) REFERENCES commentary_sources(id),
    CONSTRAINT uk_commentary_material_source_external UNIQUE (source_id, external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_commentary_material_range
    ON commentary_materials (book_code, chapter_start, verse_start, chapter_end, verse_end);
CREATE INDEX idx_commentary_material_status ON commentary_materials (status);

CREATE TABLE commentary_material_verses (
    id                          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    commentary_material_id      BIGINT       NOT NULL,
    bible_verse_id              BIGINT       NOT NULL,
    display_order               SMALLINT     NOT NULL,
    match_type                  VARCHAR(20)  NOT NULL DEFAULT 'RANGE_EXPANDED',
    CONSTRAINT fk_commentary_material_verses_material
        FOREIGN KEY (commentary_material_id) REFERENCES commentary_materials(id) ON DELETE CASCADE,
    CONSTRAINT uk_commentary_material_verse UNIQUE (commentary_material_id, bible_verse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_commentary_material_verses_verse ON commentary_material_verses (bible_verse_id);
CREATE INDEX idx_commentary_material_verses_material_order
    ON commentary_material_verses (commentary_material_id, display_order);
