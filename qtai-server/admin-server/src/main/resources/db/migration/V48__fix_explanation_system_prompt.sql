-- Fix EXPLANATION system prompts to the server-managed default.
-- Admin registration no longer accepts a custom system prompt, but historical
-- rows may still contain custom values from older versions.

SET @qtai_explanation_system_prompt = 'JSON 객체만 반환하세요. 객체는 explanations[]와 glossaryTerms[]를 반드시 포함해야 합니다.
각 explanation 항목은 verseId, summary, explanation을 포함해야 합니다.
각 glossaryTerms 항목은 verseId, term, meaning을 포함해야 합니다.
제공된 verseId만 사용하고, 차분하고 사실 기반이며 초심자에게 친절한 어조를 유지하세요.
provider raw response, prompt text, validation reference text, secrets, private data를 포함하지 마세요.
';

UPDATE ai_prompt_versions
SET system_prompt = REPLACE(@qtai_explanation_system_prompt, CONCAT(CHAR(13), CHAR(10)), CHAR(10)),
    temperature = COALESCE(temperature, 0.2),
    max_tokens = COALESCE(max_tokens, 2000),
    content_hash = LOWER(SHA2(CONCAT(
            prompt_type, CHAR(10),
            version, CHAR(10),
            REPLACE(@qtai_explanation_system_prompt, CONCAT(CHAR(13), CHAR(10)), CHAR(10)), CHAR(10),
            COALESCE(user_prompt_template, ''), CHAR(10),
            COALESCE(model_name, ''), CHAR(10),
            COALESCE(temperature, 0.2), CHAR(10),
            COALESCE(max_tokens, 2000)
    ), 256))
WHERE prompt_type = 'EXPLANATION';
