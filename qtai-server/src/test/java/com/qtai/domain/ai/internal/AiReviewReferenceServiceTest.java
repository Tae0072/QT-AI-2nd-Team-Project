package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiReviewReferenceServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-04T09:00:00+09:00");

    private ValidationReferenceJobRepository repository;
    private AiReviewReferenceService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(ValidationReferenceJobRepository.class);
        service = new AiReviewReferenceService(repository);
    }

    @Test
    void latestActiveReferenceJobIsReturnedAsMetadata() {
        ValidationReferenceJob job = referenceJob(33L);
        when(repository.findFirstByStatusOrderByCreatedAtDescIdDesc(ValidationReferenceJobStatus.ACTIVE))
                .thenReturn(Optional.of(job));

        Optional<AiReviewReferenceService.ReferenceMetadata> result = service.latestActiveReference();

        assertThat(result).isPresent();
        AiReviewReferenceService.ReferenceMetadata metadata = result.orElseThrow();
        assertThat(metadata.validationReferenceJobId()).isEqualTo(33L);
        assertThat(metadata.sourceName()).isEqualTo("검증 참조 자료");
        assertThat(metadata.sourceFileHash()).isEqualTo("sha256:reference-hash");
        assertThat(metadata.indexStorageUri()).isEqualTo("restricted://validation/index");
    }

    @Test
    void emptyWhenActiveReferenceJobDoesNotExist() {
        when(repository.findFirstByStatusOrderByCreatedAtDescIdDesc(ValidationReferenceJobStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Optional<AiReviewReferenceService.ReferenceMetadata> result = service.latestActiveReference();

        assertThat(result).isEmpty();
    }

    @Test
    void repositoryQueryUsesOnlyActiveStatusSoExpiredAndDeletedJobsAreNotSelected() {
        when(repository.findFirstByStatusOrderByCreatedAtDescIdDesc(ValidationReferenceJobStatus.ACTIVE))
                .thenReturn(Optional.empty());

        service.latestActiveReference();

        org.mockito.Mockito.verify(repository)
                .findFirstByStatusOrderByCreatedAtDescIdDesc(ValidationReferenceJobStatus.ACTIVE);
    }

    private static ValidationReferenceJob referenceJob(Long id) {
        ValidationReferenceJob job = ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                "restricted://validation/reference.pdf",
                "restricted://validation/index",
                CREATED_AT.plusDays(1),
                CREATED_AT
        );
        setId(job, id);
        return job;
    }

    private static void setId(ValidationReferenceJob target, Long id) {
        try {
            java.lang.reflect.Field field = ValidationReferenceJob.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
