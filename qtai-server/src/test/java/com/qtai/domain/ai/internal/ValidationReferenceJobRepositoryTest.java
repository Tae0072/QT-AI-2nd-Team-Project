package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ValidationReferenceJobRepositoryTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-28T10:00:00+09:00");
    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private ValidationReferenceJobRepository repository;

    @Test
    void storesValidationReferenceJobColumnsAndNullableFields() {
        ValidationReferenceJob job = ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                null,
                null,
                EXPIRES_AT,
                CREATED_AT
        );

        ValidationReferenceJob saved = repository.saveAndFlush(job);
        testEntityManager.clear();

        ValidationReferenceJob found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSourceName()).isEqualTo("검증 참조 자료");
        assertThat(found.getSourceFileName()).isEqualTo("reference-notes.pdf");
        assertThat(found.getSourceFileHash()).isEqualTo("sha256:reference-hash");
        assertThat(found.getStorageUri()).isNull();
        assertThat(found.getIndexStorageUri()).isNull();
        assertThat(found.getStatus()).isEqualTo(ValidationReferenceJobStatus.ACTIVE);
        assertThat(found.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(found.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void storesExpiredStatusWithoutDeletedAt() {
        ValidationReferenceJob job = ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                "restricted://validation/reference.pdf",
                "restricted://validation/index",
                EXPIRES_AT,
                CREATED_AT
        );
        job.expire(CREATED_AT.plusHours(1));

        ValidationReferenceJob saved = repository.saveAndFlush(job);
        testEntityManager.clear();

        ValidationReferenceJob found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ValidationReferenceJobStatus.EXPIRED);
        assertThat(found.getStorageUri()).isEqualTo("restricted://validation/reference.pdf");
        assertThat(found.getIndexStorageUri()).isEqualTo("restricted://validation/index");
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.getUpdatedAt()).isEqualTo(CREATED_AT.plusHours(1));
    }
}
