package com.qtai.domain.ai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetResult;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetResult;
import com.qtai.domain.ai.api.admin.checklist.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.admin.checklist.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.ChangeAdminAiValidationChecklistStatusCommand;
import com.qtai.domain.ai.api.admin.checklist.dto.CreateAdminAiValidationChecklistCommand;
import com.qtai.domain.ai.api.admin.checklist.dto.ListAdminAiValidationChecklistsQuery;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;
import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.admin.AdminAuthClient.AdminAuthResult;
import com.qtai.domain.ai.client.admin.AdminAuthClient.AdminRole;

class AiServiceAdminInboundControllerTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void adminAssetListRequiresRoleAdminAndAdminAuthClientVerification() throws Exception {
        AdminAuthClient adminAuthClient = org.mockito.Mockito.mock(AdminAuthClient.class);
        ListAdminAiAssetsUseCase listUseCase = org.mockito.Mockito.mock(ListAdminAiAssetsUseCase.class);
        when(adminAuthClient.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminAuthResult(107L, 7L, AdminRole.REVIEWER));
        when(listUseCase.listAdminAiAssets(any(ListAdminAiAssetsQuery.class)))
                .thenReturn(emptyAssetList());

        MockMvc mockMvc = mockMvc(new AdminAiAssetController(
                org.mockito.Mockito.mock(RegenerateAiAssetUseCase.class),
                listUseCase,
                org.mockito.Mockito.mock(GetAdminAiAssetUseCase.class),
                org.mockito.Mockito.mock(ReviewAiAssetUseCase.class),
                new AdminAiAuthentication(adminAuthClient)
        ));

        mockMvc.perform(get("/api/v1/admin/ai/assets"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(principal(7L, "ROLE_USER")))
                .andExpect(status().isForbidden());
        verify(adminAuthClient, never()).verifyAnyRole(eq(7L), any());

        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(adminAuthClient).verifyAnyRole(eq(7L), argThat(roles -> roles.contains(AdminRole.REVIEWER)));
        verify(listUseCase).listAdminAiAssets(any(ListAdminAiAssetsQuery.class));
    }

