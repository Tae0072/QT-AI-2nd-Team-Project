package com.qtai.domain.qt.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.qt.api.admin.CreateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.HideAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.ListAdminQtPassagesUseCase;
import com.qtai.domain.qt.api.admin.PublishAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.UpdateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageListResponse;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminQtPassageControllerTest {

    @Mock
    private ListAdminQtPassagesUseCase listUseCase;

    @Mock
    private CreateAdminQtPassageUseCase createUseCase;

    @Mock
    private UpdateAdminQtPassageUseCase updateUseCase;

    @Mock
    private PublishAdminQtPassageUseCase publishUseCase;

    @Mock
    private HideAdminQtPassageUseCase hideUseCase;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AdminQtPassageController controller = new AdminQtPassageController(
                listUseCase,
                createUseCase,
                updateUseCase,
                publishUseCase,
                hideUseCase,
                verifyAdminRoleUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    @DisplayName("관리자는 QT 본문 목록을 조회할 수 있다")
    void list_returnsQtPassageList() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
        when(listUseCase.list(any())).thenReturn(new AdminQtPassageListResponse(
                List.of(response(10L, "pending_review")), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/admin/qt-passages")
                        .principal(authentication("ROLE_ADMIN"))
                        .param("status", "pending_review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].status").value("pending_review"));
    }

    @Test
    @DisplayName("SUPER_ADMIN can access qt-passages admin API")
    void superAdmin_returnsQtPassageList() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "SUPER_ADMIN"));
        when(listUseCase.list(any())).thenReturn(new AdminQtPassageListResponse(
                List.of(response(10L, "pending_review")), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/admin/qt-passages")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("pending_review"));
    }

    @Test
    @DisplayName("관리자는 QT 본문을 등록할 수 있다")
    void create_returnsCreated() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
        when(createUseCase.create(any())).thenReturn(response(10L, "pending_review"));

        mockMvc.perform(post("/api/v1/admin/qt-passages")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    @Test
    @DisplayName("duplicate qtDate returns 409 C0003")
    void duplicateQtDate_returnsConflict() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
        when(createUseCase.create(any())).thenThrow(new BusinessException(ErrorCode.DUPLICATE_RESOURCE));

        mockMvc.perform(post("/api/v1/admin/qt-passages")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("C0003"));
    }

    @Test
    @DisplayName("AdminQtPassageController requires ROLE_ADMIN via @PreAuthorize")
    void classLevelPreAuthorizeRequiresRoleAdmin() {
        PreAuthorize preAuthorize = AdminQtPassageController.class.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    @DisplayName("admin_users 2차 권한이 부족하면 AD0003으로 거부한다")
    void insufficientAdminRole_returnsForbidden() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(get("/api/v1/admin/qt-passages")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    @Test
    @DisplayName("요청 본문 검증 실패는 400으로 응답한다")
    void invalidRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/qt-passages")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qtDate":"2026-06-10","bookId":19,"chapter":23,"startVerse":6,"endVerse":1,"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    @DisplayName("수정, 게시, 숨김 mapping을 제공한다")
    void updatePublishHide_mappingsReturnOk() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
        when(updateUseCase.update(eq(10L), any())).thenReturn(response(10L, "pending_review"));
        when(publishUseCase.publish(3L, 10L)).thenReturn(response(10L, "active"));
        when(hideUseCase.hide(3L, 10L)).thenReturn(response(10L, "hidden"));

        mockMvc.perform(patch("/api/v1/admin/qt-passages/10")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        mockMvc.perform(post("/api/v1/admin/qt-passages/10/publish")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));

        mockMvc.perform(post("/api/v1/admin/qt-passages/10/hide")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hidden"));
    }

    @Test
    @DisplayName("missing qt passage returns Q0001")
    void missingQtPassage_returnsNotFound() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
        when(publishUseCase.publish(3L, 404L)).thenThrow(new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));

        mockMvc.perform(post("/api/v1/admin/qt-passages/404/publish")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("Q0001"));
    }

    @Test
    @DisplayName("invalid status transition returns 409 C0007")
    void invalidStatusTransition_returnsConflict() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(requiredRoles())))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
        when(hideUseCase.hide(3L, 10L)).thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/admin/qt-passages/10/hide")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("C0007"));
    }

    private static List<String> requiredRoles() {
        return List.of("OPERATOR", "SUPER_ADMIN");
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static AdminQtPassageResponse response(Long id, String status) {
        return new AdminQtPassageResponse(
                id,
                LocalDate.of(2026, 6, 10),
                (short) 19,
                (short) 23,
                (short) 1,
                (short) 6,
                "관리자 QT",
                "시 23:1-6",
                status,
                null,
                null,
                null,
                null
        );
    }

    private static AdminQtPassageController.AdminQtPassageRequest request() {
        return new AdminQtPassageController.AdminQtPassageRequest(
                LocalDate.of(2026, 6, 10),
                (short) 19,
                (short) 23,
                (short) 1,
                (short) 6,
                "관리자 QT",
                "시 23:1-6"
        );
    }
}
