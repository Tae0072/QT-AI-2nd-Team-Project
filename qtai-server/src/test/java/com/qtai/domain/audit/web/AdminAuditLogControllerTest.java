package com.qtai.domain.audit.web;

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

import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.dto.AuditLogItem;
import com.qtai.domain.audit.api.dto.AuditLogListResponse;
import com.qtai.domain.audit.api.dto.ListAuditQuery;

class AdminAuditLogControllerTest {

    private ListAuditUseCase listAuditUseCase;
    private com.qtai.support.StubVerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        listAuditUseCase = org.mockito.Mockito.mock(ListAuditUseCase.class);
        verifyAdminRoleUseCase = new com.qtai.support.StubVerifyAdminRoleUseCase();
        AdminAuditLogController controller = new AdminAuditLogController(
                listAuditUseCase,
                new AdminAuditAuthentication(verifyAdminRoleUseCase)
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
    void listAuditLogsMapsFiltersAndReturnsPageEnvelope() throws Exception {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-02T10:30:00+09:00");
        when(listAuditUseCase.listAuditLogs(any(ListAuditQuery.class)))
                .thenReturn(new AuditLogListResponse(
                        List.of(new AuditLogItem(
                                11L,
                                null,
                                "ADMIN",
                                7L,
                                "ADMIN:7",
                                "AI_ASSET_HIDE",
                                "AI_GENERATED_ASSET",
                                500L,
                                "{\"status\":\"APPROVED\"}",
                                "{\"status\":\"HIDDEN\"}",
                                createdAt
                        )),
                        1,
                        5,
                        12L,
                        3,
                        false,
                        false,
                        "createdAt,desc,id,desc"
                ));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR"))
                        .param("actorType", "ADMIN")
                        .param("actorId", "7")
                        .param("actionType", "AI_ASSET_HIDE")
                        .param("targetType", "AI_GENERATED_ASSET")
                        .param("targetId", "500")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-02")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(11))
                .andExpect(jsonPath("$.data.content[0].actionType").value("AI_ASSET_HIDE"))
                .andExpect(jsonPath("$.data.content[0].targetType").value("AI_GENERATED_ASSET"))
                .andExpect(jsonPath("$.data.content[0].beforeJson").value("{\"status\":\"APPROVED\"}"))
                .andExpect(jsonPath("$.data.content[0].afterJson").value("{\"status\":\"HIDDEN\"}"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.sort").value("createdAt,desc,id,desc"));

        ArgumentCaptor<ListAuditQuery> queryCaptor = ArgumentCaptor.forClass(ListAuditQuery.class);
        verify(listAuditUseCase).listAuditLogs(queryCaptor.capture());
        ListAuditQuery query = queryCaptor.getValue();
        // DB 검증 통일 후 adminId = admin_users.id (스텁 규약: memberId + 100)
        assertThat(query.adminId()).isEqualTo(7L + com.qtai.support.StubVerifyAdminRoleUseCase.ADMIN_USER_ID_OFFSET);
        assertThat(query.memberRole()).isEqualTo("ADMIN");
        assertThat(query.adminRole()).isEqualTo("OPERATOR");
        assertThat(query.actorType()).isEqualTo("ADMIN");
        assertThat(query.actorId()).isEqualTo(7L);
        assertThat(query.actionType()).isEqualTo("AI_ASSET_HIDE");
        assertThat(query.targetType()).isEqualTo("AI_GENERATED_ASSET");
        assertThat(query.targetId()).isEqualTo(500L);
        assertThat(query.from()).isEqualTo("2026-06-01");
        assertThat(query.to()).isEqualTo("2026-06-02");
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(5);
    }

    @Test
    void listAuditLogsEnforcesAdminAuthority() throws Exception {
        when(listAuditUseCase.listAuditLogs(any(ListAuditQuery.class))).thenReturn(emptyResponse());

        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .principal(principal(7L, "ROLE_USER", "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_CONTENT_CREATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .principal(adminPrincipal(7L, "ADMIN_ROLE_OPERATOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .principal(adminPrincipal(8L, "ADMIN_ROLE_REVIEWER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .principal(adminPrincipal(9L, "ADMIN_ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk());

        verify(listAuditUseCase, times(3)).listAuditLogs(any(ListAuditQuery.class));
    }

    private static AuditLogListResponse emptyResponse() {
        return new AuditLogListResponse(List.of(), 0, 20, 0L, 0, true, true,
                "createdAt,desc,id,desc");
    }

    /** 관리자 토큰 생성 + 스텁 admin_users 등록 (ADMIN_ROLE_* 접두 제거 후 역할 등록). */
    private Authentication adminPrincipal(Long memberId, String adminRole) {
        if (adminRole.startsWith("ADMIN_ROLE_")) {
            verifyAdminRoleUseCase.register(memberId, adminRole.substring("ADMIN_ROLE_".length()));
        }
        return principal(memberId, "ROLE_ADMIN", adminRole);
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
