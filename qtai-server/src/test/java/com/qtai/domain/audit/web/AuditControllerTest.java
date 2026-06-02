package com.qtai.domain.audit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.dto.AuditLogFilter;
import com.qtai.domain.audit.api.dto.AuditLogResponse;
import com.qtai.security.JwtAuthenticationFilter;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AuditController MockMvc 슬라이스 테스트 — 목록 조회 + 필터 전달.
 */
@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private ListAuditUseCase listAuditUseCase;

    @Test
    void list_200_필터를_UseCase로_전달하고_매핑한다() throws Exception {
        AuditLogResponse item = new AuditLogResponse(
                100L, null, "ADMIN", 7L, "ADMIN:7",
                "CHECKLIST_CREATE", "AI_VALIDATION_CHECKLIST_VERSION", 4L,
                OffsetDateTime.parse("2026-05-27T10:00:00+09:00"));
        Page<AuditLogResponse> page = new PageImpl<>(List.of(item));
        when(listAuditUseCase.list(any(AuditLogFilter.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit")
                        .param("actorType", "ADMIN")
                        .param("actorId", "7")
                        .param("actionType", "CHECKLIST_CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(100))
                .andExpect(jsonPath("$.data.content[0].actorType").value("ADMIN"))
                .andExpect(jsonPath("$.data.content[0].actionType").value("CHECKLIST_CREATE"))
                .andExpect(jsonPath("$.data.content[0].targetId").value(4));

        ArgumentCaptor<AuditLogFilter> captor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(listAuditUseCase).list(captor.capture(), any(Pageable.class));
        AuditLogFilter filter = captor.getValue();
        assertThat(filter.actorType()).isEqualTo("ADMIN");
        assertThat(filter.actorId()).isEqualTo(7L);
        assertThat(filter.actionType()).isEqualTo("CHECKLIST_CREATE");
    }
}
