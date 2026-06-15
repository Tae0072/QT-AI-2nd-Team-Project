package com.qtai.domain.music.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.music.api.CreateAdminMusicTrackUseCase;
import com.qtai.domain.music.api.HideAdminMusicTrackUseCase;
import com.qtai.domain.music.api.ListAdminMusicTrackUseCase;
import com.qtai.domain.music.api.PublishAdminMusicTrackUseCase;
import com.qtai.domain.music.api.UpdateAdminMusicTrackUseCase;
import com.qtai.domain.music.api.dto.AdminMusicTrackListResponse;
import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMusicTrackControllerTest {

    private static final List<String> MUSIC_TRACK_ADMIN_ROLES = List.of("OPERATOR", "SUPER_ADMIN");

    @Mock
    private ListAdminMusicTrackUseCase listAdminMusicTrackUseCase;

    @Mock
    private CreateAdminMusicTrackUseCase createAdminMusicTrackUseCase;

    @Mock
    private UpdateAdminMusicTrackUseCase updateAdminMusicTrackUseCase;

    @Mock
    private PublishAdminMusicTrackUseCase publishAdminMusicTrackUseCase;

    @Mock
    private HideAdminMusicTrackUseCase hideAdminMusicTrackUseCase;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminMusicTrackController controller = new AdminMusicTrackController(
                listAdminMusicTrackUseCase,
                createAdminMusicTrackUseCase,
                updateAdminMusicTrackUseCase,
                publishAdminMusicTrackUseCase,
                hideAdminMusicTrackUseCase,
                verifyAdminRoleUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OPERATOR는 배경음악 목록을 조회할 수 있다")
    void list_returnsOk() throws Exception {
        operator();
        when(listAdminMusicTrackUseCase.listAdmin(eq("ACTIVE"), any()))
                .thenReturn(page(response(10L, "ACTIVE")));

        mockMvc.perform(get("/api/v1/admin/music-tracks")
                        .principal(authentication("ROLE_ADMIN"))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.sort").value("createdAt,desc"));
    }

    @Test
    @DisplayName("OPERATOR는 배경음악을 등록할 수 있다")
    void create_returnsCreated() throws Exception {
        operator();
        when(createAdminMusicTrackUseCase.createAdmin(eq(3L), any()))
                .thenReturn(response(10L, "HIDDEN"));

        mockMvc.perform(multipart("/api/v1/admin/music-tracks")
                        .file(audioFile())
                        .param("title", "새 배경음악")
                        .param("category", "BGM")
                        .param("mimeType", "audio/mpeg")
                        .param("durationSec", "180")
                        .param("sortOrder", "5")
                        .param("licenseNote", "운영자가 라이선스를 확인함")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }

    @Test
    @DisplayName("OPERATOR는 배경음악 메타데이터와 파일을 수정할 수 있다")
    void update_returnsOk() throws Exception {
        operator();
        when(updateAdminMusicTrackUseCase.updateAdmin(eq(3L), eq(10L), any()))
                .thenReturn(response(10L, "ACTIVE"));

        mockMvc.perform(multipart("/api/v1/admin/music-tracks/10")
                        .file(audioFile())
                        .param("title", "수정 배경음악")
                        .param("category", "BGM")
                        .param("mimeType", "audio/mpeg")
                        .param("durationSec", "200")
                        .param("sortOrder", "7")
                        .param("licenseNote", "수정된 라이선스 메모")
                        .principal(authentication("ROLE_ADMIN"))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("OPERATOR는 배경음악을 발행할 수 있다")
    void publish_returnsOk() throws Exception {
        operator();
        when(publishAdminMusicTrackUseCase.publishAdmin(3L, 10L))
                .thenReturn(response(10L, "ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/music-tracks/10/publish")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("OPERATOR는 배경음악을 숨김 처리할 수 있다")
    void hide_returnsOk() throws Exception {
        operator();
        when(hideAdminMusicTrackUseCase.hideAdmin(3L, 10L))
                .thenReturn(response(10L, "HIDDEN"));

        mockMvc.perform(post("/api/v1/admin/music-tracks/10/hide")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }

    @Test
    @DisplayName("없는 음원은 404 C0004")
    void publish_notFound() throws Exception {
        operator();
        when(publishAdminMusicTrackUseCase.publishAdmin(3L, 404L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(post("/api/v1/admin/music-tracks/404/publish")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C0004"));
    }

    @Test
    @DisplayName("잘못된 상태 전이는 409 C0007")
    void publish_invalidTransition() throws Exception {
        operator();
        when(publishAdminMusicTrackUseCase.publishAdmin(3L, 10L))
                .thenThrow(new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/v1/admin/music-tracks/10/publish")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("C0007"));
    }

    @Test
    @DisplayName("등록 요청에 파일이 없으면 400")
    void create_withoutFile_returnsBadRequest() throws Exception {
        operator();

        mockMvc.perform(multipart("/api/v1/admin/music-tracks")
                        .param("title", "새 배경음악")
                        .param("category", "BGM")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("?깅줉 ?붿껌???뚯씪???덈Т ?щ㈃ 400")
    void create_withOversizedFile_returnsBadRequest() throws Exception {
        operator();

        mockMvc.perform(multipart("/api/v1/admin/music-tracks")
                        .file(oversizedAudioFile())
                        .param("title", "??諛곌꼍?뚯븙")
                        .param("category", "BGM")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    @DisplayName("인증이 없으면 401 M0002")
    void noPrincipal_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/music-tracks")
                        .principal(new AnonymousAuthenticationToken(
                                "key", "anonymous",
                                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 403 M0003")
    void nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/music-tracks")
                        .principal(authentication("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("M0003"));
    }

    @Test
    @DisplayName("admin_users 2차 권한이 부족하면 AD0003")
    void insufficientAdminRole_returnsForbidden() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT))
                .when(verifyAdminRoleUseCase).verifyAnyRole(eq(7L), eq(MUSIC_TRACK_ADMIN_ROLES));

        mockMvc.perform(get("/api/v1/admin/music-tracks")
                        .principal(authentication("ROLE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AD0003"));
    }

    private void operator() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(MUSIC_TRACK_ADMIN_ROLES)))
                .thenReturn(new AdminUserInfo(3L, 7L, "OPERATOR"));
    }

    private static Authentication authentication(String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", authority);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static MockMultipartFile audioFile() {
        return new MockMultipartFile(
                "file",
                "music.mp3",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "audio".getBytes()
        );
    }

    private static MockMultipartFile oversizedAudioFile() {
        return new MockMultipartFile(
                "file",
                "large.mp3",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[(10 * 1024 * 1024) + 1]
        );
    }

    private static AdminMusicTrackListResponse page(AdminMusicTrackResponse response) {
        return new AdminMusicTrackListResponse(
                List.of(response),
                0,
                20,
                1,
                1,
                true,
                true,
                "createdAt,desc"
        );
    }

    private static AdminMusicTrackResponse response(Long id, String status) {
        return new AdminMusicTrackResponse(
                id,
                "새 배경음악",
                "BGM",
                "audio/mpeg",
                5L,
                180,
                5,
                "운영자가 라이선스를 확인함",
                status,
                "/api/v1/music/tracks/" + id + "/stream",
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 10, 0)
        );
    }
}
