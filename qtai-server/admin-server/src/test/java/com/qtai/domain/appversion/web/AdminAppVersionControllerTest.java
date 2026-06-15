package com.qtai.domain.appversion.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.appversion.api.AdminAppVersionUseCase;
import com.qtai.domain.appversion.api.dto.AppVersionStateResponse;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 관리자 앱 버전/업데이트 API 테스트 (AD-19, 2026-06-14 Lead 승인).
 */
@ExtendWith(MockitoExtension.class)
class AdminAppVersionControllerTest {

    @Mock
    private AdminAppVersionUseCase adminAppVersionUseCase;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc() {
        AdminAppVersionController controller =
                new AdminAppVersionController(adminAppVersionUseCase, verifyAdminRoleUseCase);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OPERATOR는 버전 상태를 조회할 수 있다")
    void state_returnsOk() throws Exception {
        operator();
        when(adminAppVersionUseCase.getState()).thenReturn(state("0.1.0", "0.1.0"));

        mockMvc().perform(get("/api/v1/admin/app-updates/state")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentVersion").value("0.1.0"));
    }

    @Test
    @DisplayName("콘텐츠 적용은 200으로 새 버전을 반환한다")
    void applyContent_returnsOk() throws Exception {
        operator();
        when(adminAppVersionUseCase.applyContent()).thenReturn(state("0.1.0.1", "0.1.0"));

        mockMvc().perform(post("/api/v1/admin/app-updates/apply-content")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentVersion").value("0.1.0.1"));
    }

    @Test
    @DisplayName("업데이트 예정 등록은 201")
    void createPending_returnsCreated() throws Exception {
        operator();
        when(adminAppVersionUseCase.createPending(any())).thenReturn(pending(7L));

        mockMvc().perform(post("/api/v1/admin/app-updates/pending")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"음원 대량 추가","targetAppVersion":"0.2.0","updateMode":"FORCED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    @DisplayName("제목이 비면 400")
    void createPending_blankTitle_returnsBadRequest() throws Exception {
        mockMvc().perform(post("/api/v1/admin/app-updates/pending")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","targetAppVersion":"0.2.0"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("업데이트 예정 적용은 200으로 새 앱 버전을 반환한다")
    void applyPending_returnsOk() throws Exception {
        operator();
        when(adminAppVersionUseCase.applyPending(7L)).thenReturn(state("0.2.0", "0.2.0"));

        mockMvc().perform(post("/api/v1/admin/app-updates/pending/7/apply")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appVersion").value("0.2.0"));
    }

    @Test
    @DisplayName("업데이트 예정 삭제는 204")
    void deletePending_returnsNoContent() throws Exception {
        operator();

        mockMvc().perform(delete("/api/v1/admin/app-updates/pending/7")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        verify(adminAppVersionUseCase).deletePending(7L);
    }

    @Test
    @DisplayName("인증이 없으면 401 M0002")
    void noPrincipal_returnsUnauthorized() throws Exception {
        mockMvc().perform(get("/api/v1/admin/app-updates/state")
                        .principal(new AnonymousAuthenticationToken(
                                "key", "anonymous",
                                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 403 M0003")
    void nonAdmin_returnsForbidden() throws Exception {
        mockMvc().perform(get("/api/v1/admin/app-updates/state")
                        .principal(authentication("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    // ── helpers ──

    private void operator() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR"))))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static AppVersionStateResponse state(String content, String app) {
        return new AppVersionStateResponse(
                content, app, app, "NONE", null, LocalDateTime.of(2026, 6, 14, 10, 0));
    }

    private static PendingAppUpdateResponse pending(Long id) {
        return new PendingAppUpdateResponse(
                id, "음원 대량 추가", "앱 번들 갱신", "0.2.0", "FORCED", "PENDING",
                LocalDateTime.of(2026, 6, 14, 10, 0), null);
    }
}
