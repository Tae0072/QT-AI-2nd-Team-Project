-- Ensure the baseline EXPLANATION prompt is usable by generation jobs.
-- V24 created only metadata rows, and some local/dev databases may have prompt
-- body columns from JPA update without V44 backfill having run.

UPDATE ai_prompt_versions
SET content_hash = CASE
        WHEN system_prompt IS NULL
            OR TRIM(system_prompt) = ''
            OR user_prompt_template IS NULL
            OR TRIM(user_prompt_template) = ''
            THEN 'c08e6b04543f57a2d60fa538485ab3fc54c2f2752e7e024ec124848fd0e1ef65'
        ELSE content_hash
    END,
    system_prompt = CASE
        WHEN system_prompt IS NULL OR TRIM(system_prompt) = '' THEN 'Return only a JSON object. The object must contain explanations[] and glossaryTerms[].
Each explanation item must contain verseId, summary, and explanation.
Each glossary term item must contain verseId, term, and meaning.
Use only the provided verseIds and keep the tone calm, factual, and beginner-friendly.
Do not include provider raw response, prompt text, validation reference text, secrets, or private data.
'
        ELSE system_prompt
    END,
    user_prompt_template = CASE
        WHEN user_prompt_template IS NULL OR TRIM(user_prompt_template) = '' THEN 'Create explanation JSON for the following Bible verses.
Target type: {{targetType}}
Target id: {{targetId}}
{{qtPassageBlock}}Verses:
{{versesBlock}}{{commentaryBlock}}
'
        ELSE user_prompt_template
    END,
    temperature = CASE
        WHEN temperature IS NULL THEN 0.2
        ELSE temperature
    END,
    max_tokens = CASE
        WHEN max_tokens IS NULL THEN 2000
        ELSE max_tokens
    END,
    description = CASE
        WHEN description IS NULL OR TRIM(description) = ''
            THEN 'Baseline EXPLANATION prompt backfilled from code defaults.'
        ELSE description
    END,
    activated_at = CASE
        WHEN status = 'ACTIVE' AND activated_at IS NULL THEN created_at
        ELSE activated_at
    END
WHERE prompt_type = 'EXPLANATION'
  AND (
      system_prompt IS NULL
      OR TRIM(system_prompt) = ''
      OR user_prompt_template IS NULL
      OR TRIM(user_prompt_template) = ''
      OR temperature IS NULL
      OR max_tokens IS NULL
      OR (status = 'ACTIVE' AND activated_at IS NULL)
  );

INSERT INTO ai_prompt_versions (
    prompt_type,
    version,
    content_hash,
    status,
    system_prompt,
    user_prompt_template,
    temperature,
    max_tokens,
    description,
    created_at,
    activated_at
)
SELECT
    'EXPLANATION',
    '2026.06.baseline-active',
    'c08e6b04543f57a2d60fa538485ab3fc54c2f2752e7e024ec124848fd0e1ef65',
    'ACTIVE',
    'Return only a JSON object. The object must contain explanations[] and glossaryTerms[].
Each explanation item must contain verseId, summary, and explanation.
Each glossary term item must contain verseId, term, and meaning.
Use only the provided verseIds and keep the tone calm, factual, and beginner-friendly.
Do not include provider raw response, prompt text, validation reference text, secrets, or private data.
',
    'Create explanation JSON for the following Bible verses.
Target type: {{targetType}}
Target id: {{targetId}}
{{qtPassageBlock}}Verses:
{{versesBlock}}{{commentaryBlock}}
',
    0.2,
    2000,
    'Baseline EXPLANATION prompt inserted because no active prompt existed.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_prompt_versions
    WHERE prompt_type = 'EXPLANATION'
      AND status = 'ACTIVE'
);
