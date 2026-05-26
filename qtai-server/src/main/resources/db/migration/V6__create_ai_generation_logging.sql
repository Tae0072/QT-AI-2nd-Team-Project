-- V6__create_ai_generation_logging.sql
CREATE TABLE ai_prompt_versions (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    prompt_type     VARCHAR(30)  NOT NULL,
    version         VARCHAR(30)  NOT NULL,
    content_hash    VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_prompt_type_version (prompt_type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_generation_jobs (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    job_type            VARCHAR(40)  NOT NULL,
    target_type         VARCHAR(40)  NOT NULL,
    target_id           BIGINT       NOT NULL,
    prompt_version_id   BIGINT       NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'QUEUED',
    active_unique_key   VARCHAR(20),
    requested_by_admin_id BIGINT,
    started_at          DATETIME(6),
    finished_at         DATETIME(6),
    error_message       VARCHAR(1000),
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6),
    UNIQUE KEY uk_ai_generation_jobs_active_target_prompt (
        job_type,
        target_type,
        target_id,
        prompt_version_id,
        active_unique_key
    ),
    INDEX idx_ai_jobs_status_created (status, created_at),
    INDEX idx_ai_jobs_target (target_type, target_id),
    INDEX idx_ai_jobs_prompt_version (prompt_version_id),
    CONSTRAINT fk_ai_generation_jobs_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_versions(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_generated_assets (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    generation_job_id   BIGINT       NOT NULL,
    asset_type          VARCHAR(40)  NOT NULL,
    target_type         VARCHAR(40)  NOT NULL,
    target_id           BIGINT       NOT NULL,
    payload_json        JSON         NOT NULL,
    source_label        VARCHAR(255),
    status              VARCHAR(30)  NOT NULL DEFAULT 'VALIDATING',
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at         DATETIME(6),
    updated_at          DATETIME(6),
    INDEX idx_ai_assets_target_status (target_type, target_id, status),
    INDEX idx_ai_assets_job (generation_job_id),
    CONSTRAINT fk_ai_generated_assets_generation_job
        FOREIGN KEY (generation_job_id) REFERENCES ai_generation_jobs(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE validation_reference_jobs (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    source_name         VARCHAR(150) NOT NULL,
    source_file_name    VARCHAR(255) NOT NULL,
    source_file_hash    VARCHAR(100) NOT NULL,
    storage_uri         VARCHAR(500),
    index_storage_uri   VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    expires_at          DATETIME(6),
    deleted_at          DATETIME(6),
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6),
    INDEX idx_validation_reference_status_expires (status, expires_at),
    INDEX idx_validation_reference_hash (source_file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_validation_checklist_versions (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    checklist_type      VARCHAR(30)  NOT NULL,
    version             VARCHAR(30)  NOT NULL,
    content_hash        VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by_admin_id BIGINT,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    activated_at        DATETIME(6),
    retired_at          DATETIME(6),
    UNIQUE KEY uk_checklist_type_version (checklist_type, version),
    INDEX idx_checklist_type_status (checklist_type, status),
    INDEX idx_checklist_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_validation_logs (
    id                          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    ai_asset_id                 BIGINT       NOT NULL,
    validation_reference_job_id BIGINT,
    checklist_version_id        BIGINT       NOT NULL,
    layer                       TINYINT      NOT NULL,
    result                      VARCHAR(30)  NOT NULL,
    checklist_json              JSON,
    reviewer_type               VARCHAR(30)  NOT NULL DEFAULT 'AUTO',
    reviewer_admin_id           BIGINT,
    error_message               VARCHAR(1000),
    created_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_validation_asset_layer (ai_asset_id, layer),
    INDEX idx_validation_result_created (result, created_at),
    INDEX idx_validation_reference_job (validation_reference_job_id),
    INDEX idx_validation_checklist_version (checklist_version_id),
    CONSTRAINT fk_ai_validation_logs_asset
        FOREIGN KEY (ai_asset_id) REFERENCES ai_generated_assets(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ai_validation_logs_reference_job
        FOREIGN KEY (validation_reference_job_id) REFERENCES validation_reference_jobs(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ai_validation_logs_checklist_version
        FOREIGN KEY (checklist_version_id) REFERENCES ai_validation_checklist_versions(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
