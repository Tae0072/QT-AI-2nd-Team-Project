package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.dto.ValidationReferenceJobResponse;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

@Service
public class ValidationReferenceJobService implements
        CreateValidationReferenceJobUseCase,
        GetValidationReferenceJobUseCase,
        ExpireValidationReferenceJobUseCase {

    private static final int SOURCE_NAME_MAX_LENGTH = 150;
    private static final int SOURCE_FILE_NAME_MAX_LENGTH = 255;
    private static final int SOURCE_FILE_HASH_MAX_LENGTH = 100;
    private static final int URI_MAX_LENGTH = 500;
    private static final String ACTOR_TYPE_SYSTEM_BATCH = "SYSTEM_BATCH";
    private static final String TARGET_TYPE = "VALIDATION_REFERENCE_JOB";
    private static final String ACTION_CREATE = "VALIDATION_REFERENCE_JOB_CREATE";
    private static final String ACTION_EXPIRE = "VALIDATION_REFERENCE_JOB_EXPIRE";

    private final ValidationReferenceJobRepository repository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ValidationReferenceJobService(
            ValidationReferenceJobRepository repository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper
    ) {
        this(repository, auditLogUseCase, objectMapper, Clock.systemDefaultZone());
    }

    ValidationReferenceJobService(
            ValidationReferenceJobRepository repository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ValidationReferenceJobResponse createValidationReferenceJob(CreateValidationReferenceJobCommand command) {
        requireValidCommand(command);
        OffsetDateTime now = OffsetDateTime.now(clock);
        ValidationReferenceJob job = repository.save(ValidationReferenceJob.create(
                command.sourceName(),
                command.sourceFileName(),
                command.sourceFileHash(),
                command.storageUri(),
                command.indexStorageUri(),
                command.expiresAt(),
                now
        ));
        writeAudit(ACTION_CREATE, job.getId(), null, snapshot(job, now));
        return toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationReferenceJobResponse getValidationReferenceJob(GetValidationReferenceJobQuery query) {
        requireValidQuery(query);
        return toResponse(findJob(query.jobId()));
    }

    @Override
    @Transactional
    public ValidationReferenceJobResponse expireValidationReferenceJob(ExpireValidationReferenceJobCommand command) {
        requireValidCommand(command);
        ValidationReferenceJob job = findJob(command.jobId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        String beforeJson = snapshot(job, now);
        job.expire(now);
        writeAudit(ACTION_EXPIRE, job.getId(), beforeJson, snapshot(job, now));
        return toResponse(job);
    }

    private ValidationReferenceJob findJob(Long jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_REFERENCE_JOB_NOT_FOUND));
    }

    private void writeAudit(String actionType, Long targetId, String beforeJson, String afterJson) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_SYSTEM_BATCH,
                null,
                ACTOR_TYPE_SYSTEM_BATCH,
                actionType,
                TARGET_TYPE,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    private String snapshot(ValidationReferenceJob job, OffsetDateTime eventAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", job.getId());
        payload.put("sourceName", job.getSourceName());
        payload.put("sourceFileName", job.getSourceFileName());
        payload.put("status", job.getStatus().name());
        payload.put("expiresAt", toStringOrNull(job.getExpiresAt()));
        payload.put("deletedAt", toStringOrNull(job.getDeletedAt()));
        payload.put("timestamp", formatOffsetDateTime(eventAt));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }

    private static String toStringOrNull(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return formatOffsetDateTime(value);
    }

    private static String formatOffsetDateTime(OffsetDateTime value) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
    }

    private static ValidationReferenceJobResponse toResponse(ValidationReferenceJob job) {
        return new ValidationReferenceJobResponse(
                job.getId(),
                job.getSourceName(),
                job.getSourceFileName(),
                job.getStatus().name(),
                job.getExpiresAt(),
                job.getDeletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private static void requireValidCommand(CreateValidationReferenceJobCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requireText(command.sourceName(), "sourceName", SOURCE_NAME_MAX_LENGTH);
        requireText(command.sourceFileName(), "sourceFileName", SOURCE_FILE_NAME_MAX_LENGTH);
        requireText(command.sourceFileHash(), "sourceFileHash", SOURCE_FILE_HASH_MAX_LENGTH);
        requireLengthWhenPresent(command.storageUri(), "storageUri", URI_MAX_LENGTH);
        requireLengthWhenPresent(command.indexStorageUri(), "indexStorageUri", URI_MAX_LENGTH);
    }

    private static void requireValidQuery(GetValidationReferenceJobQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.jobId(), "jobId");
    }

    private static void requireValidCommand(ExpireValidationReferenceJobCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.jobId(), "jobId");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return requireLengthWhenPresent(value, fieldName, maxLength);
    }

    private static String requireLengthWhenPresent(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    fieldName + " length must be less than or equal to " + maxLength
            );
        }
        return value;
    }
}
