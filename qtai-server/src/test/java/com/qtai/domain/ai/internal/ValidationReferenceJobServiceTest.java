package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.validation.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

class ValidationReferenceJobServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-05-28T10:00:00+09:00");
    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");

    private ValidationReferenceJobRepository repository;
    private WriteAuditLogUseCase auditLogUseCase;
    private ValidationReferenceJobService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(ValidationReferenceJobRepository.class);
        auditLogUseCase = org.mockito.Mockito.mock(WriteAuditLogUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new ValidationReferenceJobService(repository, auditLogUseCase, new ObjectMapper(), clock);
    }

    @Test
    void serviceImplementsUseCases() {
        assertThat(service).isInstanceOf(CreateValidationReferenceJobUseCase.class);
        assertThat(service).isInstanceOf(GetValidationReferenceJobUseCase.class);
        assertThat(service).isInstanceOf(ExpireValidationReferenceJobUseCase.class);
    }

    @Test
    void createStoresActiveJobAndWritesSanitizedAuditSnapshot() {
        when(repository.save(any(ValidationReferenceJob.class)))
                .thenAnswer(invocation -> {
                    ValidationReferenceJob job = invocation.getArgument(0);
                    setId(job, 33L);
                    return job;
                });

        ValidationReferenceJobResponse response = service.createValidationReferenceJob(createCommand());

        assertThat(response.id()).isEqualTo(33L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(NOW);

        ArgumentCaptor<ValidationReferenceJob> jobCaptor = ArgumentCaptor.forClass(ValidationReferenceJob.class);
        verify(repository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getSourceFileHash()).isEqualTo("sha256:reference-hash");
        assertThat(jobCaptor.getValue().getStorageUri()).isEqualTo("restricted://validation/reference.pdf");

        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        AuditLogWriteRequest audit = auditCaptor.getValue();
        assertThat(audit.adminUserId()).isNull();
        assertThat(audit.actorType()).isEqualTo("SYSTEM_BATCH");
        assertThat(audit.actorId()).isNull();
        assertThat(audit.actorLabel()).isEqualTo("SYSTEM_BATCH");
        assertThat(audit.actionType()).isEqualTo("VALIDATION_REFERENCE_JOB_CREATE");
        assertThat(audit.targetType()).isEqualTo("VALIDATION_REFERENCE_JOB");
        assertThat(audit.targetId()).isEqualTo(33L);
        assertThat(audit.beforeJson()).isNull();
        assertSanitizedSnapshot(audit.afterJson());
    }

    @Test
    void getReturnsJobOrNotFound() {
        ValidationReferenceJob job = persistedJob(33L);
        when(repository.findById(33L)).thenReturn(Optional.of(job));
        when(repository.findById(404L)).thenReturn(Optional.empty());

        ValidationReferenceJobResponse response =
                service.getValidationReferenceJob(new GetValidationReferenceJobQuery(33L));

        assertThat(response.id()).isEqualTo(33L);
        assertThat(response.sourceName()).isEqualTo("검증 참조 자료");
        assertThatThrownBy(() -> service.getValidationReferenceJob(new GetValidationReferenceJobQuery(404L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_REFERENCE_JOB_NOT_FOUND));
    }

    @Test
    void expireAllowsOnlyActiveJobAndWritesAudit() {
        ValidationReferenceJob job = persistedJob(33L);
        when(repository.findById(33L)).thenReturn(Optional.of(job));

        ValidationReferenceJobResponse response =
                service.expireValidationReferenceJob(new ExpireValidationReferenceJobCommand(33L));

        assertThat(response.status()).isEqualTo("EXPIRED");
        assertThat(response.deletedAt()).isNull();

        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        AuditLogWriteRequest audit = auditCaptor.getValue();
        assertThat(audit.actionType()).isEqualTo("VALIDATION_REFERENCE_JOB_EXPIRE");
        assertSanitizedSnapshot(audit.beforeJson());
        assertSanitizedSnapshot(audit.afterJson());
        assertThat(audit.afterJson()).contains("\"status\":\"EXPIRED\"");
    }

    @Test
    void expireRejectsAlreadyExpiredJob() {
        ValidationReferenceJob job = persistedJob(33L);
        job.expire(NOW);
        when(repository.findById(33L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.expireValidationReferenceJob(new ExpireValidationReferenceJobCommand(33L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    private static CreateValidationReferenceJobCommand createCommand() {
        return new CreateValidationReferenceJobCommand(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                "restricted://validation/reference.pdf",
                "restricted://validation/index",
                EXPIRES_AT
        );
    }

    private static ValidationReferenceJob persistedJob(Long id) {
        ValidationReferenceJob job = ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                "sha256:reference-hash",
                "restricted://validation/reference.pdf",
                "restricted://validation/index",
                EXPIRES_AT,
                NOW
        );
        setId(job, id);
        return job;
    }

    private static void assertSanitizedSnapshot(String json) {
        assertThat(json)
                .contains("\"id\":33", "\"sourceName\":\"검증 참조 자료\"", "\"sourceFileName\":\"reference-notes.pdf\"")
                .contains("\"timestamp\":\"2026-05-28T10:00:00+09:00\"")
                .doesNotContain("sourceFileHash")
                .doesNotContain("storageUri")
                .doesNotContain("indexStorageUri")
                .doesNotContain("원문")
                .doesNotContain("secret")
                .doesNotContain("token")
                .doesNotContain("password");
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
