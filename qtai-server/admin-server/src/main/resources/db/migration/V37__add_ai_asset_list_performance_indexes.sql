CREATE INDEX idx_ai_assets_status_created_id
    ON ai_generated_assets (status, created_at DESC, id DESC);

CREATE INDEX idx_ai_assets_created_id
    ON ai_generated_assets (created_at DESC, id DESC);

CREATE INDEX idx_validation_asset_created_id
    ON ai_validation_logs (ai_asset_id, created_at DESC, id DESC);

CREATE INDEX idx_ai_jobs_type_target_status_created_id
    ON ai_generation_jobs (job_type, target_type, target_id, status, created_at DESC, id DESC);
