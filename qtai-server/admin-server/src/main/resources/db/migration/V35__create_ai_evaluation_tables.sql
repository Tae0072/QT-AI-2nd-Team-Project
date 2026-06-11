CREATE TABLE ai_evaluation_sets (
    id                   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    eval_type            VARCHAR(30)  NOT NULL,
    version              VARCHAR(30)  NOT NULL,
    target_type          VARCHAR(30)  NOT NULL,
    expected_policy_json LONGTEXT,
    description          VARCHAR(500),
    status               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at         TIMESTAMP,
    retired_at           TIMESTAMP,
    CONSTRAINT uk_evaluation_set_type_version UNIQUE (eval_type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_evaluation_sets_status ON ai_evaluation_sets (status, created_at);
CREATE INDEX idx_evaluation_sets_eval_target_status ON ai_evaluation_sets (eval_type, target_type, status);

CREATE TABLE ai_evaluation_cases (
    id                    BIGINT      AUTO_INCREMENT PRIMARY KEY,
    evaluation_set_id     BIGINT      NOT NULL,
    target_type           VARCHAR(30) NOT NULL,
    target_id             BIGINT,
    source_type           VARCHAR(30) NOT NULL,
    source_id             BIGINT,
    input_json            LONGTEXT    NOT NULL,
    expected_output_json  LONGTEXT,
    expected_policy_json  LONGTEXT,
    status                VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
    reviewed_by_admin_id  BIGINT,
    reviewed_at           TIMESTAMP,
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_eval_cases_set
        FOREIGN KEY (evaluation_set_id) REFERENCES ai_evaluation_sets(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_eval_cases_set_status ON ai_evaluation_cases (evaluation_set_id, status);
CREATE INDEX idx_eval_cases_source ON ai_evaluation_cases (source_type, source_id);
CREATE INDEX idx_eval_cases_target ON ai_evaluation_cases (target_type, target_id);
CREATE INDEX idx_eval_cases_reviewed_admin ON ai_evaluation_cases (reviewed_by_admin_id, reviewed_at);
