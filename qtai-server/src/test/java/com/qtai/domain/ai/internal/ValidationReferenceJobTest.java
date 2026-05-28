package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class ValidationReferenceJobTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-28T10:00:00+09:00");
    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-28T11:00:00+09:00");

    @Test
    void createStartsActiveAndAllowsNullableUrisAndExpiresAt() {
        ValidationReferenceJob job = ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                null,
                null,
                null,
                CREATED_AT
        );

        assertThat(job.getSourceName()).isEqualTo("검증 참조 자료");
        assertThat(job.getSourceFileName()).isEqualTo("reference-notes.pdf");
        assertThat(job.getSourceFileHash()).isEqualTo("sha256:reference-hash");
        assertThat(job.getStorageUri()).isNull();
        assertThat(job.getIndexStorageUri()).isNull();
        assertThat(job.getStatus()).isEqualTo(ValidationReferenceJobStatus.ACTIVE);
        assertThat(job.getExpiresAt()).isNull();
        assertThat(job.getDeletedAt()).isNull();
        assertThat(job.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(job.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void requiredTextFieldsMustNotBeBlank() {
        assertThatThrownBy(() -> ValidationReferenceJob.create(
                " ",
                "reference-notes.pdf",
                "sha256:reference-hash",
                null,
                null,
                EXPIRES_AT,
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceName must not be blank");

        assertThatThrownBy(() -> ValidationReferenceJob.create(
                "검증 참조 자료",
                "",
                "sha256:reference-hash",
                null,
                null,
                EXPIRES_AT,
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceFileName must not be blank");

        assertThatThrownBy(() -> ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                null,
                null,
                null,
                EXPIRES_AT,
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceFileHash must not be blank");
    }

    @Test
    void activeJobCanExpireWithoutDeletedAt() {
        ValidationReferenceJob job = newJob();

        job.expire(UPDATED_AT);

        assertThat(job.getStatus()).isEqualTo(ValidationReferenceJobStatus.EXPIRED);
        assertThat(job.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(job.getDeletedAt()).isNull();
    }

    @Test
    void expiredOrDeletedJobCannotExpireAgain() {
        ValidationReferenceJob expired = newJob();
        expired.expire(UPDATED_AT);
        ValidationReferenceJob deleted = newJob();
        deleted.markDeleted(UPDATED_AT);

        assertInvalidTransition(() -> expired.expire(UPDATED_AT.plusMinutes(1)));
        assertInvalidTransition(() -> deleted.expire(UPDATED_AT.plusMinutes(1)));
    }

    private static ValidationReferenceJob newJob() {
        return ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                "restricted://validation/reference.pdf",
                "restricted://validation/index",
                EXPIRES_AT,
                CREATED_AT
        );
    }

    private static void assertInvalidTransition(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }
}
