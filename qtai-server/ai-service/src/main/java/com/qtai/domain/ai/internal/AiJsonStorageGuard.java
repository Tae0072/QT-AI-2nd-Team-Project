package com.qtai.domain.ai.internal;

import java.util.regex.Pattern;

final class AiJsonStorageGuard {

    private static final Pattern FORBIDDEN_FIELD_PATTERN = Pattern.compile(
            "\"(?:prompt|promptText|prompt_text|providerRawResponse|provider_raw_response|rawResponse|raw_response|"
                    + "validationReferenceText|validation_reference_text|referenceText|reference_text|"
                    + "commentaryOriginal|commentary_original|scriptureText|scripture_text|bibleText|bible_text|"
                    + "koreanText|korean_text|englishText|english_text|credentialValue|credential_value|"
                    + "dbConnectionValue|db_connection_value|authHeaderValue|auth_header_value)\"\\s*:"
    );

    private AiJsonStorageGuard() {
    }

    static String rejectRawProviderOrReferenceText(String json, String fieldName) {
        if (json == null) {
            return null;
        }
        if (FORBIDDEN_FIELD_PATTERN.matcher(json).find()) {
            throw new IllegalArgumentException(
                    fieldName + " must not store forbidden provider or validation reference fields"
            );
        }
        return json;
    }
}
