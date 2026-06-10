package com.qtai.domain.notification.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.notification.api.CreateAdminNoticeUseCase;
import com.qtai.domain.notification.api.HideAdminNoticeUseCase;
import com.qtai.domain.notification.api.ListAdminNoticesUseCase;
import com.qtai.domain.notification.api.PublishAdminNoticeUseCase;
import com.qtai.domain.notification.api.UpdateAdminNoticeUseCase;
import com.qtai.domain.notification.api.dto.AdminNoticeDetailResponse;
import com.qtai.domain.notification.api.dto.AdminNoticeListResponse;
import com.qtai.domain.notification.api.dto.AdminNoticePublishResponse;
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

@WebMvcTest(AdminNoticeController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminNoticeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    ListAdminNoticesUseCase listAdminNoticesUseCase;
    @MockBean
    CreateAdminNoticeUseCase createAdminNoticeUseCase;
    @MockBean
    UpdateAdminNoticeUseCase updateAdminNoticeUseCase;
    @MockBean
    PublishAdminNoticeUseCase publishAdminNoticeUseCase;
    @MockBean
    HideAdminNoticeUseCase hideAdminNoticeUseCase;
    @MockBean
    VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @BeforeEach
    void setUp() {
        authenticate("ROLE_ADMIN");
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(100L, 7L, "OPERATOR"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_operator_200() throws Exception {
        when(listAdminNoticesUseCase.listAdminNotices(0, 20)).thenReturn(listResponse());

        mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    @Test
    void list_superAdmin_200() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(100L, 7L, "SUPER_ADMIN"));
        when(listAdminNoticesUseCase.listAdminNotices(0, 20)).thenReturn(listResponse());

        mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isOk());
    }

    @Test
    void list_reviewer_403() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    @Test
    void list_contentCreator_403() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_user_403() throws Exception {
        authenticate("ROLE_USER");

        mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    @Test
    void list_unauthenticated_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_201() throws Exception {
        when(createAdminNoticeUseCase.createNotice(any())).thenReturn(detailResponse("DRAFT"));

        mockMvc.perform(post("/api/v1/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"body\":\"본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void update_200() throws Exception {
        when(updateAdminNoticeUseCase.updateNotice(eq(1L), any())).thenReturn(detailResponse("DRAFT"));

        mockMvc.perform(patch("/api/v1/admin/notices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"body\":\"본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void publish_200() throws Exception {
        when(publishAdminNoticeUseCase.publishNotice(100L, 1L)).thenReturn(new AdminNoticePublishResponse(
                1L,
                "PUBLISHED",
                LocalDateTime.of(2026, 6, 10, 10, 30),
                new AdminNoticePublishResponse.NotificationResult(2, 2, 0)
        ));

        mockMvc.perform(post("/api/v1/admin/notices/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.notificationResult.createdCount").value(2));
    }

    @Test
    void hide_204() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notices/1/hide"))
                .andExpect(status().isNoContent());
    }

    private void authenticate(String... authorities) {
        var auth = new UsernamePasswordAuthenticationToken(7L, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static AdminNoticeListResponse listResponse() {
        return new AdminNoticeListResponse(
                List.of(new AdminNoticeListResponse.Item(
                        1L,
                        "공지",
                        "본문",
                        "DRAFT",
                        null,
                        LocalDateTime.of(2026, 6, 10, 10, 0),
                        LocalDateTime.of(2026, 6, 10, 10, 0)
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );
    }

    private static AdminNoticeDetailResponse detailResponse(String status) {
        return new AdminNoticeDetailResponse(
                1L,
                "공지",
                "본문",
                status,
                null,
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 10, 0)
        );
    }
}
