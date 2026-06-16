package com.qtai.domain.sharing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.sharing.api.AdminSharingPostUseCase;
import com.qtai.domain.sharing.api.dto.AdminSharingPostResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 관리자 나눔 공유글 API 테스트 (AD-15). 인가(401/403)와 목록/상세/숨김/복원 경로를 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminSharingControllerTest {

    @Mock
    private AdminSharingPostUseCase adminSharingPostUseCase;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc() {
        AdminSharingController controller =
                new AdminSharingController(adminSharingPostUseCase, verifyAdminRoleUseCase);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication adminAuth() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "7", "n/a", AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        auth.setAuthenticated(true);
        return auth;
    }

    private void stubOperator() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR"))))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
    }

    private AdminSharingPostResponse sample() {
        return new AdminSharingPostResponse(
                1L, 7L, "철수", "오늘의 묵상", "감사", "PUBLISHED",
                "미리보기", null, "시편 23:1-6", "2026-06-16",
                true, 3, 2, null, null, LocalDateTime.now());
    }

    @Test
    @DisplayName("OPERATOR는 나눔 글 목록을 조회한다 (status·q 전달)")
    void list_ok() throws Exception {
        stubOperator();
        when(adminSharingPostUseCase.listForAdmin(eq("HIDDEN"), eq("psalm"), any()))
                .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 20), 1));

        mockMvc().perform(get("/api/v1/admin/sharing-posts")
                        .param("status", "HIDDEN")
                        .param("q", "psalm")
                        .principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
        verify(adminSharingPostUseCase).listForAdmin(eq("HIDDEN"), eq("psalm"), any());
    }

    @Test
    @DisplayName("OPERATOR는 나눔 글 상세를 조회한다")
    void detail_ok() throws Exception {
        stubOperator();
        when(adminSharingPostUseCase.getForAdmin(1L)).thenReturn(sample());

        mockMvc().perform(get("/api/v1/admin/sharing-posts/1").principal(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("OPERATOR는 나눔 글을 숨긴다")
    void hide_ok() throws Exception {
        stubOperator();
        when(adminSharingPostUseCase.hide(1L)).thenReturn(sample());

        mockMvc().perform(patch("/api/v1/admin/sharing-posts/1/hide").principal(adminAuth()))
                .andExpect(status().isOk());
        verify(adminSharingPostUseCase).hide(1L);
    }

    @Test
    @DisplayName("OPERATOR는 숨긴 나눔 글을 복원한다")
    void restore_ok() throws Exception {
        stubOperator();
        when(adminSharingPostUseCase.restore(1L)).thenReturn(sample());

        mockMvc().perform(patch("/api/v1/admin/sharing-posts/1/restore").principal(adminAuth()))
                .andExpect(status().isOk());
        verify(adminSharingPostUseCase).restore(1L);
    }

    @Test
    @DisplayName("인증이 없으면 401")
    void noPrincipal_returnsUnauthorized() throws Exception {
        mockMvc().perform(get("/api/v1/admin/sharing-posts")
                        .principal(new AnonymousAuthenticationToken(
                                "key", "anonymous",
                                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 403")
    void notAdmin_returnsForbidden() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "7", "n/a", AuthorityUtils.createAuthorityList("ROLE_USER"));
        auth.setAuthenticated(true);

        mockMvc().perform(get("/api/v1/admin/sharing-posts").principal(auth))
                .andExpect(status().isForbidden());
    }
}
