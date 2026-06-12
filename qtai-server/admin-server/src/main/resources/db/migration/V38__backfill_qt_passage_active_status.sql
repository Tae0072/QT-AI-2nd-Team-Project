-- V31 added moderation columns with PENDING_REVIEW as the DB default.
-- Rows that already existed before moderation were already public content, so keep
-- past/today passages visible after the ACTIVE user-exposure gate is deployed.
UPDATE qt_passages
SET status = 'ACTIVE',
    published_at = COALESCE(published_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE status = 'PENDING_REVIEW'
  AND published_at IS NULL
  AND hidden_at IS NULL
  AND qt_date <= CURRENT_DATE;
