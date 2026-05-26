package com.qtai.domain.notification.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.qtai.domain.notification.api.ListNotificationUseCase;
import com.qtai.domain.notification.api.MarkAsReadUseCase;
import com.qtai.domain.notification.api.dto.NotificationResponse;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * NotificationController MockMvc 슬라이스 테스트.
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ListNotificationUseCase listNotificationUseCase;

    @MockBean
    private MarkAsReadUseCase markAsReadUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMy_200_알림_목록() throws Exception {
        NotificationResponse noti = new NotificationResponse(
                10L, "LIKE", "좋아요", "본문", "SHARING_POST", 5L,
                false, null, LocalDateTime.of(2026, 5, 26, 12, 0));
        Page<NotificationResponse> page = new PageImpl<>(List.of(noti));
        when(listNotificationUseCase.listMy(eq(1L), eq(false), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("좋아요"));
    }

    @Test
    void markAsRead_204() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/10/read"))
                .andExpect(status().isNoContent());

        verify(markAsReadUseCase).markAsRead(1L, 10L);
    }

    @Test
    void markAllAsRead_204() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(markAsReadUseCase).markAllAsRead(1L);
    }
}
