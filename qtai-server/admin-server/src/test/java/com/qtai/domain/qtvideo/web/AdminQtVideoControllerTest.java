package com.qtai.domain.qtvideo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.qtvideo.internal.AdminQtVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 컨트롤러 슬라이스(standalone MockMvc) 테스트. 컨트롤러는 인증·위임만 담당하고
 * 감사 로그는 서비스 트랜잭션 안에서 기록하므로, 여기서는 각 엔드포인트가 인증 관리자(adminUserId)와
 * 함께 서비스를 호출하는지와 응답 상태를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminQtVideoControllerTest {

    @Mock
    private AdminQtVideoService adminQtVideoService;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @Mock
    private ListBibleBooksUseCase listBibleBooksUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminQtVideoController controller = new AdminQtVideoController(
                adminQtVideoService,
                verifyAdminRoleUseCase,
                listBibleBooksUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("성경권 목록 조회")
    void listBibleBooks() throws Exception {
        manager();
        mockMvc.perform(get("/api/v1/admin/qt-videos/bible-books").principal(authentication()))
                .andExpect(status().isOk());
        verify(listBibleBooksUseCase).listBibleBooks();
    }

    @Test
    @DisplayName("원본 영상 목록 조회")
    void listSourceVideos() throws Exception {
        manager();
        mockMvc.perform(get("/api/v1/admin/qt-videos/source-videos")
                        .param("bibleBookId", "46").param("status", "ACTIVE")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).listSourceVideos((short) 46, "ACTIVE", 0, 20);
    }

    @Test
    @DisplayName("원본 영상 등록 — 관리자 ID와 함께 서비스 호출")
    void createSourceVideo() throws Exception {
        manager();
        mockMvc.perform(post("/api/v1/admin/qt-videos/source-videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bibleBookId\":46,\"title\":\"t\",\"videoUrl\":\"u\",\"durationSec\":100}")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).createSourceVideo(
                eq(100L), eq((short) 46), eq("t"), eq("u"), eq(new BigDecimal("100")));
    }

    @Test
    @DisplayName("원본 영상 수정")
    void updateSourceVideo() throws Exception {
        manager();
        mockMvc.perform(patch("/api/v1/admin/qt-videos/source-videos/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"videoUrl\":\"u\",\"durationSec\":100,\"status\":\"ACTIVE\"}")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).updateSourceVideo(
                eq(100L), eq(3L), eq("t"), eq("u"), eq(new BigDecimal("100")), eq("ACTIVE"));
    }

    @Test
    @DisplayName("원본 영상 삭제 — 204 + 관리자 ID 전달")
    void deleteSourceVideo() throws Exception {
        manager();
        mockMvc.perform(delete("/api/v1/admin/qt-videos/source-videos/4").principal(authentication()))
                .andExpect(status().isNoContent());
        verify(adminQtVideoService).deleteSourceVideo(100L, 4L);
    }

    @Test
    @DisplayName("절별 구간 조회")
    void listSegments() throws Exception {
        manager();
        mockMvc.perform(get("/api/v1/admin/qt-videos/source-videos/3/segments").principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).listSegments(3L);
    }

    @Test
    @DisplayName("절별 구간 저장")
    void replaceSegments() throws Exception {
        manager();
        mockMvc.perform(put("/api/v1/admin/qt-videos/source-videos/3/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segments\":[{\"chapter\":10,\"verse\":14,\"startTimeSec\":0,\"endTimeSec\":10}]}")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).replaceSegments(eq(100L), eq(3L), any());
    }

    @Test
    @DisplayName("QT 클립 목록 조회")
    void listClips() throws Exception {
        manager();
        mockMvc.perform(get("/api/v1/admin/qt-videos/clips").param("qtPassageId", "2")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).listClips(2L, null, 0, 20);
    }

    @Test
    @DisplayName("QT 클립 생성")
    void prepareClip() throws Exception {
        manager();
        mockMvc.perform(post("/api/v1/admin/qt-videos/qt-passages/2/clips/prepare")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).prepareClip(100L, 2L);
    }

    @Test
    @DisplayName("QT 클립 삭제 — 204 + 관리자 ID 전달")
    void deleteClip() throws Exception {
        manager();
        mockMvc.perform(delete("/api/v1/admin/qt-videos/clips/7").principal(authentication()))
                .andExpect(status().isNoContent());
        verify(adminQtVideoService).deleteClip(100L, 7L);
    }

    @Test
    @DisplayName("QT 클립 상태 변경")
    void changeClipStatus() throws Exception {
        manager();
        mockMvc.perform(patch("/api/v1/admin/qt-videos/clips/7/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}")
                        .principal(authentication()))
                .andExpect(status().isOk());
        verify(adminQtVideoService).changeClipStatus(100L, 7L, "APPROVED");
    }

    @Test
    @DisplayName("SUPER_ADMIN 우월권으로 통과")
    void deleteClip_allowsSuperAdmin() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR", "REVIEWER", "CONTENT_CREATOR"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "SUPER_ADMIN"));
        mockMvc.perform(delete("/api/v1/admin/qt-videos/clips/7").principal(authentication()))
                .andExpect(status().isNoContent());
        verify(adminQtVideoService).deleteClip(100L, 7L);
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 차단하고 서비스를 호출하지 않는다")
    void rejectsNonAdminBeforeServiceCall() throws Exception {
        TestingAuthenticationToken user = new TestingAuthenticationToken("7", "n/a", "ROLE_USER");
        user.setAuthenticated(true);
        mockMvc.perform(delete("/api/v1/admin/qt-videos/clips/7").principal(user))
                .andExpect(status().isForbidden());
        verify(adminQtVideoService, never()).deleteClip(any(), any());
    }

    private void manager() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR", "REVIEWER", "CONTENT_CREATOR"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "OPERATOR"));
    }

    private static Authentication authentication() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("7", "n/a", "ROLE_ADMIN");
        authentication.setAuthenticated(true);
        return authentication;
    }
}
