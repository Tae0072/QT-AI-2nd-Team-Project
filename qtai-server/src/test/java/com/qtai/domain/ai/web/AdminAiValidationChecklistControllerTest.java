package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

class AdminAiValidationChecklistControllerTest {

    private ListAdminAiValidationChecklistsUseCase listUseCase;
    private CreateAdminAiValidationChecklistUseCase createUseCase;
    private ActivateAdminAiValidationChecklistUseCase activateUseCase;
    private RetireAdminAiValidationChecklistUseCase retireUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        listUseCase = org.mockito.Mockito.mock(ListAdminAiValidationChecklistsUseCase.class);
        createUseCase = org.mockito.Mockito.mock(CreateAdminAiValidationChecklistUseCase.class);
        activateUseCase = org.mockito.Mockito.mock(ActivateAdminAiValidationChecklistUseCase.class);
        retireUseCase = org.mockito.Mockito.mock(RetireAdminAiValidationChecklistUseCase.class);

        AdminAiValidationChecklistController controller = new AdminAiValidationChecklistController(
                listUseCase,
                createUseCase,
                activateUseCase,
                retireUseCase
        );
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listChecklistsMapsFiltersAndReturnsPageEnvelope() throws Exception {
        when(listUseCase.listAdminAiValidationChecklists(any(ListAdminAiValidationChecklistsQuery.class)))
                .thenReturn(new AdminAiValidationChecklistListResponse(
                        List.of(response(4L, "ACTIVE")),
                        1,
                        5,
                        12L,
                        3,
                        false,
                        false,
                        "createdAt,desc,id,desc"
                ));

        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .param("checklistType", "EXPLANATION")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(4))
                .andExpect(jsonPath("$.data.content[0].checklistType").value("EXPLANATION"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.sort").value("createdAt,desc,id,desc"));

        ArgumentCaptor<ListAdminAiValidationChecklistsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListAdminAiValidationChecklistsQuery.class);
        verify(listUseCase).listAdminAiValidationChecklists(queryCaptor.capture());
        ListAdminAiValidationChecklistsQuery query = queryCaptor.getValue();
        assertThat(query.adminId()).isEqualTo(7L);
        assertThat(query.memberRole()).isEqualTo("ADMIN");
        assertThat(query.adminRole()).isEqualTo("REVIEWER");
        assertThat(query.checklistType()).isEqualTo("EXPLANATION");
        assertThat(query.status()).isEqualTo("ACTIVE");
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(5);
    }

    @Test
    void listChecklistsEnforcesReviewerAdminAuthority() throws Exception {
        when(listUseCase.listAdminAiValidationChecklists(any(ListAdminAiValidationChecklistsQuery.class)))
                .thenReturn(new AdminAiValidationChecklistListResponse(List.of(), 0, 20, 0L, 0, true, true,
                        "createdAt,desc,id,desc"));

        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists")
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_CONTENT_CREATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(8L, "ADMIN_ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(listUseCase).listAdminAiValidationChecklists(any(ListAdminAiValidationChecklistsQuery.class));
    }

    @Test
    void createChecklistMapsRequestAndReturnsCreated() throws Exception {
        when(createUseCase.createAdminAiValidationChecklist(any(CreateAdminAiValidationChecklistCommand.class)))
                .thenReturn(response(4L, "DRAFT"));

        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistType": "EXPLANATION",
                                  "version": "2026.05.1",
                                  "contentHash": "sha256:checklist-v1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createdByAdminId").doesNotExist());

        ArgumentCaptor<CreateAdminAiValidationChecklistCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAdminAiValidationChecklistCommand.class);
        verify(createUseCase).createAdminAiValidationChecklist(commandCaptor.capture());
        CreateAdminAiValidationChecklistCommand command = commandCaptor.getValue();
        assertThat(command.adminId()).isEqualTo(7L);
        assertThat(command.memberRole()).isEqualTo("ADMIN");
        assertThat(command.adminRole()).isEqualTo("REVIEWER");
        assertThat(command.checklistType()).isEqualTo("EXPLANATION");
        assertThat(command.status()).isEqualTo("DRAFT");
    }

    @Test
    void createChecklistRejectsMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistType": "",
                                  "version": "2026.05.1",
                                  "contentHash": "sha256:checklist-v1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));

        verify(createUseCase, never()).createAdminAiValidationChecklist(any(CreateAdminAiValidationChecklistCommand.class));
    }

    @Test
    void createChecklistPassesDirectActiveStatusToServiceValidation() throws Exception {
        when(createUseCase.createAdminAiValidationChecklist(any(CreateAdminAiValidationChecklistCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT));

        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistType": "EXPLANATION",
                                  "version": "2026.05.1",
                                  "contentHash": "sha256:checklist-v1",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));

        ArgumentCaptor<CreateAdminAiValidationChecklistCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAdminAiValidationChecklistCommand.class);
        verify(createUseCase).createAdminAiValidationChecklist(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo("ACTIVE");
    }

    @Test
    void activateAndRetireReturnChangedStatusResponses() throws Exception {
        when(activateUseCase.activateAdminAiValidationChecklist(any(ChangeAdminAiValidationChecklistStatusCommand.class)))
                .thenReturn(response(4L, "ACTIVE"));
        when(retireUseCase.retireAdminAiValidationChecklist(any(ChangeAdminAiValidationChecklistStatusCommand.class)))
                .thenReturn(response(4L, "RETIRED"));

        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists/{id}/activate", 4L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.activatedAt").value("2026-05-27T10:00:00+09:00"));

        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists/{id}/retire", 4L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETIRED"))
                .andExpect(jsonPath("$.data.retiredAt").value("2026-05-27T10:00:00+09:00"));

        ArgumentCaptor<ChangeAdminAiValidationChecklistStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeAdminAiValidationChecklistStatusCommand.class);
        verify(activateUseCase).activateAdminAiValidationChecklist(commandCaptor.capture());
        assertThat(commandCaptor.getValue().checklistId()).isEqualTo(4L);
        assertThat(commandCaptor.getValue().adminRole()).isEqualTo("REVIEWER");
    }

    @Test
    void missingChecklistIdMapsToChecklistNotFound() throws Exception {
        when(activateUseCase.activateAdminAiValidationChecklist(any(ChangeAdminAiValidationChecklistStatusCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.CHECKLIST_NOT_FOUND));

        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists/{id}/activate", 404L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("A0003"));
    }

    private static AdminAiValidationChecklistResponse response(Long id, String status) {
        OffsetDateTime changedAt = OffsetDateTime.parse("2026-05-27T10:00:00+09:00");
        return new AdminAiValidationChecklistResponse(
                id,
                "EXPLANATION",
                "2026.05.1",
                "sha256:checklist-v1",
                status,
                null,
                OffsetDateTime.parse("2026-05-27T09:00:00+09:00"),
                "ACTIVE".equals(status) ? changedAt : null,
                "RETIRED".equals(status) ? changedAt : null
        );
    }

    private static Authentication adminPrincipal(Long adminId, String... adminAuthorities) {
        String[] authorities = new String[adminAuthorities.length + 1];
        authorities[0] = "ROLE_ADMIN";
        System.arraycopy(adminAuthorities, 0, authorities, 1, adminAuthorities.length);
        return principal(adminId, authorities);
    }

    private static Authentication principal(Object principal, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, "N/A", grantedAuthorities);
    }
}
