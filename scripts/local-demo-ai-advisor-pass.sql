-- [LOCAL/DEMO ONLY] AI asset approval-flow seed.
-- This script is intentionally outside Flyway migrations and must not be wired into production seed.
-- Purpose: keep 2-3 VALIDATING assets with AUTO PASSED + ADVISOR PASSED so admin approval can be demonstrated.
--
-- Usage example:
--   docker exec -i qtai-mysql mysql --default-character-set=utf8mb4 -uqtai -p<DB_PASSWORD> qtai < scripts/local-demo-ai-advisor-pass.sql

SET @now := NOW(6);

INSERT INTO ai_validation_logs (
    ai_asset_id,
    validation_reference_job_id,
    checklist_version_id,
    layer,
    result,
    checklist_json,
    reviewer_type,
    error_message,
    created_at
)
SELECT
    asset.id,
    NULL,
    auto_log.checklist_version_id,
    2,
    'PASSED',
    JSON_OBJECT(
        'validator', 'LOCAL_DEMO_ADVISOR_PASS_SEED',
        'scope', 'local-demo-only',
        'note', 'Manual demo seed for admin approval flow'
    ),
    'ADVISOR',
    NULL,
    @now
FROM ai_generated_assets asset
JOIN ai_validation_logs auto_log
  ON auto_log.id = (
    SELECT latest_auto.id
    FROM ai_validation_logs latest_auto
    WHERE latest_auto.ai_asset_id = asset.id
      AND latest_auto.layer = 1
      AND latest_auto.reviewer_type = 'AUTO'
    ORDER BY latest_auto.created_at DESC, latest_auto.id DESC
    LIMIT 1
  )
LEFT JOIN ai_validation_logs advisor_log
  ON advisor_log.id = (
    SELECT latest_advisor.id
    FROM ai_validation_logs latest_advisor
    WHERE latest_advisor.ai_asset_id = asset.id
      AND latest_advisor.layer = 2
      AND latest_advisor.reviewer_type = 'ADVISOR'
    ORDER BY latest_advisor.created_at DESC, latest_advisor.id DESC
    LIMIT 1
  )
WHERE asset.status = 'VALIDATING'
  AND auto_log.result = 'PASSED'
  AND (advisor_log.id IS NULL OR advisor_log.result = 'NEEDS_REVIEW')
ORDER BY asset.created_at DESC, asset.id DESC
LIMIT 3;

SELECT
    asset.id AS ai_asset_id,
    asset.status AS asset_status,
    auto_log.result AS auto_result,
    advisor_log.result AS advisor_result,
    advisor_log.created_at AS advisor_created_at
FROM ai_generated_assets asset
JOIN ai_validation_logs auto_log
  ON auto_log.id = (
    SELECT latest_auto.id
    FROM ai_validation_logs latest_auto
    WHERE latest_auto.ai_asset_id = asset.id
      AND latest_auto.layer = 1
      AND latest_auto.reviewer_type = 'AUTO'
    ORDER BY latest_auto.created_at DESC, latest_auto.id DESC
    LIMIT 1
  )
JOIN ai_validation_logs advisor_log
  ON advisor_log.id = (
    SELECT latest_advisor.id
    FROM ai_validation_logs latest_advisor
    WHERE latest_advisor.ai_asset_id = asset.id
      AND latest_advisor.layer = 2
      AND latest_advisor.reviewer_type = 'ADVISOR'
    ORDER BY latest_advisor.created_at DESC, latest_advisor.id DESC
    LIMIT 1
  )
WHERE asset.status = 'VALIDATING'
  AND auto_log.result = 'PASSED'
  AND advisor_log.result = 'PASSED'
ORDER BY advisor_log.created_at DESC, asset.id DESC
LIMIT 3;
