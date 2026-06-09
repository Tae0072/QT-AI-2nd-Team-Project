-- ai-service owned AI persistence schema.
-- This migration intentionally contains only ai-service owned tables.
-- Data backfill and prompt version seed migration are handled by follow-up PRs.

CREATE TABLE ai_prompt_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_type VARCHAR(30) NOT NULL,
    version VARCHAR(30) NOT NULL,
    content_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ai_prompt_versions_type_version UNIQUE (prompt_type, version)
);

CREATE TABLE ai_generation_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NOT NULL,
    prompt_version_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    active_unique_key VARCHAR(20),
    error_message VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    started_at DATETIME(6),
    finished_at DATETIME(6),
    CONSTRAINT uk_ai_generation_jobs_active_target_prompt UNIQUE (
        job_type,
        target_type,
        target_id,
        prompt_version_id,
        active_unique_key
    ),
    CONSTRAINT uk_ai_generation_jobs_active_target UNIQUE (
        job_type,
        target_type,
        target_id,
        active_unique_key
    ),
    CONSTRAINT fk_ai_generation_jobs_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_versions(id)
);

CREATE INDEX idx_ai_generation_jobs_status_created
    ON ai_generation_jobs (status, created_at);
CREATE INDEX idx_ai_generation_jobs_target
    ON ai_generation_jobs (target_type, target_id);
CREATE INDEX idx_ai_generation_jobs_prompt_version
    ON ai_generation_jobs (prompt_version_id);

CREATE TABLE ai_generated_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    generation_job_id BIGINT NOT NULL,
    asset_type VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NOT NULL,
    payload_json LONGTEXT NOT NULL,
    source_label VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'VALIDATING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at DATETIME(6),
    CONSTRAINT fk_ai_generated_assets_generation_job
        FOREIGN KEY (generation_job_id) REFERENCES ai_generation_jobs(id)
);

CREATE INDEX idx_ai_generated_assets_target_status
    ON ai_generated_assets (target_type, target_id, status);
CREATE INDEX idx_ai_generated_assets_job
    ON ai_generated_assets (generation_job_id);

CREATE TABLE validation_reference_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(150) NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    source_file_hash VARCHAR(100) NOT NULL,
    storage_uri VARCHAR(500),
    index_storage_uri VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME(6),
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)
);

CREATE INDEX idx_validation_reference_jobs_status_expires
    ON validation_reference_jobs (status, expires_at);
CREATE INDEX idx_validation_reference_jobs_hash
    ON validation_reference_jobs (source_file_hash);

CREATE TABLE ai_validation_checklist_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    checklist_type VARCHAR(30) NOT NULL,
    version VARCHAR(30) NOT NULL,
    content_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by_admin_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    activated_at DATETIME(6),
    retired_at DATETIME(6),
    CONSTRAINT uk_checklist_type_version UNIQUE (checklist_type, version)
);

CREATE INDEX idx_ai_validation_checklist_versions_type_status
    ON ai_validation_checklist_versions (checklist_type, status);
CREATE INDEX idx_ai_validation_checklist_versions_hash
    ON ai_validation_checklist_versions (content_hash);

CREATE TABLE ai_validation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ai_asset_id BIGINT NOT NULL,
    validation_reference_job_id BIGINT,
    layer INT NOT NULL,
    result VARCHAR(30) NOT NULL,
    reviewer_type VARCHAR(30) NOT NULL DEFAULT 'AUTO',
    checklist_version_id BIGINT,
    checklist_json LONGTEXT,
    error_message VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ai_validation_logs_asset
        FOREIGN KEY (ai_asset_id) REFERENCES ai_generated_assets(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_validation_logs_reference_job
        FOREIGN KEY (validation_reference_job_id) REFERENCES validation_reference_jobs(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ai_validation_logs_checklist_version
        FOREIGN KEY (checklist_version_id) REFERENCES ai_validation_checklist_versions(id)
);

CREATE INDEX idx_ai_validation_logs_asset_layer
    ON ai_validation_logs (ai_asset_id, layer);
CREATE INDEX idx_ai_validation_logs_result_created
    ON ai_validation_logs (result, created_at);
CREATE INDEX idx_ai_validation_logs_reference_job
    ON ai_validation_logs (validation_reference_job_id);
CREATE INDEX idx_ai_validation_logs_checklist_version
    ON ai_validation_logs (checklist_version_id);

CREATE TABLE ai_batch_run_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_name VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    processed_count INT NOT NULL DEFAULT 0,
    error_type VARCHAR(100),
    error_message VARCHAR(1000),
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_ai_batch_run_logs_batch_created
    ON ai_batch_run_logs (batch_name, created_at);
CREATE INDEX idx_ai_batch_run_logs_status_created
    ON ai_batch_run_logs (status, created_at);

CREATE TABLE ai_event_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(80) NOT NULL,
    event_name VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    schema_version VARCHAR(30) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(100),
    last_error_message VARCHAR(1000),
    trace_id VARCHAR(120),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6),
    CONSTRAINT uk_ai_event_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_ai_event_outbox_status_created
    ON ai_event_outbox (status, created_at);
CREATE INDEX idx_ai_event_outbox_aggregate
    ON ai_event_outbox (aggregate_type, aggregate_id);
CREATE INDEX idx_ai_event_outbox_event_name_status
    ON ai_event_outbox (event_name, status);

CREATE TABLE ai_processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(80) NOT NULL,
    handler_name VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    last_error_code VARCHAR(100),
    last_error_message VARCHAR(1000),
    CONSTRAINT uk_ai_processed_events_event_handler UNIQUE (event_id, handler_name)
);

CREATE INDEX idx_ai_processed_events_status_processed
    ON ai_processed_events (status, processed_at);
CREATE INDEX idx_ai_processed_events_aggregate
    ON ai_processed_events (aggregate_type, aggregate_id);
