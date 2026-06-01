package com.qtai.domain.report.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.report.api.ListAdminReportsUseCase;
import com.qtai.domain.report.api.ProcessReportUseCase;
import com.qtai.domain.report.api.dto.AdminReportListResponse;
import com.qtai.domain.report.api.dto.ProcessReportResult;
import com.qtai.security.JwtAuthenticationFilter;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AdminReportController MockMvc 슬라이스 테스트 — 목록/처리 + 권한(OPERATOR).
 */
@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private ListAdminReportsUseCase listAdminReportsUseCase;
    @MockBean
    private ProcessReportUseCase processReportUseCase;

    private void authenticate(String... authorities) {
        var auth = new UsernamePasswordAuthenticationToken(9L, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void setUp() {
        authenticate("ROLE_ADMIN", "ADMIN_ROLE_OPERATOR");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_200_OPERATOR_신고목록() throws Exception {
        AdminReportListResponse resp = new AdminReportListResponse(
                List.of(new AdminReportListResponse.Item(
                        900L, 1L, "POST", 300L, "SPAM", "부적절", "RECEIVED",
                        null, null, LocalDateTime.of(2026, 5, 1, 0, 0))),
                0, 20, 1, 1);
        when(listAdminReportsUseCase.listReports(any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/admin/reports").param("status", "RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(900))
                .andExpect(jsonPath("$.data.content[0].status").value("RECEIVED"));
    }

    @Test
    void resolve_200_처리완료() throws Exception {
        when(processReportUseCase.resolve(any()))
                .thenReturn(new ProcessReportResult(900L, "RESOLVED", 9L, LocalDateTime.of(2026, 5, 29, 12, 0)));

        mockMvc.perform(post("/api/v1/admin/reports/900/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HIDE_TARGET\",\"reason\":\"정책 위반\",\"notifyReporter\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.processedByAdminId").value(9));
    }

    @Test
    void reject_200_반려() throws Exception {
        when(processReportUseCase.reject(any()))
                .thenReturn(new ProcessReportResult(901L, "REJECTED", 9L, LocalDateTime.of(2026, 5, 29, 12, 0)));

        mockMvc.perform(post("/api/v1/admin/reports/901/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"사유 부적합\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void list_403_관리자역할_부족() throws Exception {
        // ROLE_ADMIN은 있으나 OPERATOR/SUPER_ADMIN 세부 권한 없음
        authenticate("ROLE_ADMIN");

        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    @Test
    void list_403_관리자_아님() throws Exception {
        authenticate("ROLE_USER");

        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }
}
