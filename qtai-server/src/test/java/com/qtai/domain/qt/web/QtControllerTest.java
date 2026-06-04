package com.qtai.domain.qt.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
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

/**
 * QtController MockMvc 슬라이스 테스트 — F-01.
 *
 * <p>Security 필터 비활성화 (addFilters=false).
 * SecurityContextHolder에 직접 Authentication 세팅하여
 * {@code @AuthenticationPrincipal Long memberId} 바인딩을 검증한다.
 *
 * <p>검증 항목:
 * <ul>
 *   <li>GET /today → 200 + ApiResponse envelope</li>
 *   <li>GET /passages/{id} → 200 + 단건 조회</li>
 *   <li>GET /passages/{id} → 404 QT_PASSAGE_NOT_FOUND</li>
 *   <li>STALE_FALLBACK 등 다양한 cacheStatus 정상 반환</li>
 * </ul>
 */
@WebMvcTest(QtController.class)
@AutoConfigureMockMvc(addFilters = false)
class QtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private GetTodayQtUseCase getTodayQtUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /today — 200 + ApiResponse envelope + HIT 응답")
    void getToday_200_성공() throws Exception {
        TodayQtResponse response = new TodayQtResponse(
                1L, "2026-05-28", "하나님이 세상을 이처럼 사랑하사",
                "MISSING", false, null, "HIT",
                new TodayQtRangeResponse("NEW", "1CO", "고린도전서", "1 Corinthians", 1, 10, 17, "고린도전서 1:10-17"));
        when(getTodayQtUseCase.getToday(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/qt/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qtPassageId").value(1))
                .andExpect(jsonPath("$.data.passageDate").value("2026-05-28"))
                .andExpect(jsonPath("$.data.title").value("하나님이 세상을 이처럼 사랑하사"))
                .andExpect(jsonPath("$.data.simulatorStatus").value("MISSING"))
                .andExpect(jsonPath("$.data.hasExplanation").value(false))
                .andExpect(jsonPath("$.data.cacheStatus").value("HIT"))
                .andExpect(jsonPath("$.data.range.testament").value("NEW"))
                .andExpect(jsonPath("$.data.range.bookCode").value("1CO"))
                .andExpect(jsonPath("$.data.range.chapter").value(1))
                .andExpect(jsonPath("$.data.range.verseFrom").value(10))
                .andExpect(jsonPath("$.data.range.verseTo").value(17));
    }

    @Test
    @DisplayName("GET /passages/{id} — 200 + 단건 본문 조회 성공")
    void getPassage_200_성공() throws Exception {
        TodayQtResponse response = new TodayQtResponse(
                5L, "2026-05-26", "태초에 하나님이",
                "MISSING", false, null, "HIT");
        when(getTodayQtUseCase.getPassage(any(), eq(5L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/qt/passages/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qtPassageId").value(5))
                .andExpect(jsonPath("$.data.title").value("태초에 하나님이"));
    }

    @Test
    @DisplayName("GET /passages/{id} — 404 QT_PASSAGE_NOT_FOUND")
    void getPassage_404_미존재() throws Exception {
        when(getTodayQtUseCase.getPassage(any(), eq(999L)))
                .thenThrow(new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/qt/passages/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("Q0001"));
    }

    @Test
    @DisplayName("GET /today — STALE_FALLBACK 응답도 정상 반환")
    void getToday_STALE_FALLBACK_성공() throws Exception {
        TodayQtResponse response = new TodayQtResponse(
                2L, "2026-05-27", "여호와는 나의 목자시니",
                "MISSING", false, null, "STALE_FALLBACK");
        when(getTodayQtUseCase.getToday(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/qt/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cacheStatus").value("STALE_FALLBACK"))
                .andExpect(jsonPath("$.data.passageDate").value("2026-05-27"));
    }
}
