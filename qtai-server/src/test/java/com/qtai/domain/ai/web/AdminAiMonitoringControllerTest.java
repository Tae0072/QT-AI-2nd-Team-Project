package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.qtai.domain.ai.api.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.dto.GetAdminAiMonitoringQuery;

class AdminAiMonitoringControllerTest {

    private GetAdminAiMonitoringUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = org.mockito.Mockito.mock(GetAdminAiMonitoringUseCase.class);
        AdminAiMonitoringController controller = new AdminAiMonitoringController(useCase);
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
    void getMonitoringMapsQueryAndReturnsSummary() throws Exception {
        when(useCase.getAdminAiMonitoring(any(GetAdminAiMonitoringQuery.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR"))
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period.from").value("2026-06-01"))
                .andExpect(jsonPath("$.data.period.to").value("2026-06-02"))
                .andExpect(jsonPath("$.data.period.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.generationJobs.queued").value(3))
                .andExpect(jsonPath("$.data.validation.failureReasons[0].resultCode").value("SOURCE_MISSING"))
                .andExpect(jsonPath("$.data.batchRuns.latestFailures[0].batchName")
                        .value("AI_DAILY_QT_VERSE_EXPLANATION_SEED"))
                .andExpect(jsonPath("$.data.qa.requested").value(0))
                .andExpect(jsonPath("$.data.checklists[0].checklistType").value("EXPLANATION"));

        ArgumentCaptor<GetAdminAiMonitoringQuery> queryCaptor =
                ArgumentCaptor.forClass(GetAdminAiMonitoringQuery.class);
        verify(useCase).getAdminAiMonitoring(queryCaptor.capture());
        GetAdminAiMonitoringQuery query = queryCaptor.getValue();
        assertThat(query.adminId()).isEqualTo(7L);
        assertThat(query.memberRole()).isEqualTo("ADMIN");
        assertThat(query.adminRole()).isEqualTo("OPERATOR");
        assertThat(query.from()).isEqualTo("2026-06-01");
        assertThat(query.to()).isEqualTo("2026-06-02");
    }

    @Test
    void getMonitoringAllowsDefaultPeriodWithoutDateParams() throws Exception {
        when(useCase.getAdminAiMonitoring(any(GetAdminAiMonitoringQuery.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk());

        ArgumentCaptor<GetAdminAiMonitoringQuery> queryCaptor =
                ArgumentCaptor.forClass(GetAdminAiMonitoringQuery.class);
        verify(useCase).getAdminAiMonitoring(queryCaptor.capture());
        assertThat(queryCaptor.getValue().from()).isNull();
        assertThat(queryCaptor.getValue().to()).isNull();
        assertThat(queryCaptor.getValue().adminRole()).isEqualTo("REVIEWER");
    }

    @Test
    void getMonitoringEnforcesMonitoringAdminAuthority() throws Exception {
        when(useCase.getAdminAiMonitoring(any(GetAdminAiMonitoringQuery.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/ai/monitoring"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_CONTENT_CREATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(adminPrincipal(8L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/ai/monitoring")
                        .principal(adminPrincipal(9L, "ADMIN_ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk());

        verify(useCase, times(3)).getAdminAiMonitoring(any(GetAdminAiMonitoringQuery.class));
    }

    private static AdminAiMonitoringResponse sampleResponse() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");
        return new AdminAiMonitoringResponse(
                new AdminAiMonitoringResponse.Period(
                        LocalDate.parse("2026-06-01"),
                        LocalDate.parse("2026-06-02"),
                        "Asia/Seoul"
                ),
                new AdminAiMonitoringResponse.GenerationJobs(3, 1, 120, 4),
                new AdminAiMonitoringResponse.Validation(
                        8,
                        110,
                        10,
                        2,
                        List.of(new AdminAiMonitoringResponse.FailureReason("SOURCE_MISSING", 4))
                ),
                new AdminAiMonitoringResponse.BatchRuns(
                        5,
                        1,
                        2,
                        List.of(new AdminAiMonitoringResponse.BatchRunFailure(
                                9L,
                                "AI_DAILY_QT_VERSE_EXPLANATION_SEED",
                                "FAILED",
                                "IllegalStateException",
                                "failed",
                                createdAt
                        ))
                ),
                new AdminAiMonitoringResponse.Qa(0, 0, 0, 0, List.of()),
                List.of(new AdminAiMonitoringResponse.Checklist("EXPLANATION", "2026.06.1", 0.91))
        );
    }

    private static Authentication adminPrincipal(Long principal, String adminRole) {
        return principal(principal, "ROLE_ADMIN", adminRole);
    }

    private static Authentication principal(Long principal, String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(authorities).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }
}
