package com.qtai.domain.ai.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.ai.api.admin.asset.GenerateQtPassageExplanationUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationResult;
import com.qtai.security.JwtAuthenticationFilter;

/**
 * {@link AdminAiExplanationController} 정상 경로/권한 통합 테스트 (F-02/F-06).
 *
 * <p>인증 없음/비-ADMIN 차단은 {@code AdminServerSecurityTest}(SecurityFilterChain)가 커버하고,
 * 여기서는 컨트롤러+인가 헬퍼({@link AdminAiAuthentication}) 결합의 정상 경로(202·REVIEWER 허용)와
 * admin_role 2차 검증 거부(403)를 검증한다.
 */
@WebMvcTest(AdminAiExplanationController.class)
@Import(AdminAiAuthentication.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAiExplanationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    GenerateQtPassageExplanationUseCase generateQtPassageExplanationUseCase;
    @MockBean
    VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reviewer_generate_202() throws Exception {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(100L, 7L, "REVIEWER"));
        when(generateQtPassageExplanationUseCase.generateQtPassageExplanation(any()))
                .thenReturn(new GenerateQtPassageExplanationResult(3, 0, null));

        mockMvc.perform(post("/api/v1/admin/ai/qt-passages/35/explanations/generate"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.createdCount").value(3))
                .andExpect(jsonPath("$.data.failedCount").value(0));
    }

    @Test
    void superAdmin_generate_202() throws Exception {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(100L, 7L, "SUPER_ADMIN"));
        when(generateQtPassageExplanationUseCase.generateQtPassageExplanation(any()))
                .thenReturn(new GenerateQtPassageExplanationResult(1, 0, null));

        mockMvc.perform(post("/api/v1/admin/ai/qt-passages/35/explanations/generate"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.createdCount").value(1));
    }

    @Test
    void insufficient_admin_role_403() throws Exception {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(post("/api/v1/admin/ai/qt-passages/35/explanations/generate"))
                .andExpect(status().isForbidden());
    }

    private void authenticate(String... authorities) {
        var auth = new UsernamePasswordAuthenticationToken(7L, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
