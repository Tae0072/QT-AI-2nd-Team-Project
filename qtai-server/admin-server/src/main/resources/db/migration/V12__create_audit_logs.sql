CREATE TABLE audit_logs (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT,
    actor_type    VARCHAR(30)  NOT NULL,
    actor_id      BIGINT,
    actor_label   VARCHAR(100) NOT NULL,
    action_type   VARCHAR(50)  NOT NULL,
    target_type   VARCHAR(50)  NOT NULL,
    target_id     BIGINT,
    before_json   LONGTEXT,
    after_json    LONGTEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_actor_created ON audit_logs (actor_type, actor_id, created_at);
CREATE INDEX idx_audit_target ON audit_logs (target_type, target_id);
CREATE INDEX idx_audit_action_created ON audit_logs (action_type, created_at);
