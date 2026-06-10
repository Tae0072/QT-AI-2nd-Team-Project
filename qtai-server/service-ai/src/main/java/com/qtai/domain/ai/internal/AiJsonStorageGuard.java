package com.qtai.domain.ai.internal;

import java.util.regex.Pattern;

final class AiJsonStorageGuard {

    private static final Pattern FORBIDDEN_FIELD_PATTERN = Pattern.compile(
            "\"(?:providerRawResponse|provider_raw_response|rawResponse|raw_response|"
                    + "validationReferenceText|validation_reference_text|referenceText|reference_text|"
                    + "commentaryOriginal|commentary_original)\"\\s*:"
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
