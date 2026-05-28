package com.qtai.domain.member.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.notification.api.ListNotificationUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MyPageController MockMvc 슬라이스 테스트.
 */
@WebMvcTest(MyPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private GetMemberUseCase getMemberUseCase;

    @MockBean
    private ListNotificationUseCase listNotificationUseCase;

    @MockBean
    private ListMemberPraiseSongUseCase listMemberPraiseSongUseCase;

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
    void dashboard_200_정상_응답() throws Exception {
        MemberResponse member = new MemberResponse(
                1L, "user1", "user@test.com", null,
                "ACTIVE", "USER", null, null);
        when(getMemberUseCase.getMember(1L)).thenReturn(member);
        when(listNotificationUseCase.countUnread(1L)).thenReturn(3L);
        when(listMemberPraiseSongUseCase.countMy(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/me/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.nickname").value("user1"))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(3))
                .andExpect(jsonPath("$.data.praiseSummary.savedSongCount").value(5))
                .andExpect(jsonPath("$.data.widgetErrors").isEmpty());
    }

    @Test
    void dashboard_위젯_부분_실패_widgetErrors_포함() throws Exception {
        when(getMemberUseCase.getMember(1L))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        when(listNotificationUseCase.countUnread(1L)).thenReturn(0L);
        when(listMemberPraiseSongUseCase.countMy(1L)).thenReturn(0L);

        mockMvc.perform(get("/api/v1/me/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.widgetErrors[0]").value("profile"));
    }

}
