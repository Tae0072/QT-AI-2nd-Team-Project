package com.qtai.domain.ai.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.qtai.domain.ai.api.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.dto.AdminAiBatchRunLogItem;
import com.qtai.domain.ai.api.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.dto.ListAdminAiBatchRunLogsQuery;

class AdminAiBatchRunLogControllerTest {

    private ListAdminAiBatchRunLogsUseCase listUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        listUseCase = org.mockito.Mockito.mock(ListAdminAiBatchRunLogsUseCase.class);
        AdminAiBatchRunLogController controller = new AdminAiBatchRunLogController(listUseCase);
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
    void listBatchRunLogsMapsFiltersAndReturnsPageEnvelope() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");
        when(listUseCase.listAdminAiBatchRunLogs(any(ListAdminAiBatchRunLogsQuery.class)))
                .thenReturn(new AdminAiBatchRunLogListResponse(
                        List.of(new AdminAiBatchRunLogItem(
                                3L,
                                "AI_DAILY_QT_VERSE_EXPLANATION_SEED",
                                "PARTIAL_FAILED",
                                2,
                                1,
                                0,
                                "IllegalStateException",
                                "REDACTED_SENSITIVE_ERROR_MESSAGE",
                                now.minusMinutes(1),
                                now,
                                now.plusSeconds(1)
                        )),
                        1,
                        5,
                        12L,
                        3,
                        false,
                        false,
                        "createdAt,desc,id,desc"
                ));

        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR"))
                        .param("batchName", "AI_DAILY_QT_VERSE_EXPLANATION_SEED")
                        .param("status", "PARTIAL_FAILED")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-02")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(3))
                .andExpect(jsonPath("$.data.content[0].batchName").value("AI_DAILY_QT_VERSE_EXPLANATION_SEED"))
                .andExpect(jsonPath("$.data.content[0].status").value("PARTIAL_FAILED"))
                .andExpect(jsonPath("$.data.content[0].errorMessage").value("REDACTED_SENSITIVE_ERROR_MESSAGE"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.sort").value("createdAt,desc,id,desc"));

        ArgumentCaptor<ListAdminAiBatchRunLogsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListAdminAiBatchRunLogsQuery.class);
        verify(listUseCase).listAdminAiBatchRunLogs(queryCaptor.capture());
        ListAdminAiBatchRunLogsQuery query = queryCaptor.getValue();
        assertThat(query.adminId()).isEqualTo(7L);
        assertThat(query.memberRole()).isEqualTo("ADMIN");
        assertThat(query.adminRole()).isEqualTo("OPERATOR");
        assertThat(query.batchName()).isEqualTo("AI_DAILY_QT_VERSE_EXPLANATION_SEED");
        assertThat(query.status()).isEqualTo("PARTIAL_FAILED");
        assertThat(query.from()).isEqualTo("2026-06-01");
        assertThat(query.to()).isEqualTo("2026-06-02");
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(5);
    }

    @Test
    void listBatchRunLogsEnforcesMonitoringAdminAuthority() throws Exception {
        when(listUseCase.listAdminAiBatchRunLogs(any(ListAdminAiBatchRunLogsQuery.class)))
                .thenReturn(emptyResponse());

        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_CONTENT_CREATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(adminPrincipal(8L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/ai/batch-run-logs")
                        .principal(adminPrincipal(9L, "ADMIN_ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk());

        verify(listUseCase, times(3)).listAdminAiBatchRunLogs(any(ListAdminAiBatchRunLogsQuery.class));
    }

    private static AdminAiBatchRunLogListResponse emptyResponse() {
        return new AdminAiBatchRunLogListResponse(List.of(), 0, 20, 0L, 0, true, true,
                "createdAt,desc,id,desc");
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
