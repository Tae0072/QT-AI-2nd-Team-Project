package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.checklist.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.admin.checklist.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.ChangeAdminAiValidationChecklistStatusCommand;
import com.qtai.domain.ai.api.admin.checklist.dto.CreateAdminAiValidationChecklistCommand;
import com.qtai.domain.ai.api.admin.checklist.dto.ListAdminAiValidationChecklistsQuery;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

class AdminAiValidationChecklistServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-05-27T10:00:00+09:00");

    private AiValidationChecklistVersionRepository repository;
    private WriteAuditLogUseCase auditLogUseCase;
    private AdminAiValidationChecklistService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(AiValidationChecklistVersionRepository.class);
        auditLogUseCase = org.mockito.Mockito.mock(WriteAuditLogUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-27T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new AdminAiValidationChecklistService(repository, auditLogUseCase, new ObjectMapper(), clock);
    }

    @Test
    void serviceImplementsChecklistUseCases() {
        assertThat(service).isInstanceOf(ListAdminAiValidationChecklistsUseCase.class);
        assertThat(service).isInstanceOf(CreateAdminAiValidationChecklistUseCase.class);
        assertThat(service).isInstanceOf(ActivateAdminAiValidationChecklistUseCase.class);
        assertThat(service).isInstanceOf(RetireAdminAiValidationChecklistUseCase.class);
    }

    @Test
    void listChecklistsRequiresReviewerOrSuperAdminAndReturnsPage() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(version(4L, AiValidationChecklistStatus.ACTIVE)), Pageable.ofSize(20), 1));

        AdminAiValidationChecklistListResponse response = service.listAdminAiValidationChecklists(
                listQuery("REVIEWER")
        );
        service.listAdminAiValidationChecklists(listQuery("SUPER_ADMIN"));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("ACTIVE");
        assertThat(response.sort()).isEqualTo("createdAt,desc,id,desc");
        assertForbidden(() -> service.listAdminAiValidationChecklists(listQuery("OPERATOR")));
        assertForbidden(() -> service.listAdminAiValidationChecklists(listQuery("CONTENT_CREATOR")));
        assertForbidden(() -> service.listAdminAiValidationChecklists(new ListAdminAiValidationChecklistsQuery(
                7L, "USER", "REVIEWER", null, null, 0, 20
        )));
    }

    @Test
    void listChecklistsUsesRepositoryMethodByFilterCombination() {
        PageImpl<AiValidationChecklistVersion> emptyPage = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);
        when(repository.findByChecklistType(eq(AiValidationChecklistType.EXPLANATION), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(repository.findByStatus(eq(AiValidationChecklistStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(repository.findByChecklistTypeAndStatus(eq(AiValidationChecklistType.EXPLANATION),
                eq(AiValidationChecklistStatus.ACTIVE), any(Pageable.class))).thenReturn(emptyPage);

        service.listAdminAiValidationChecklists(new ListAdminAiValidationChecklistsQuery(
                7L, "ADMIN", "REVIEWER", "EXPLANATION", null, 0, 20
        ));
        service.listAdminAiValidationChecklists(new ListAdminAiValidationChecklistsQuery(
                7L, "ADMIN", "REVIEWER", null, "ACTIVE", 0, 20
        ));
        service.listAdminAiValidationChecklists(new ListAdminAiValidationChecklistsQuery(
                7L, "ADMIN", "REVIEWER", "EXPLANATION", "ACTIVE", 0, 20
        ));

        verify(repository).findByChecklistType(eq(AiValidationChecklistType.EXPLANATION), any(Pageable.class));
        verify(repository).findByStatus(eq(AiValidationChecklistStatus.ACTIVE), any(Pageable.class));
        verify(repository).findByChecklistTypeAndStatus(eq(AiValidationChecklistType.EXPLANATION),
                eq(AiValidationChecklistStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void createChecklistStoresDraftWithNullCreatedByAdminIdAndWritesAudit() {
        when(repository.existsByChecklistTypeAndVersion(AiValidationChecklistType.EXPLANATION, "2026.05.1"))
                .thenReturn(false);
        when(repository.save(any(AiValidationChecklistVersion.class)))
                .thenAnswer(invocation -> {
                    AiValidationChecklistVersion version = invocation.getArgument(0);
                    setId(version, 4L);
                    return version;
                });

        AdminAiValidationChecklistResponse response = service.createAdminAiValidationChecklist(createCommand("DRAFT"));

        assertThat(response.id()).isEqualTo(4L);
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.createdByAdminId()).isNull();

        ArgumentCaptor<AiValidationChecklistVersion> versionCaptor =
                ArgumentCaptor.forClass(AiValidationChecklistVersion.class);
        verify(repository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getCreatedByAdminId()).isNull();

        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("CHECKLIST_CREATE");
        assertThat(auditCaptor.getValue().targetType()).isEqualTo("AI_VALIDATION_CHECKLIST_VERSION");
        assertThat(auditCaptor.getValue().afterJson())
                .contains("\"id\":4", "\"status\":\"DRAFT\"")
                .doesNotContain("SOURCE_REQUIRED");
    }

    @Test
    void createChecklistRejectsActiveStatusAndDuplicateVersion() {
        assertThatThrownBy(() -> service.createAdminAiValidationChecklist(createCommand("ACTIVE")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        when(repository.existsByChecklistTypeAndVersion(AiValidationChecklistType.EXPLANATION, "2026.05.1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createAdminAiValidationChecklist(createCommand("DRAFT")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CHECKLIST_VERSION));
    }

    @Test
    void activateChecklistRetiresExistingActiveChecklistAndWritesAuditLogs() {
        AiValidationChecklistVersion target = version(4L, AiValidationChecklistStatus.DRAFT);
        AiValidationChecklistVersion active = version(3L, AiValidationChecklistStatus.ACTIVE);
        when(repository.findChecklistTypeById(4L)).thenReturn(Optional.of(AiValidationChecklistType.EXPLANATION));
        when(repository.findAllByChecklistTypeForUpdate(AiValidationChecklistType.EXPLANATION))
                .thenReturn(List.of(active, target));

        AdminAiValidationChecklistResponse response = service.activateAdminAiValidationChecklist(statusCommand(4L));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(target.getStatus()).isEqualTo(AiValidationChecklistStatus.ACTIVE);
        assertThat(active.getStatus()).isEqualTo(AiValidationChecklistStatus.RETIRED);

        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase, org.mockito.Mockito.times(2)).write(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditLogWriteRequest::actionType)
                .containsExactly("CHECKLIST_RETIRE", "CHECKLIST_ACTIVATE");
    }

    @Test
    void retireChecklistAllowsOnlyActiveStatus() {
        AiValidationChecklistVersion draft = version(4L, AiValidationChecklistStatus.DRAFT);
        when(repository.findById(4L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.retireAdminAiValidationChecklist(statusCommand(4L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));

        AiValidationChecklistVersion active = version(5L, AiValidationChecklistStatus.ACTIVE);
        when(repository.findById(5L)).thenReturn(Optional.of(active));
        AdminAiValidationChecklistResponse response = service.retireAdminAiValidationChecklist(statusCommand(5L));

        assertThat(response.status()).isEqualTo("RETIRED");
        verify(auditLogUseCase).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void statusChangeThrowsChecklistNotFoundForMissingId() {
        when(repository.findChecklistTypeById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateAdminAiValidationChecklist(statusCommand(404L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHECKLIST_NOT_FOUND));
    }

    private static ListAdminAiValidationChecklistsQuery listQuery(String adminRole) {
        return new ListAdminAiValidationChecklistsQuery(7L, "ADMIN", adminRole, null, null, 0, 20);
    }

    private static CreateAdminAiValidationChecklistCommand createCommand(String status) {
        return new CreateAdminAiValidationChecklistCommand(
                7L,
                "ADMIN",
                "REVIEWER",
                "EXPLANATION",
                "2026.05.1",
                "sha256:checklist-v1",
                status
        );
    }

    private static ChangeAdminAiValidationChecklistStatusCommand statusCommand(Long id) {
        return new ChangeAdminAiValidationChecklistStatusCommand(7L, "ADMIN", "REVIEWER", id);
    }

    private static AiValidationChecklistVersion version(Long id, AiValidationChecklistStatus status) {
        AiValidationChecklistVersion version = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.05." + id,
                "sha256:checklist-" + id,
                null,
                NOW.minusHours(1)
        );
        setId(version, id);
        if (status == AiValidationChecklistStatus.ACTIVE) {
            version.activate(NOW.minusMinutes(30));
        }
        if (status == AiValidationChecklistStatus.RETIRED) {
            version.activate(NOW.minusMinutes(30));
            version.retire(NOW.minusMinutes(10));
        }
        return version;
    }

    private static void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private static void setId(AiValidationChecklistVersion target, Long id) {
        try {
            java.lang.reflect.Field field = AiValidationChecklistVersion.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
