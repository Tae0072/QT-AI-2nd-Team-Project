package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiValidationChecklistVersionTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-27T09:00:00+09:00");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-05-27T10:00:00+09:00");

    @Test
    void createStartsAsDraftAndKeepsAdminIdNullable() {
        AiValidationChecklistVersion version = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.1",
                "sha256:checklist-v1",
                null,
                CREATED_AT
        );

        assertThat(version.getChecklistType()).isEqualTo(AiValidationChecklistType.EXPLANATION);
        assertThat(version.getStatus()).isEqualTo(AiValidationChecklistStatus.DRAFT);
        assertThat(version.getCreatedByAdminId()).isNull();
        assertThat(version.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void draftCanBeActivatedAndActiveCanBeRetired() {
        AiValidationChecklistVersion version = newVersion();

        version.activate(CHANGED_AT);
        version.retire(CHANGED_AT.plusMinutes(1));

        assertThat(version.getStatus()).isEqualTo(AiValidationChecklistStatus.RETIRED);
        assertThat(version.getActivatedAt()).isEqualTo(CHANGED_AT);
        assertThat(version.getRetiredAt()).isEqualTo(CHANGED_AT.plusMinutes(1));
    }

    @Test
    void invalidStatusTransitionsAreRejected() {
        AiValidationChecklistVersion version = newVersion();

        assertThatThrownBy(() -> version.retire(CHANGED_AT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));

        version.activate(CHANGED_AT);
        assertThatThrownBy(() -> version.activate(CHANGED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void requiredFieldsAreValidated() {
        assertThatThrownBy(() -> AiValidationChecklistVersion.create(
                null,
                "2026.05.1",
                "sha256:checklist-v1",
                null,
                CREATED_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("checklistType");

        assertThatThrownBy(() -> AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                " ",
                "sha256:checklist-v1",
                null,
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must not be blank");
    }

    private static AiValidationChecklistVersion newVersion() {
        return AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.1",
                "sha256:checklist-v1",
                null,
                CREATED_AT
        );
    }
}
