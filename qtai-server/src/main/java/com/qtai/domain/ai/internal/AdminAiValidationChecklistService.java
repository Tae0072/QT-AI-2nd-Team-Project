package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.dto.ChangeAdminAiValidationChecklistStatusCommand;
import com.qtai.domain.ai.api.dto.CreateAdminAiValidationChecklistCommand;
import com.qtai.domain.ai.api.dto.ListAdminAiValidationChecklistsQuery;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

@Service
public class AdminAiValidationChecklistService implements
        ListAdminAiValidationChecklistsUseCase,
        CreateAdminAiValidationChecklistUseCase,
        ActivateAdminAiValidationChecklistUseCase,
        RetireAdminAiValidationChecklistUseCase {

    private static final String SORT = "createdAt,desc,id,desc";
    private static final int MAX_PAGE_SIZE = 100;
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE = "AI_VALIDATION_CHECKLIST_VERSION";

    private final AiValidationChecklistVersionRepository repository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AdminAiValidationChecklistService(
            AiValidationChecklistVersionRepository repository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper
    ) {
        this(repository, auditLogUseCase, objectMapper, Clock.systemDefaultZone());
    }

    AdminAiValidationChecklistService(
            AiValidationChecklistVersionRepository repository,
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
    @Transactional(readOnly = true)
    public AdminAiValidationChecklistListResponse listAdminAiValidationChecklists(
            ListAdminAiValidationChecklistsQuery query
    ) {
        requireValidListQuery(query);
        requireAuthorizedReviewer(query.memberRole(), query.adminRole());

        AiValidationChecklistType checklistType = parseEnum(
                AiValidationChecklistType.class,
                query.checklistType(),
                "checklistType"
        );
        AiValidationChecklistStatus status = parseEnum(
                AiValidationChecklistStatus.class,
                query.status(),
                "status"
        );
        PageRequest pageRequest = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<AiValidationChecklistVersion> page = findChecklistPage(checklistType, status, pageRequest);

        return new AdminAiValidationChecklistListResponse(
                page.getContent().stream()
                        .map(AdminAiValidationChecklistService::toResponse)
                        .toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                SORT
        );
    }

    @Override
    @Transactional
    public AdminAiValidationChecklistResponse createAdminAiValidationChecklist(
            CreateAdminAiValidationChecklistCommand command
    ) {
        requireValidCreateCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());
        AiValidationChecklistType checklistType = parseEnum(
                AiValidationChecklistType.class,
                command.checklistType(),
                "checklistType"
        );
        requireDraftCreateStatus(command.status());
        if (repository.existsByChecklistTypeAndVersion(checklistType, command.version())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CHECKLIST_VERSION);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        AiValidationChecklistVersion version = repository.save(AiValidationChecklistVersion.create(
                checklistType,
                command.version(),
                command.contentHash(),
                null,
                now
        ));
        writeAudit(command.adminId(), "CHECKLIST_CREATE", version.getId(), null, snapshot(version, now));
        return toResponse(version);
    }

    @Override
    @Transactional
    public AdminAiValidationChecklistResponse activateAdminAiValidationChecklist(
            ChangeAdminAiValidationChecklistStatusCommand command
    ) {
        requireValidStatusCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());

        AiValidationChecklistType checklistType = findChecklistType(command.checklistId());
        List<AiValidationChecklistVersion> lockedVersions =
                repository.findAllByChecklistTypeForUpdate(checklistType);
        AiValidationChecklistVersion target = lockedVersions.stream()
                .filter(version -> command.checklistId().equals(version.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_NOT_FOUND));
        if (target.getStatus() != AiValidationChecklistStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (AiValidationChecklistVersion active : lockedVersions) {
            if (active.getStatus() != AiValidationChecklistStatus.ACTIVE) {
                continue;
            }
            String beforeJson = snapshot(active, now);
            active.retire(now);
            writeAudit(command.adminId(), "CHECKLIST_RETIRE", active.getId(), beforeJson, snapshot(active, now));
        }

        String beforeJson = snapshot(target, now);
        target.activate(now);
        writeAudit(command.adminId(), "CHECKLIST_ACTIVATE", target.getId(), beforeJson, snapshot(target, now));
        return toResponse(target);
    }

    private Page<AiValidationChecklistVersion> findChecklistPage(
            AiValidationChecklistType checklistType,
            AiValidationChecklistStatus status,
            PageRequest pageRequest
    ) {
        if (checklistType != null && status != null) {
            return repository.findByChecklistTypeAndStatus(checklistType, status, pageRequest);
        }
        if (checklistType != null) {
            return repository.findByChecklistType(checklistType, pageRequest);
        }
        if (status != null) {
            return repository.findByStatus(status, pageRequest);
        }
        return repository.findAll(pageRequest);
    }

    @Override
    @Transactional
    public AdminAiValidationChecklistResponse retireAdminAiValidationChecklist(
            ChangeAdminAiValidationChecklistStatusCommand command
    ) {
        requireValidStatusCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());

        AiValidationChecklistVersion target = findChecklist(command.checklistId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        String beforeJson = snapshot(target, now);
        target.retire(now);
        writeAudit(command.adminId(), "CHECKLIST_RETIRE", target.getId(), beforeJson, snapshot(target, now));
        return toResponse(target);
    }

    private AiValidationChecklistVersion findChecklist(Long checklistId) {
        return repository.findById(checklistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_NOT_FOUND));
    }

    private AiValidationChecklistType findChecklistType(Long checklistId) {
        return repository.findChecklistTypeById(checklistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_NOT_FOUND));
    }

    private void writeAudit(Long adminId, String actionType, Long targetId, String beforeJson, String afterJson) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                adminId,
                "ADMIN:" + adminId,
                actionType,
                TARGET_TYPE,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    private String snapshot(AiValidationChecklistVersion version, OffsetDateTime eventAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", version.getId());
        payload.put("checklistType", version.getChecklistType().name());
        payload.put("version", version.getVersion());
        payload.put("contentHash", version.getContentHash());
        payload.put("status", version.getStatus().name());
        payload.put("timestamp", eventAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }

    private static AdminAiValidationChecklistResponse toResponse(AiValidationChecklistVersion version) {
        return new AdminAiValidationChecklistResponse(
                version.getId(),
                version.getChecklistType().name(),
                version.getVersion(),
                version.getContentHash(),
                version.getStatus().name(),
                version.getCreatedByAdminId(),
                version.getCreatedAt(),
                version.getActivatedAt(),
                version.getRetiredAt()
        );
    }

    private static void requireAuthorizedReviewer(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole) || !("REVIEWER".equals(adminRole) || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireValidListQuery(ListAdminAiValidationChecklistsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        if (query.page() < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
    }

    private static void requireValidCreateCommand(CreateAdminAiValidationChecklistCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.adminId(), "adminId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requireText(command.checklistType(), "checklistType");
        requireText(command.version(), "version");
        requireText(command.contentHash(), "contentHash");
    }

    private static void requireValidStatusCommand(ChangeAdminAiValidationChecklistStatusCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.adminId(), "adminId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requirePositive(command.checklistId(), "checklistId");
    }

    private static void requireDraftCreateStatus(String status) {
        if (status == null || status.isBlank() || AiValidationChecklistStatus.DRAFT.name().equals(status)) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "status must be DRAFT when provided");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return value;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is not supported");
        }
    }
}
