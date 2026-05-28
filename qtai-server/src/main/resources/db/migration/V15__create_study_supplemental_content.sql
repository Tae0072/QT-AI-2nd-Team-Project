-- F-08/F-12 사용자 노출용 승인 콘텐츠 조회 테이블

CREATE TABLE glossary_terms (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    bible_verse_id    BIGINT       NOT NULL,
    term              VARCHAR(100) NOT NULL,
    meaning           TEXT         NOT NULL,
    source_label      VARCHAR(200) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'HIDDEN',
    ai_asset_id       BIGINT,
    approved_at       DATETIME(6),
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at        DATETIME(6),
    CONSTRAINT fk_glossary_terms_verse FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id),
    CONSTRAINT fk_glossary_terms_ai_asset FOREIGN KEY (ai_asset_id) REFERENCES ai_generated_assets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_glossary_terms_verse_status ON glossary_terms (bible_verse_id, status);
CREATE INDEX idx_glossary_terms_status ON glossary_terms (status);

CREATE TABLE simulator_component_library_versions (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    version     VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6),
    CONSTRAINT uk_simulator_component_library_version UNIQUE (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulator_clips (
    id                             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    qt_passage_id                  BIGINT       NOT NULL,
    title                          VARCHAR(200) NOT NULL,
    component_library_version_id   BIGINT       NOT NULL,
    scene_script_json              LONGTEXT     NOT NULL,
    status                         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ai_asset_id                    BIGINT,
    approved_at                    DATETIME(6),
    created_at                     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                     DATETIME(6),
    CONSTRAINT fk_simulator_clips_qt_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id),
    CONSTRAINT fk_simulator_clips_component_version
        FOREIGN KEY (component_library_version_id) REFERENCES simulator_component_library_versions(id),
    CONSTRAINT fk_simulator_clips_ai_asset FOREIGN KEY (ai_asset_id) REFERENCES ai_generated_assets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_simulator_clips_passage_status ON simulator_clips (qt_passage_id, status);
CREATE INDEX idx_simulator_clips_status ON simulator_clips (status);
