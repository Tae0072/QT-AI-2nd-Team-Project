ALTER TABLE ai_generation_jobs
    ADD UNIQUE KEY uk_ai_generation_jobs_active_target (
        job_type,
        target_type,
        target_id,
        active_unique_key
    );
