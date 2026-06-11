-- V18__create_mission.sql
-- mission 도메인 엔티티(MissionDefinition, MemberMissionProgress) 누락 마이그레이션 보강 (PR #141).
-- ERD §2.23 mission_definitions(미션 정의), §2.24 member_mission_progress(회원 진행률).

CREATE TABLE mission_definitions (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL,
    title         VARCHAR(100) NOT NULL,
    metric_type   VARCHAR(30)  NOT NULL,
    period_type   VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    target_count  INT          NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME,
    UNIQUE KEY uk_mission_definitions_code (code),
    INDEX idx_mission_definitions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_mission_progress (
    id                     BIGINT        AUTO_INCREMENT PRIMARY KEY,
    member_id              BIGINT        NOT NULL,
    mission_definition_id  BIGINT        NOT NULL,
    period_start_date      DATE          NOT NULL,
    period_end_date        DATE          NOT NULL,
    current_count          INT           NOT NULL DEFAULT 0,
    target_count_snapshot  INT           NOT NULL,
    progress_rate          DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    completed_at           DATETIME,
    last_calculated_at     DATETIME,
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME,
    UNIQUE KEY uk_member_mission_period (member_id, mission_definition_id, period_start_date),
    INDEX idx_member_mission_member_period (member_id, period_start_date, period_end_date),
    CONSTRAINT fk_mmp_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mmp_definition FOREIGN KEY (mission_definition_id) REFERENCES mission_definitions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
