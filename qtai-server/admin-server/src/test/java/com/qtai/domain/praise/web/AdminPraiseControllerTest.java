package com.qtai.domain.praise.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.qtai.domain.praise.api.CreatePraiseUseCase;
import com.qtai.domain.praise.api.DeletePraiseUseCase;
import com.qtai.domain.praise.api.ListPraiseUseCase;
import com.qtai.domain.praise.api.UpdatePraiseUseCase;
import com.qtai.domain.praise.api.dto.PraiseResponse;
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
class AdminPraiseControllerTest {

    @Mock
    private ListPraiseUseCase listPraiseUseCase;

    @Mock
    private CreatePraiseUseCase createPraiseUseCase;

    @Mock
    private UpdatePraiseUseCase updatePraiseUseCase;

    @Mock
    private DeletePraiseUseCase deletePraiseUseCase;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AdminPraiseController controller = new AdminPraiseController(
                listPraiseUseCase,
                createPraiseUseCase,
                updatePraiseUseCase,
                deletePraiseUseCase,
                verifyAdminRoleUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    @DisplayName("OPERATOR는 찬양 큐레이션 목록을 조회할 수 있다")
    void list_returnsOk() throws Exception {
        operator();
        when(listPraiseUseCase.listAdmin(eq("ACTIVE"), any()))
                .thenReturn(page(response(50L)));

        mockMvc.perform(get("/api/v1/admin/praise-songs")
                        .principal(authentication("ROLE_ADMIN"))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(50));
    }

    @Test
    @DisplayName("OPERATOR는 찬양 곡을 등록할 수 있다 (201)")
    void create_returnsCreated() throws Exception {
        operator();
        when(createPraiseUseCase.create(eq(3L), any())).thenReturn(response(50L));

        mockMvc.perform(post("/api/v1/admin/praise-songs")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"은혜","artist":"큐레이션","licenseNote":"확인함"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(50));
    }

    @Test
    @DisplayName("OPERATOR는 찬양 곡을 수정할 수 있다 (200)")
    void update_returnsOk() throws Exception {
        operator();
        when(updatePraiseUseCase.update(eq(3L), eq(50L), any())).thenReturn(response(50L));

        mockMvc.perform(patch("/api/v1/admin/praise-songs/50")
                        .principal(authentication("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"새 제목","artist":"새 아티스트","licenseNote":"확인함"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(50));
    }

    @Test
    @DisplayName("OPERATOR는 찬양 곡을 삭제할 수 있다 (204)")
    void delete_returnsNoContent() throws Exception {
        operator();

        mockMvc.perform(delete("/api/v1/admin/praise-songs/50")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        verify(deletePraiseUseCase).delete(3L, 50L);
    }

    @Test
    @DisplayName("없는 곡 삭제는 404 P0001")
    void delete_notFound() throws Exception {
        operator();
        doThrow(new BusinessException(ErrorCode.PRAISE_SONG_NOT_FOUND))
                .when(deletePraiseUseCase).delete(3L, 404L);

        mockMvc.perform(delete("/api/v1/admin/praise-songs/404")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("P0001"));
    }

    @Test
    @DisplayName("정의되지 않은 status 목록 조회는 400 C0002")
    void list_invalidStatus_returnsBadRequest() throws Exception {
        operator();
        when(listPraiseUseCase.listAdmin(eq("UNKNOWN"), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT));

        mockMvc.perform(get("/api/v1/admin/praise-songs")
                        .principal(authentication("ROLE_ADMIN"))
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    @DisplayName("인증이 없으면 401 M0002")
    void noPrincipal_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/praise-songs")
                        .principal(new AnonymousAuthenticationToken(
                                "key", "anonymous",
                                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 403 M0003")
    void nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/praise-songs")
                        .principal(authentication("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    @Test
    @DisplayName("admin_users 2차 권한이 부족하면 AD0003")
    void insufficientAdminRole_returnsForbidden() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR"))))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(get("/api/v1/admin/praise-songs")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    // ── helpers ──

    private void operator() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR"))))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static Page<PraiseResponse> page(PraiseResponse response) {
        PageRequest pageable = PageRequest.of(0, 20);
        return new PageImpl<>(List.of(response), pageable, 1);
    }

    private static PraiseResponse response(Long id) {
        return new PraiseResponse(
                id,
                "은혜",
                "큐레이션",
                "CURATED",
                "운영자가 저작권 상태를 확인함",
                "ACTIVE",
                LocalDateTime.of(2026, 6, 11, 10, 0),
                null
        );
    }
}
