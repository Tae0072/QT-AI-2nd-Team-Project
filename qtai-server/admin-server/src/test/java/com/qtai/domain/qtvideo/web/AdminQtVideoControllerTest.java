package com.qtai.domain.qtvideo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.qtai.common.exception.GlobalExceptionHandler;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.qtvideo.internal.AdminQtVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminQtVideoControllerTest {

    @Mock
    private AdminQtVideoService adminQtVideoService;

    @Mock
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @Mock
    private WriteAuditLogUseCase auditLogUseCase;

    @Mock
    private ListBibleBooksUseCase listBibleBooksUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminQtVideoController controller = new AdminQtVideoController(
                adminQtVideoService,
                verifyAdminRoleUseCase,
                auditLogUseCase,
                listBibleBooksUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("관리자는 원본 영상을 삭제할 수 있고 감사 로그를 남긴다")
    void deleteSourceVideo_returnsNoContentAndWritesAudit() throws Exception {
        manager();

        mockMvc.perform(delete("/api/v1/admin/qt-videos/source-videos/4")
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(adminQtVideoService).deleteSourceVideo(4L);

        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(captor.capture());
        AuditLogWriteRequest audit = captor.getValue();
        assertThat(audit.actionType()).isEqualTo("QT_VIDEO_SOURCE_DELETE");
        assertThat(audit.targetType()).isEqualTo("SOURCE_VIDEO");
        assertThat(audit.targetId()).isEqualTo(4L);
        assertThat(audit.actorId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("관리자는 QT 클립을 삭제할 수 있고 감사 로그를 남긴다")
    void deleteClip_returnsNoContentAndWritesAudit() throws Exception {
        manager();

        mockMvc.perform(delete("/api/v1/admin/qt-videos/clips/7")
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(adminQtVideoService).deleteClip(7L);

        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(captor.capture());
        AuditLogWriteRequest audit = captor.getValue();
        assertThat(audit.actionType()).isEqualTo("QT_VIDEO_CLIP_DELETE");
        assertThat(audit.targetType()).isEqualTo("QT_VIDEO_CLIP");
        assertThat(audit.targetId()).isEqualTo(7L);
        assertThat(audit.actorId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("SUPER_ADMIN도 verifyAnyRole 우월권 결과로 QT 영상 삭제 API를 통과한다")
    void deleteClip_allowsSuperAdminResultFromAdminRoleUseCase() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), eq(List.of("OPERATOR", "REVIEWER", "CONTENT_CREATOR"))))
                .thenReturn(new AdminUserInfo(100L, 7L, "SUPER_ADMIN"));

        mockMvc.perform(delete("/api/v1/admin/qt-videos/clips/7")
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(adminQtVideoService).deleteClip(7L);
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한이 없으면 QT 영상 삭제 API를 차단한다")
    void deleteClip_rejectsNonAdminRoleBeforeServiceCall() throws Exception {
        TestingAuthenticationToken user = new TestingAuthenticationToken("7", "n/a", "ROLE_USER");
        user.setAuthenticated(true);

        mockMvc.perform(delete("/api/v1/admin/qt-videos/clips/7")
                        .principal(user))
                .andExpect(status().isForbidden());

        verify(adminQtVideoService, never()).deleteClip(any());
        verify(auditLogUseCase, never()).write(any());
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
