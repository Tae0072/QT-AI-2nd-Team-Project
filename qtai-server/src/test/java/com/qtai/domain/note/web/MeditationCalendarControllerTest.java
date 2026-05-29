package com.qtai.domain.note.web;

import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeditationCalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeditationCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
        when(clock.instant()).thenReturn(Instant.parse("2026-05-28T03:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("authenticated request delegates member and month")
    void getCalendar_delegatesMemberAndMonth() throws Exception {
        MeditationCalendarResponse response = new MeditationCalendarResponse(
                "2026-05",
                List.of(new MeditationCalendarResponse.Day(LocalDate.of(2026, 5, 28), true, 1, 99L, List.of())),
                new MeditationCalendarResponse.Summary(1, 1, 1)
        );
        when(getMeditationCalendarUseCase.getCalendar(1L, YearMonth.of(2026, 5))).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/meditation-calendar").param("month", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value("2026-05"))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-05-28"))
                .andExpect(jsonPath("$.data.days[0].saved").value(true))
                .andExpect(jsonPath("$.data.summary.savedDays").value(1));

        verify(getMeditationCalendarUseCase).getCalendar(1L, YearMonth.of(2026, 5));
    }

    @Test
    @DisplayName("missing month uses current KST month")
    void getCalendar_missingMonth_usesCurrentKstMonth() throws Exception {
        MeditationCalendarResponse response = new MeditationCalendarResponse(
                "2026-05", List.of(), new MeditationCalendarResponse.Summary(0, 0, 0));
        when(getMeditationCalendarUseCase.getCalendar(1L, YearMonth.of(2026, 5))).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/meditation-calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value("2026-05"));

        verify(getMeditationCalendarUseCase).getCalendar(1L, YearMonth.of(2026, 5));
    }

    @Test
    @DisplayName("invalid month format is rejected")
    void getCalendar_invalidMonth_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/me/meditation-calendar").param("month", "2026-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing member id is unauthorized")
    void getCalendar_missingMember_rejected() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/me/meditation-calendar").param("month", "2026-05"))
                .andExpect(status().isUnauthorized());
    }
}
