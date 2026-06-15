package com.qtai.domain.member.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.member.api.GetMemberDetailForAdminUseCase;
import com.qtai.domain.member.api.ListMembersForAdminUseCase;
import com.qtai.domain.member.api.ListNicknameHistoryForAdminUseCase;
import com.qtai.domain.member.api.UpdateMemberStatusForAdminUseCase;
import com.qtai.domain.member.api.dto.AdminMemberDetailResponse;
import com.qtai.domain.member.api.dto.AdminMemberResponse;
import com.qtai.domain.mission.api.GetMemberMissionProgressUseCase;
import com.qtai.domain.note.api.ListMemberNotesForAdminUseCase;
import com.qtai.domain.sharing.api.AdminMemberSharingQueryUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMemberControllerTest {

    @Mock
    private ListMembersForAdminUseCase listMembersForAdminUseCase;

    @Mock
    private UpdateMemberStatusForAdminUseCase updateMemberStatusForAdminUseCase;

    @Mock
    private GetMemberDetailForAdminUseCase getMemberDetailForAdminUseCase;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @Mock
    private ListMemberNotesForAdminUseCase listMemberNotesForAdminUseCase;

    @Mock
    private AdminMemberSharingQueryUseCase adminMemberSharingQueryUseCase;

    @Mock
    private GetMemberMissionProgressUseCase getMemberMissionProgressUseCase;

    @Mock
    private ListNicknameHistoryForAdminUseCase listNicknameHistoryForAdminUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminMemberController controller = new AdminMemberController(
                listMembersForAdminUseCase,
                updateMemberStatusForAdminUseCase,
                getMemberDetailForAdminUseCase,
                verifyAdminRoleUseCase,
                listMemberNotesForAdminUseCase,
                adminMemberSharingQueryUseCase,
                getMemberMissionProgressUseCase,
                listNicknameHistoryForAdminUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OPERATOR는 회원 목록을 조회할 수 있다")
    void list_returnsOk() throws Exception {
        operator();
        when(listMembersForAdminUseCase.listForAdmin(any(), any(), any()))
                .thenReturn(page(response(11L)));

        mockMvc.perform(get("/api/v1/admin/members")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(11));
    }

    @Test
    @DisplayName("OPERATOR는 회원 상세(신고/나눔 집계)를 조회할 수 있다")
    void detail_returnsStats() throws Exception {
        operator();
        when(getMemberDetailForAdminUseCase.getDetailForAdmin(11L))
                .thenReturn(new AdminMemberDetailResponse(
                        11L, "닉네임", "ACTIVE", "USER", null, null,
                        LocalDateTime.of(2026, 6, 14, 10, 0), 3L, 2L, 5L));

        mockMvc.perform(get("/api/v1/admin/members/11/detail")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportsReceivedCount").value(5))
                .andExpect(jsonPath("$.data.reportsFiledCount").value(2))
                .andExpect(jsonPath("$.data.sharingPostCount").value(3));
    }

    @Test
    @DisplayName("OPERATOR는 회원을 정지할 수 있다 (200)")
    void updateStatus_returnsOk() throws Exception {
        operator();
        when(updateMemberStatusForAdminUseCase.updateStatus(eq(11L), any()))
                .thenReturn(response(11L));

        mockMvc.perform(patch("/api/v1/admin/members/11/status")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11));
    }

    @Test
    @DisplayName("허용되지 않은 status는 400을 반환한다")
    void updateStatus_invalid_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/members/11/status")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WITHDRAWN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증이 없으면 401 M0002")
    void noPrincipal_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members")
                        .principal(new AnonymousAuthenticationToken(
                                "key", "anonymous",
                                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 403 M0003")
    void nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members")
                        .principal(authentication("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    private void operator() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR"))))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static Page<AdminMemberResponse> page(AdminMemberResponse response) {
        return new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
    }

    private static AdminMemberResponse response(Long id) {
        return new AdminMemberResponse(
                id, "닉네임", "ACTIVE", "USER", null, null,
                LocalDateTime.of(2026, 6, 14, 10, 0));
    }
}
