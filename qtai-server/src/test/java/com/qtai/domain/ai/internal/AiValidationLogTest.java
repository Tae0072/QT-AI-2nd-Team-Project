package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class AiValidationLogTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-20T04:04:00+09:00");

    @Test
    void layerLessThanOneIsRejected() {
        assertThatThrownBy(() -> newLog(0, "{\"result\":\"passed\"}", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("layer must be greater than zero");
    }

    @Test
    void longErrorMessageIsTruncated() {
        String longMessage = "x".repeat(1_001);

        AiValidationLog log = newLog(1, "{\"result\":\"rejected\"}", longMessage);

        assertThat(log.getErrorMessage()).hasSize(1_000);
    }

    @Test
    void checklistJsonCannotStoreValidationReferenceText() {
        assertThatThrownBy(() -> newLog(
                2,
                "{\"validationReferenceText\":\"copyrighted commentary original\"}",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checklistJson");
    }

    @Test
    void checklistJsonCannotStoreProviderRawResponse() {
        assertThatThrownBy(() -> newLog(
                2,
                "{\"providerRawResponse\":\"full provider response\"}",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checklistJson");
    }

    private static AiValidationLog newLog(int layer, String checklistJson, String errorMessage) {
        return AiValidationLog.create(
                2L,
                layer,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                4L,
                checklistJson,
                errorMessage,
                CREATED_AT
        );
    }
}
