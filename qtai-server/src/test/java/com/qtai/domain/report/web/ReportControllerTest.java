package com.qtai.domain.report.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.CreateReportUseCase;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import com.qtai.security.JwtAuthenticationFilter;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ReportController MockMvc 슬라이스 테스트.
 *
 * <p>POST /api/v1/reports — 접수 성공(201), 중복(409), 입력 검증(400).
 */
@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CreateReportUseCase createReportUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReport_201_신고_접수() throws Exception {
        ReportCreateRequest request =
                new ReportCreateRequest("POST", 300L, "INAPPROPRIATE", "부적절한 표현");
        ReportResponse response =
                new ReportResponse(900L, "RECEIVED", LocalDateTime.of(2026, 5, 29, 12, 0));
        when(createReportUseCase.createReport(eq(1L), any(ReportCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(900))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void createReport_409_중복_신고() throws Exception {
        ReportCreateRequest request =
                new ReportCreateRequest("POST", 300L, "SPAM", null);
        when(createReportUseCase.createReport(eq(1L), any(ReportCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_REPORT));

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("R0001"));
    }

    @Test
    void createReport_400_targetType_누락() throws Exception {
        // targetType 누락 → @NotBlank 위반 → 400
        String body = "{\"targetId\": 300, \"reason\": \"SPAM\"}";

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }
}
