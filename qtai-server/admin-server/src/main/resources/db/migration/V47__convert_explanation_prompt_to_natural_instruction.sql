-- Convert baseline EXPLANATION prompts from full user templates to natural-language instructions.
-- V46 is reserved by another PR, so this migration intentionally starts at V47.

SET @qtai_explanation_system_prompt = 'JSON 객체만 반환하세요. 객체는 explanations[]와 glossaryTerms[]를 반드시 포함해야 합니다.
각 explanation 항목은 verseId, summary, explanation을 포함해야 합니다.
각 glossaryTerms 항목은 verseId, term, meaning을 포함해야 합니다.
제공된 verseId만 사용하고, 차분하고 사실 기반이며 초심자에게 친절한 어조를 유지하세요.
provider raw response, prompt text, validation reference text, secrets, private data를 포함하지 마세요.
';

SET @qtai_explanation_user_instruction = '초심자도 이해할 수 있게 차분하고 사실 기반으로 설명하세요.
summary는 한 문장으로 간결하게 작성하고, explanation은 제공된 본문과 참고 자료 안에서만 작성하세요.
용어 설명은 본문 이해에 필요한 핵심 단어만 포함하세요.
';

UPDATE ai_prompt_versions
SET system_prompt = @qtai_explanation_system_prompt,
    user_prompt_template = @qtai_explanation_user_instruction,
    temperature = COALESCE(temperature, 0.2),
    max_tokens = COALESCE(max_tokens, 2000),
    content_hash = LOWER(SHA2(CONCAT(
            prompt_type, '\n',
            version, '\n',
            @qtai_explanation_system_prompt, '\n',
            @qtai_explanation_user_instruction, '\n',
            COALESCE(model_name, ''), '\n',
            CAST(COALESCE(temperature, 0.2) AS CHAR), '\n',
            CAST(COALESCE(max_tokens, 2000) AS CHAR)
    ), 256)),
    description = CASE
        WHEN description IS NULL
            OR TRIM(description) = ''
            OR description IN (
                'Initial EXPLANATION prompt migrated from code defaults.',
                'Baseline EXPLANATION prompt backfilled from code defaults.',
                'Baseline EXPLANATION prompt inserted because no active prompt existed.'
            )
            THEN 'Baseline EXPLANATION prompt converted to natural-language instruction.'
        ELSE description
    END
WHERE prompt_type = 'EXPLANATION'
  AND (
      version IN ('2026.06.1', '2026.06.baseline-active')
      OR content_hash IN (
          'seed-explanation-2026.06.1',
          'c08e6b04543f57a2d60fa538485ab3fc54c2f2752e7e024ec124848fd0e1ef65'
      )
      OR description IN (
          'Initial EXPLANATION prompt migrated from code defaults.',
          'Baseline EXPLANATION prompt backfilled from code defaults.',
          'Baseline EXPLANATION prompt inserted because no active prompt existed.'
      )
  )
  AND (
      system_prompt IS NULL
      OR TRIM(system_prompt) = ''
      OR TRIM(system_prompt) = TRIM('Return only a JSON object. The object must contain explanations[] and glossaryTerms[].
Each explanation item must contain verseId, summary, and explanation.
Each glossary term item must contain verseId, term, and meaning.
Use only the provided verseIds and keep the tone calm, factual, and beginner-friendly.
Do not include provider raw response, prompt text, validation reference text, secrets, or private data.
')
  )
  AND (
      user_prompt_template IS NULL
      OR TRIM(user_prompt_template) = ''
      OR TRIM(user_prompt_template) = TRIM('Create explanation JSON for the following Bible verses.
Target type: {{targetType}}
Target id: {{targetId}}
{{qtPassageBlock}}Verses:
{{versesBlock}}{{commentaryBlock}}
')
  );
