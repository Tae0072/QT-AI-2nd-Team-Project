package com.qtai.domain.member.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.mission.api.GetMemberMissionProgressUseCase;
import com.qtai.domain.mission.api.dto.MissionProgressResponse;
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

    @MockBean
    private GetMemberMissionProgressUseCase getMemberMissionProgressUseCase;

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
        MissionProgressResponse mission = new MissionProgressResponse(
                5L, "MED_30", "묵상 30일", "MEDITATION_SAVED_DAYS", "MONTHLY",
                10, 30, new BigDecimal("33.33"), false,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null);
        when(getMemberMissionProgressUseCase.getMissionProgress(1L)).thenReturn(List.of(mission));

        mockMvc.perform(get("/api/v1/me/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.nickname").value("user1"))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(3))
                .andExpect(jsonPath("$.data.praiseSummary.savedSongCount").value(5))
                .andExpect(jsonPath("$.data.missionProgress[0].title").value("묵상 30일"))
                .andExpect(jsonPath("$.data.missionProgress[0].progressRate").value(33.33))
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

    @Test
    void dashboard_미션_위젯_실패시_빈배열_widgetErrors_포함() throws Exception {
        MemberResponse member = new MemberResponse(
                1L, "user1", "user@test.com", null,
                "ACTIVE", "USER", null, null);
        when(getMemberUseCase.getMember(1L)).thenReturn(member);
        when(listNotificationUseCase.countUnread(1L)).thenReturn(0L);
        when(listMemberPraiseSongUseCase.countMy(1L)).thenReturn(0L);
        // 미션 위젯만 실패 → 부분 실패 격리: missionProgress는 빈 배열, widgetErrors에 기록
        when(getMemberMissionProgressUseCase.getMissionProgress(1L))
                .thenThrow(new RuntimeException("mission unavailable"));

        mockMvc.perform(get("/api/v1/me/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missionProgress").isEmpty())
                .andExpect(jsonPath("$.data.widgetErrors[0]").value("missionProgress"));
    }

}