    @Test
    void adminAssetEndpointsReturnSuccess() throws Exception {
        AdminAuthClient adminAuthClient = reviewerAuthClient();
        RegenerateAiAssetUseCase regenerateUseCase = org.mockito.Mockito.mock(RegenerateAiAssetUseCase.class);
        ListAdminAiAssetsUseCase listUseCase = org.mockito.Mockito.mock(ListAdminAiAssetsUseCase.class);
        GetAdminAiAssetUseCase getUseCase = org.mockito.Mockito.mock(GetAdminAiAssetUseCase.class);
        ReviewAiAssetUseCase reviewUseCase = org.mockito.Mockito.mock(ReviewAiAssetUseCase.class);

        when(listUseCase.listAdminAiAssets(any(ListAdminAiAssetsQuery.class))).thenReturn(emptyAssetList());
        when(getUseCase.getAdminAiAsset(any(GetAdminAiAssetQuery.class))).thenReturn(assetDetail());
        when(reviewUseCase.reviewAiAsset(any(ReviewAiAssetCommand.class)))
                .thenReturn(new ReviewAiAssetResult(501L, "APPROVED"));
        when(regenerateUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenReturn(new RegenerateAiAssetResult(
                        1001L,
                        "QUEUED",
                        OffsetDateTime.parse("2026-06-09T00:00:00+09:00")
                ));

        MockMvc mockMvc = mockMvc(new AdminAiAssetController(
                regenerateUseCase,
                listUseCase,
                getUseCase,
                reviewUseCase,
                new AdminAiAuthentication(adminAuthClient)
        ));

        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/ai/assets/501")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(501));
        mockMvc.perform(post("/api/v1/admin/ai/assets/501/approve")
                        .principal(principal(7L, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"approved\",\"activateForTarget\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value(501))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(post("/api/v1/admin/ai/assets/501/regenerate")
                        .principal(principal(7L, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retry\",\"promptVersionId\":3}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.generationJobId").value(1001));
    }

    @Test
    void adminMonitoringBatchAndChecklistEndpointsReturnSuccess() throws Exception {
        AdminAuthClient adminAuthClient = reviewerAuthClient();
        GetAdminAiMonitoringUseCase monitoringUseCase = org.mockito.Mockito.mock(GetAdminAiMonitoringUseCase.class);
        ListAdminAiBatchRunLogsUseCase batchUseCase =
                org.mockito.Mockito.mock(ListAdminAiBatchRunLogsUseCase.class);
        ListAdminAiValidationChecklistsUseCase listChecklistUseCase =
                org.mockito.Mockito.mock(ListAdminAiValidationChecklistsUseCase.class);
        CreateAdminAiValidationChecklistUseCase createChecklistUseCase =
                org.mockito.Mockito.mock(CreateAdminAiValidationChecklistUseCase.class);
        ActivateAdminAiValidationChecklistUseCase activateChecklistUseCase =
                org.mockito.Mockito.mock(ActivateAdminAiValidationChecklistUseCase.class);
        RetireAdminAiValidationChecklistUseCase retireChecklistUseCase =
                org.mockito.Mockito.mock(RetireAdminAiValidationChecklistUseCase.class);

        AdminAiValidationChecklistResponse checklist = checklistResponse("DRAFT");
        when(monitoringUseCase.getAdminAiMonitoring(any(GetAdminAiMonitoringQuery.class)))
                .thenReturn(monitoringResponse());
        when(batchUseCase.listAdminAiBatchRunLogs(any(ListAdminAiBatchRunLogsQuery.class)))
                .thenReturn(new AdminAiBatchRunLogListResponse(List.of(), 0, 20, 0, 0, true, true, "createdAt,desc"));
        when(listChecklistUseCase.listAdminAiValidationChecklists(any(ListAdminAiValidationChecklistsQuery.class)))
                .thenReturn(new AdminAiValidationChecklistListResponse(
                        List.of(checklist),
                        0,
                        20,
                        1,
                        1,
                        true,
                        true,
                        "createdAt,desc"
                ));
        when(createChecklistUseCase.createAdminAiValidationChecklist(any(CreateAdminAiValidationChecklistCommand.class)))
                .thenReturn(checklist);
        when(activateChecklistUseCase.activateAdminAiValidationChecklist(
                any(ChangeAdminAiValidationChecklistStatusCommand.class)))
                .thenReturn(checklistResponse("ACTIVE"));
        when(retireChecklistUseCase.retireAdminAiValidationChecklist(
                any(ChangeAdminAiValidationChecklistStatusCommand.class)))
                .thenReturn(checklistResponse("RETIRED"));

        AdminAiAuthentication auth = new AdminAiAuthentication(adminAuthClient);
        MockMvc mockMvc = mockMvc(
                new AdminAiMonitoringController(monitoringUseCase, auth),
                new AdminAiBatchRunLogController(batchUseCase, auth),
                new AdminAiValidationChecklistController(
                        listChecklistUseCase,
                        createChecklistUseCase,
                        activateChecklistUseCase,
                        retireChecklistUseCase,
                        auth
                )
        );

        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period.timezone").value("Asia/Seoul"));
        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(12));
        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists")
                        .principal(principal(7L, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistType": "EXPLANATION",
                                  "version": "2026.06.1",
                                  "contentHash": "hash-1",
                                  "status": "DRAFT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists/12/activate")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(post("/api/v1/admin/ai/validation-checklists/12/retire")
                        .principal(principal(7L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETIRED"));
    }

    private static AdminAuthClient reviewerAuthClient() {
        AdminAuthClient adminAuthClient = org.mockito.Mockito.mock(AdminAuthClient.class);
        when(adminAuthClient.verifyAnyRole(any(), any()))
                .thenReturn(new AdminAuthResult(107L, 7L, AdminRole.REVIEWER));
        return adminAuthClient;
    }

    private MockMvc mockMvc(Object... controllers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controllers)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private static Authentication principal(Long memberId, String authority) {
        return new UsernamePasswordAuthenticationToken(
                memberId,
                "n/a",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    private static AdminAiAssetListResponse emptyAssetList() {
        return new AdminAiAssetListResponse(List.of(), 0, 20, 0, 0, true, true, "createdAt,desc");
    }

    private AdminAiAssetDetailResponse assetDetail() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-09T00:00:00+09:00");
        return new AdminAiAssetDetailResponse(
                501L,
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                "VALIDATING",
                objectMapper.createObjectNode().put("summary", "allowed summary"),
                "QT-AI",
                now,
                null,
                null,
                null,
                List.of()
        );
    }

    private static AdminAiMonitoringResponse monitoringResponse() {
        return new AdminAiMonitoringResponse(
                new AdminAiMonitoringResponse.Period(
                        LocalDate.of(2026, 6, 9),
                        LocalDate.of(2026, 6, 9),
                        "Asia/Seoul"
                ),
                new AdminAiMonitoringResponse.GenerationJobs(0, 0, 0, 0),
                new AdminAiMonitoringResponse.Validation(0, 0, 0, 0, List.of()),
                new AdminAiMonitoringResponse.BatchRuns(0, 0, 0, List.of()),
                new AdminAiMonitoringResponse.Qa(0, 0, 0, 0, List.of()),
                List.of()
        );
    }

    private static AdminAiValidationChecklistResponse checklistResponse(String status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-09T00:00:00+09:00");
        return new AdminAiValidationChecklistResponse(
                12L,
                "EXPLANATION",
                "2026.06.1",
                "hash-1",
                status,
                107L,
                now,
                "ACTIVE".equals(status) ? now : null,
                "RETIRED".equals(status) ? now : null
        );
    }
}
