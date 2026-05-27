-- V6__create_auth_ai_explanation_tables.sql
-- 병합: member_auth_providers + verse_explanations + ai_generation_logging

-- ============================================================
-- 1. member_auth_providers (OAuth 제공자 연동)
-- ============================================================
CREATE TABLE member_auth_providers (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id         BIGINT       NOT NULL,
    provider          VARCHAR(20)  NOT NULL DEFAULT 'KAKAO',
    provider_user_id  VARCHAR(100) NOT NULL,
    connected_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_auth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_auth_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auth_member_id ON member_auth_providers(member_id);

-- ============================================================
-- 2. verse_explanations (절 해설)
-- ============================================================
CREATE TABLE verse_explanations (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    bible_verse_id      BIGINT          NOT NULL,
    summary             VARCHAR(300),
    explanation         TEXT            NOT NULL,
    source_label        VARCHAR(200)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    active_unique_key   VARCHAR(20),
    ai_asset_id         BIGINT,
    approved_at         DATETIME(6),
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_explanations_verse_status (bible_verse_id, status),
    INDEX idx_explanations_status (status),
    UNIQUE KEY uk_explanations_active_per_verse (bible_verse_id, active_unique_key),
    CONSTRAINT fk_verse_explanations_verse FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. AI 생성·검증 로깅 (6 tables)
-- ============================================================
CREATE TABLE ai_prompt_versions (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    prompt_type     VARCHAR(30)  NOT NULL,
    version         VARCHAR(30)  NOT NULL,
    content_hash    VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prompt_type_version UNIQUE (prompt_type, version)
);

CREATE TABLE ai_generation_jobs (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    job_type            VARCHAR(40)  NOT NULL,
    target_type         VARCHAR(40)  NOT NULL,
    target_id           BIGINT       NOT NULL,
    prompt_version_id   BIGINT       NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'QUEUED',
    active_unique_key   VARCHAR(20),
    requested_by_admin_id BIGINT,
    started_at          TIMESTAMP,
    finished_at         TIMESTAMP,
    error_message       VARCHAR(1000),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT uk_ai_generation_jobs_active_target_prompt UNIQUE (
        job_type, target_type, target_id, prompt_version_id, active_unique_key
    ),
    CONSTRAINT fk_ai_generation_jobs_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_versions(id)
);

CREATE INDEX idx_ai_jobs_status_created ON ai_generation_jobs (status, created_at);
CREATE INDEX idx_ai_jobs_target ON ai_generation_jobs (target_type, target_id);
CREATE INDEX idx_ai_jobs_prompt_version ON ai_generation_jobs (prompt_version_id);

CREATE TABLE ai_generated_assets (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    generation_job_id   BIGINT       NOT NULL,
    asset_type          VARCHAR(40)  NOT NULL,
    target_type         VARCHAR(40)  NOT NULL,
    target_id           BIGINT       NOT NULL,
    payload_json        CLOB         NOT NULL,
    source_label        VARCHAR(255),
    status              VARCHAR(30)  NOT NULL DEFAULT 'VALIDATING',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at         TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT fk_ai_generated_assets_generation_job
        FOREIGN KEY (generation_job_id) REFERENCES ai_generation_jobs(id)
);

CREATE INDEX idx_ai_assets_target_status ON ai_generated_assets (target_type, target_id, status);
CREATE INDEX idx_ai_assets_job ON ai_generated_assets (generation_job_id);

CREATE TABLE validation_reference_jobs (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    source_name         VARCHAR(150) NOT NULL,
    source_file_name    VARCHAR(255) NOT NULL,
    source_file_hash    VARCHAR(100) NOT NULL,
    storage_uri         VARCHAR(500),
    index_storage_uri   VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    expires_at          TIMESTAMP,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP
);

CREATE INDEX idx_validation_reference_status_expires ON validation_reference_jobs (status, expires_at);
CREATE INDEX idx_validation_reference_hash ON validation_reference_jobs (source_file_hash);

CREATE TABLE ai_validation_checklist_versions (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    checklist_type      VARCHAR(30)  NOT NULL,
    version             VARCHAR(30)  NOT NULL,
    content_hash        VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by_admin_id BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at        TIMESTAMP,
    retired_at          TIMESTAMP,
    CONSTRAINT uk_checklist_type_version UNIQUE (checklist_type, version)
);

CREATE INDEX idx_checklist_type_status ON ai_validation_checklist_versions (checklist_type, status);
CREATE INDEX idx_checklist_hash ON ai_validation_checklist_versions (content_hash);

CREATE TABLE ai_validation_logs (
    id                          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    ai_asset_id                 BIGINT       NOT NULL,
    validation_reference_job_id BIGINT,
    checklist_version_id        BIGINT       NOT NULL,
    layer                       TINYINT      NOT NULL,
    result                      VARCHAR(30)  NOT NULL,
    checklist_json              CLOB,
    reviewer_type               VARCHAR(30)  NOT NULL DEFAULT 'AUTO',
    reviewer_admin_id           BIGINT,
    error_message               VARCHAR(1000),
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_validation_logs_asset
        FOREIGN KEY (ai_asset_id) REFERENCES ai_generated_assets(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_validation_logs_reference_job
        FOREIGN KEY (validation_reference_job_id) REFERENCES validation_reference_jobs(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ai_validation_logs_checklist_version
        FOREIGN KEY (checklist_version_id) REFERENCES ai_validation_checklist_versions(id)
);

CREATE INDEX idx_validation_asset_layer ON ai_validation_logs (ai_asset_id, layer);
CREATE INDEX idx_validation_result_created ON ai_validation_logs (result, created_at);
CREATE INDEX idx_validation_reference_job ON ai_validation_logs (validation_reference_job_id);
CREATE INDEX idx_validation_checklist_version ON ai_validation_logs (checklist_version_id);
