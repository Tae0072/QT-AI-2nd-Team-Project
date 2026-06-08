ALTER TABLE glossary_terms
    ADD COLUMN active_unique_key VARCHAR(20) NULL AFTER status;

UPDATE glossary_terms
SET active_unique_key = NULL
WHERE status <> 'APPROVED';

UPDATE glossary_terms glossary_term
    JOIN (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY bible_verse_id
                   ORDER BY approved_at DESC, id DESC
               ) AS row_number
        FROM glossary_terms
        WHERE status = 'APPROVED'
    ) ranked_glossary_term ON glossary_term.id = ranked_glossary_term.id
SET glossary_term.status = 'HIDDEN',
    glossary_term.active_unique_key = NULL
WHERE ranked_glossary_term.row_number > 1;

UPDATE glossary_terms
SET active_unique_key = 'ACTIVE'
WHERE status = 'APPROVED';

ALTER TABLE glossary_terms
    ADD UNIQUE KEY uk_glossary_terms_active_per_verse (bible_verse_id, active_unique_key);
