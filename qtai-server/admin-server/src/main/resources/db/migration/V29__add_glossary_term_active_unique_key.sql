ALTER TABLE glossary_terms
    ADD COLUMN active_unique_key VARCHAR(20) NULL AFTER status;

UPDATE glossary_terms
SET active_unique_key = NULL
WHERE status <> 'APPROVED';

UPDATE glossary_terms AS stale
    JOIN glossary_terms AS keeper
        ON keeper.bible_verse_id = stale.bible_verse_id
        AND keeper.status = 'APPROVED'
        AND stale.status = 'APPROVED'
        AND (
            keeper.approved_at > stale.approved_at
            OR (keeper.approved_at = stale.approved_at AND keeper.id > stale.id)
            OR (keeper.approved_at IS NOT NULL AND stale.approved_at IS NULL)
            OR (keeper.approved_at IS NULL AND stale.approved_at IS NULL AND keeper.id > stale.id)
        )
SET stale.status = 'HIDDEN',
    stale.active_unique_key = NULL;

UPDATE glossary_terms
SET active_unique_key = 'ACTIVE'
WHERE status = 'APPROVED';

ALTER TABLE glossary_terms
    ADD UNIQUE KEY uk_glossary_terms_active_per_verse (bible_verse_id, active_unique_key);
