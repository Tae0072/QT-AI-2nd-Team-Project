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

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
import org.slf4j.MDC;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListItem;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiValidationLogItem;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetResult;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetResult;

class AdminAiAssetControllerTest {

    private RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    private ListAdminAiAssetsUseCase listAdminAiAssetsUseCase;
    private GetAdminAiAssetUseCase getAdminAiAssetUseCase;
    private ReviewAiAssetUseCase reviewAiAssetUseCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        regenerateAiAssetUseCase = org.mockito.Mockito.mock(RegenerateAiAssetUseCase.class);
        listAdminAiAssetsUseCase = org.mockito.Mockito.mock(ListAdminAiAssetsUseCase.class);
        getAdminAiAssetUseCase = org.mockito.Mockito.mock(GetAdminAiAssetUseCase.class);
        reviewAiAssetUseCase = org.mockito.Mockito.mock(ReviewAiAssetUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-21T01:30:00Z"), ZoneId.of("Asia/Seoul"));
        AdminAiAssetController controller = new AdminAiAssetController(
                regenerateAiAssetUseCase,
                listAdminAiAssetsUseCase,
                getAdminAiAssetUseCase,
                reviewAiAssetUseCase,
                clock
        );
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(
                objectMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter)
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void listAssetsMapsFiltersAndReturnsPageResponse() throws Exception {
        when(listAdminAiAssetsUseCase.listAdminAiAssets(any(ListAdminAiAssetsQuery.class)))
                .thenReturn(new AdminAiAssetListResponse(
                        List.of(new AdminAiAssetListItem(
                                500L,
                                "EXPLANATION",
                                "QT_PASSAGE",
                                100L,
                                "VALIDATING",
                                new AdminAiAssetListItem.PromptVersionSummary(
                                        3L,
                                        "EXPLANATION",
                                        "2026.05.1",
                                        "ACTIVE"
                                ),
                                12L,
                                "NEEDS_REVIEW",
                                true,
                                OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                        )),
                        1,
                        5,
                        12L,
                        3,
                        false,
                        false,
                        "createdAt,desc"
                ));

        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .param("assetType", "EXPLANATION")
                        .param("targetType", "QT_PASSAGE")
                        .param("status", "VALIDATING")
                        .param("promptVersionId", "3")
                        .param("checklistVersionId", "12")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(500))
                .andExpect(jsonPath("$.data.content[0].promptVersion.id").value(3))
                .andExpect(jsonPath("$.data.content[0].latestValidationResult").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.content[0].sourceLabelPresent").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.sort").value("createdAt,desc"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

        ArgumentCaptor<ListAdminAiAssetsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListAdminAiAssetsQuery.class);
        verify(listAdminAiAssetsUseCase).listAdminAiAssets(queryCaptor.capture());
        ListAdminAiAssetsQuery query = queryCaptor.getValue();
        assertThat(query.adminId()).isEqualTo(7L);
        assertThat(query.memberRole()).isEqualTo("ADMIN");
        assertThat(query.adminRole()).isEqualTo("REVIEWER");
        assertThat(query.assetType()).isEqualTo("EXPLANATION");
        assertThat(query.targetType()).isEqualTo("QT_PASSAGE");
        assertThat(query.status()).isEqualTo("VALIDATING");
        assertThat(query.promptVersionId()).isEqualTo(3L);
        assertThat(query.checklistVersionId()).isEqualTo(12L);
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(5);
    }

