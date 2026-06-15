ALTER TABLE ai_prompt_versions
    ADD COLUMN system_prompt LONGTEXT,
    ADD COLUMN user_prompt_template LONGTEXT,
    ADD COLUMN model_name VARCHAR(100),
    ADD COLUMN temperature DOUBLE,
    ADD COLUMN max_tokens INT,
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN created_by_admin_id BIGINT,
    ADD COLUMN activated_at TIMESTAMP,
    ADD COLUMN retired_at TIMESTAMP;

UPDATE ai_prompt_versions
SET system_prompt = 'Return only a JSON object. The object must contain explanations[] and glossaryTerms[].
Each explanation item must contain verseId, summary, and explanation.
Each glossary term item must contain verseId, term, and meaning.
Use only the provided verseIds and keep the tone calm, factual, and beginner-friendly.
Do not include provider raw response, prompt text, validation reference text, secrets, or private data.
',
    user_prompt_template = 'Create explanation JSON for the following Bible verses.
Target type: {{targetType}}
Target id: {{targetId}}
{{qtPassageBlock}}Verses:
{{versesBlock}}{{commentaryBlock}}
',
    temperature = 0.2,
    max_tokens = 2000,
    content_hash = 'c08e6b04543f57a2d60fa538485ab3fc54c2f2752e7e024ec124848fd0e1ef65',
    description = 'Initial EXPLANATION prompt migrated from code defaults.',
    activated_at = created_at
WHERE prompt_type = 'EXPLANATION'
  AND (system_prompt IS NULL OR user_prompt_template IS NULL);

CREATE INDEX idx_ai_prompt_versions_type_status_created
    ON ai_prompt_versions (prompt_type, status, created_at, id);

CREATE TABLE ai_evaluation_runs (
    id                 BIGINT       AUTO_INCREMENT PRIMARY KEY,
    evaluation_set_id  BIGINT       NOT NULL,
    prompt_version_id  BIGINT       NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    total_count        INT          NOT NULL DEFAULT 0,
    passed_count       INT          NOT NULL DEFAULT 0,
    failed_count       INT          NOT NULL DEFAULT 0,
    needs_review_count INT          NOT NULL DEFAULT 0,
    requested_by_admin_id BIGINT    NOT NULL,
    started_at         TIMESTAMP    NOT NULL,
    finished_at        TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_eval_runs_set
        FOREIGN KEY (evaluation_set_id) REFERENCES ai_evaluation_sets(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_eval_runs_prompt
        FOREIGN KEY (prompt_version_id) REFERENCES ai_prompt_versions(id)
);

CREATE INDEX idx_ai_eval_runs_set_created
    ON ai_evaluation_runs (evaluation_set_id, created_at, id);
CREATE INDEX idx_ai_eval_runs_prompt_status_finished
    ON ai_evaluation_runs (prompt_version_id, status, finished_at, id);

CREATE TABLE ai_evaluation_results (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    evaluation_run_id   BIGINT       NOT NULL,
    evaluation_case_id  BIGINT       NOT NULL,
    result              VARCHAR(30)  NOT NULL,
    reason              VARCHAR(1000),
    output_summary_json LONGTEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_eval_results_run
        FOREIGN KEY (evaluation_run_id) REFERENCES ai_evaluation_runs(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_eval_results_case
        FOREIGN KEY (evaluation_case_id) REFERENCES ai_evaluation_cases(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_eval_results_run
    ON ai_evaluation_results (evaluation_run_id, id);
CREATE INDEX idx_ai_eval_results_case
    ON ai_evaluation_results (evaluation_case_id);
