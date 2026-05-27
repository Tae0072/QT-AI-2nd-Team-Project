package com.qtai.domain.member.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarDay;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.domain.note.api.dto.MeditationCalendarSummary;
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

import java.time.LocalDate;
import java.time.YearMonth;

import static com.qtai.domain.note.api.NoteCategory.MEDITATION;

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
    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;

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

    @Test
    void meditationCalendar_200_noteUseCase_위임() throws Exception {
        MeditationCalendarResponse response = new MeditationCalendarResponse(
                "2026-05",
                List.of(new MeditationCalendarDay(
                        LocalDate.of(2026, 5, 17),
                        true,
                        1,
                        200L,
                        List.of(MEDITATION)
                )),
                new MeditationCalendarSummary(1, 1, 1)
        );
        when(getMeditationCalendarUseCase.get(1L, YearMonth.of(2026, 5))).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/meditation-calendar").param("month", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value("2026-05"))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-05-17"))
                .andExpect(jsonPath("$.data.days[0].meditationNoteId").value(200))
                .andExpect(jsonPath("$.data.summary.savedDays").value(1));
    }

    @Test
    void meditationCalendar_인증_없음_UNAUTHORIZED() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/me/meditation-calendar").param("month", "2026-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }
}
