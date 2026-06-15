package com.qtai.domain.study.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.study.api.HidePublishedSimulatorClipUseCase;
import com.qtai.domain.study.api.ListAdminSimulatorClipsUseCase;
import com.qtai.domain.study.api.dto.AdminSimulatorClipListResponse;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipResult;
import com.qtai.security.JwtAuthenticationFilter;

/**
 * {@link AdminSimulatorClipController} 정상 경로/권한 통합 테스트 (AD-14, F-06/F-12).
 * 인증없음/비-ADMIN 차단은 AdminServerSecurityTest(SecurityFilterChain)가 커버.
 */
@WebMvcTest(AdminSimulatorClipController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSimulatorClipControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    ListAdminSimulatorClipsUseCase listAdminSimulatorClipsUseCase;
    @MockBean
    HidePublishedSimulatorClipUseCase hidePublishedSimulatorClipUseCase;
    @MockBean
    VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    @MockBean
    WriteAuditLogUseCase auditLogUseCase;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_reviewer_200() throws Exception {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(listAdminSimulatorClipsUseCase.listAdminSimulatorClips(any()))
                .thenReturn(new AdminSimulatorClipListResponse(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/admin/simulator-clips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void hide_reviewer_200_writes_audit() throws Exception {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(hidePublishedSimulatorClipUseCase.hidePublishedSimulatorClip(any()))
                .thenReturn(new HidePublishedSimulatorClipResult(500L, 1));

        mockMvc.perform(post("/api/v1/admin/simulator-clips/500/hide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hiddenCount").value(1));

        verify(auditLogUseCase).write(any());
    }

    @Test
    void insufficient_admin_role_403_no_audit() throws Exception {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(post("/api/v1/admin/simulator-clips/500/hide"))
                .andExpect(status().isForbidden());

        verify(hidePublishedSimulatorClipUseCase, never()).hidePublishedSimulatorClip(any());
        verify(auditLogUseCase, never()).write(any());
    }

    private void authenticate(String... authorities) {
        var auth = new UsernamePasswordAuthenticationToken(7L, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