    @Test
    void listAssetsAllowsSuperAdmin() throws Exception {
        when(listAdminAiAssetsUseCase.listAdminAiAssets(any(ListAdminAiAssetsQuery.class)))
                .thenReturn(emptyListResponse());

        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(adminPrincipal(8L, "ADMIN_ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<ListAdminAiAssetsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListAdminAiAssetsQuery.class);
        verify(listAdminAiAssetsUseCase).listAdminAiAssets(queryCaptor.capture());
        assertThat(queryCaptor.getValue().adminRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void listAssetsReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/assets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));
        verify(listAdminAiAssetsUseCase, never()).listAdminAiAssets(any(ListAdminAiAssetsQuery.class));
    }

    @Test
    void listAssetsReturnsForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));
        verify(listAdminAiAssetsUseCase, never()).listAdminAiAssets(any(ListAdminAiAssetsQuery.class));
    }

    @Test
    void listAssetsReturnsForbiddenForOperatorAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/assets")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));
        verify(listAdminAiAssetsUseCase, never()).listAdminAiAssets(any(ListAdminAiAssetsQuery.class));
    }

    @Test
    void getAssetReturnsDetailResponseWithValidationLogs() throws Exception {
        when(getAdminAiAssetUseCase.getAdminAiAsset(any(GetAdminAiAssetQuery.class)))
                .thenReturn(new AdminAiAssetDetailResponse(
                        500L,
                        "EXPLANATION",
                        "QT_PASSAGE",
                        100L,
                        "VALIDATING",
                        payloadJson(),
                        "QT-AI 검토용 출처",
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00"),
                        null,
                        new AdminAiAssetDetailResponse.GenerationJobSummary(
                                101L,
                                "EXPLANATION",
                                "QT_PASSAGE",
                                100L,
                                3L,
                                "SUCCEEDED",
                                OffsetDateTime.parse("2026-05-21T10:00:00+09:00"),
                                OffsetDateTime.parse("2026-05-21T10:01:00+09:00"),
                                OffsetDateTime.parse("2026-05-21T10:02:00+09:00"),
                                null
                        ),
                        new AdminAiAssetDetailResponse.PromptVersionSummary(
                                3L,
                                "EXPLANATION",
                                "2026.05.1",
                                "ACTIVE"
                        ),
                        List.of(new AdminAiValidationLogItem(
                                900L,
                                300L,
                                12L,
                                2,
                                "NEEDS_REVIEW",
                                "ADMIN",
                                "출처 표시 확인 필요",
                                OffsetDateTime.parse("2026-05-21T10:40:00+09:00")
                        ))
                ));

        mockMvc.perform(get("/api/v1/admin/ai/assets/{assetId}", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(500))
                .andExpect(jsonPath("$.data.payloadJson.summary").value("검토용 요약"))
                .andExpect(jsonPath("$.data.generationJob.id").value(101))
                .andExpect(jsonPath("$.data.promptVersion.id").value(3))
                .andExpect(jsonPath("$.data.validationLogs[0].validationLogId").value(900))
                .andExpect(jsonPath("$.data.validationLogs[0].checklistVersionId").value(12))
                .andExpect(jsonPath("$.data.validationLogs[0].result").value("NEEDS_REVIEW"));

        ArgumentCaptor<GetAdminAiAssetQuery> queryCaptor =
                ArgumentCaptor.forClass(GetAdminAiAssetQuery.class);
        verify(getAdminAiAssetUseCase).getAdminAiAsset(queryCaptor.capture());
        GetAdminAiAssetQuery query = queryCaptor.getValue();
        assertThat(query.adminId()).isEqualTo(7L);
        assertThat(query.memberRole()).isEqualTo("ADMIN");
        assertThat(query.adminRole()).isEqualTo("REVIEWER");
        assertThat(query.assetId()).isEqualTo(500L);
    }

    @Test
    void getAssetMapsNotFoundToAiAssetNotFoundResponse() throws Exception {
        when(getAdminAiAssetUseCase.getAdminAiAsset(any(GetAdminAiAssetQuery.class)))
                .thenThrow(new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/ai/assets/{assetId}", 404L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A0002"));
    }

    @Test
    void approveMapsRequestAndReturnsApprovedStatus() throws Exception {
        when(reviewAiAssetUseCase.reviewAiAsset(any(ReviewAiAssetCommand.class)))
                .thenReturn(new ReviewAiAssetResult(500L, "APPROVED"));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/approve", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistVersionId": 4,
                                  "reason": "검증 기준을 충족합니다.",
                                  "activateForTarget": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assetId").value(500))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        ArgumentCaptor<ReviewAiAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(ReviewAiAssetCommand.class);
        verify(reviewAiAssetUseCase).reviewAiAsset(commandCaptor.capture());
        ReviewAiAssetCommand command = commandCaptor.getValue();
        assertThat(command.reviewerId()).isEqualTo(7L);
        assertThat(command.assetId()).isEqualTo(500L);
        assertThat(command.memberRole()).isEqualTo("ADMIN");
        assertThat(command.adminRole()).isEqualTo("REVIEWER");
        assertThat(command.action()).isEqualTo("APPROVE");
        assertThat(command.checklistVersionId()).isEqualTo(4L);
        assertThat(command.reason()).isEqualTo("검증 기준을 충족합니다.");
        assertThat(command.activateForTarget()).isTrue();
        assertThat(command.reviewedAt()).isEqualTo(OffsetDateTime.parse("2026-05-21T10:30:00+09:00"));
    }

    @Test
    void rejectMapsRequestAndReturnsRejectedStatus() throws Exception {
        when(reviewAiAssetUseCase.reviewAiAsset(any(ReviewAiAssetCommand.class)))
                .thenReturn(new ReviewAiAssetResult(500L, "REJECTED"));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/reject", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "출처 표기가 부족합니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        ArgumentCaptor<ReviewAiAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(ReviewAiAssetCommand.class);
        verify(reviewAiAssetUseCase).reviewAiAsset(commandCaptor.capture());
        assertThat(commandCaptor.getValue().action()).isEqualTo("REJECT");
        assertThat(commandCaptor.getValue().adminRole()).isEqualTo("SUPER_ADMIN");
        assertThat(commandCaptor.getValue().activateForTarget()).isFalse();
    }

    @Test
    void hideMapsRequestAndReturnsHiddenStatus() throws Exception {
        when(reviewAiAssetUseCase.reviewAiAsset(any(ReviewAiAssetCommand.class)))
                .thenReturn(new ReviewAiAssetResult(500L, "HIDDEN"));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/hide", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "승인 후 숨김 처리합니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        ArgumentCaptor<ReviewAiAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(ReviewAiAssetCommand.class);
        verify(reviewAiAssetUseCase).reviewAiAsset(commandCaptor.capture());
        assertThat(commandCaptor.getValue().action()).isEqualTo("HIDE");
        assertThat(commandCaptor.getValue().activateForTarget()).isFalse();
    }

    @Test
    void approveReturnsForbiddenForOperatorAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/approve", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistVersionId": 4,
                                  "reason": "권한 확인",
                                  "activateForTarget": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        verify(reviewAiAssetUseCase, never()).reviewAiAsset(any(ReviewAiAssetCommand.class));
    }

    @Test
    void approveMapsInvalidStatusTransitionToConflict() throws Exception {
        when(reviewAiAssetUseCase.reviewAiAsset(any(ReviewAiAssetCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/approve", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "checklistVersionId": 4,
                                  "reason": "상태 전이 확인",
                                  "activateForTarget": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("C0003"));
    }

    @Test
    void regenerateMapsRequestAndReturnsAcceptedQueuedJob() throws Exception {
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenReturn(new RegenerateAiAssetResult(
                        101L,
                        "QUEUED",
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                ));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "출처 표기가 부족합니다.",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generationJobId").value(101))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-05-21T10:30:00+09:00"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());

        ArgumentCaptor<RegenerateAiAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(RegenerateAiAssetCommand.class);
        verify(regenerateAiAssetUseCase).regenerateAiAsset(commandCaptor.capture());
        RegenerateAiAssetCommand command = commandCaptor.getValue();
        assertThat(command.adminId()).isEqualTo(7L);
        assertThat(command.assetId()).isEqualTo(500L);
        assertThat(command.memberRole()).isEqualTo("ADMIN");
        assertThat(command.adminRole()).isEqualTo("REVIEWER");
        assertThat(command.promptVersionId()).isEqualTo(3L);
        assertThat(command.requestedAt())
                .isEqualTo(OffsetDateTime.parse("2026-05-21T10:30:00+09:00"));
    }

    @Test
    void forbiddenAdminRoleReturnsForbiddenResponse() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "검토 권한 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
    }

    @Test
    void invalidAssetStatusReturnsConflictResponse() throws Exception {
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "검증 중 상태 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0003"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void responseEnvelopeUsesMdcTraceIdWhenPresent() throws Exception {
        MDC.put("traceId", "trace-ai-regenerate-001");
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenReturn(new RegenerateAiAssetResult(
                        101L,
                        "QUEUED",
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                ));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "출처 표기가 부족합니다.",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.traceId").value("trace-ai-regenerate-001"));
    }

    @Test
    void securityContextHolderAuthenticationIsUsedWhenArgumentIsMissing() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER"));
        when(regenerateAiAssetUseCase.regenerateAiAsset(any(RegenerateAiAssetCommand.class)))
                .thenReturn(new RegenerateAiAssetResult(
                        101L,
                        "QUEUED",
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                ));

        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "출처 표기가 부족합니다.",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<RegenerateAiAssetCommand> commandCaptor =
                ArgumentCaptor.forClass(RegenerateAiAssetCommand.class);
        verify(regenerateAiAssetUseCase).regenerateAiAsset(commandCaptor.capture());
        assertThat(commandCaptor.getValue().adminId()).isEqualTo(7L);
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorizedEvenWithForgedHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .header("X-Admin-Id", "7")
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "헤더 위조 시도",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
    }

    @Test
    void forgedHeadersDoNotOverrideSecurityContextRoles() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_REVIEWER"))
                        .header("X-Member-Role", "ADMIN")
                        .header("X-Admin-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "권한 헤더 위조 시도",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0003"));
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
    }

    @Test
    void nonNumericPrincipalReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/ai/assets/{assetId}/regenerate", 500L)
                        .principal(principal("admin@example.com", "ROLE_ADMIN", "ADMIN_ROLE_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "principal 형식 확인",
                                  "promptVersionId": 3
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));
        verify(regenerateAiAssetUseCase, never()).regenerateAiAsset(any(RegenerateAiAssetCommand.class));
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

    private static AdminAiAssetListResponse emptyListResponse() {
        return new AdminAiAssetListResponse(
                Collections.emptyList(),
                0,
                20,
                0L,
                0,
                true,
                true,
                "createdAt,desc"
        );
    }

    private JsonNode payloadJson() throws Exception {
        return objectMapper.readTree("""
                {
                  "summary": "검토용 요약",
                  "sourceLabel": "QT-AI 검토용 출처"
                }
                """);
    }
}
