package com.qtai.domain.qt.web;

import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.security.JwtProvider;
import com.qtai.security.SecurityConfig;
import com.qtai.security.SecurityErrorResponseWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QtController 보안 테스트 — F-01.
 *
 * <p>Security 필터 활성화 상태에서 미인증 접근 시 401을 검증한다.
 * CLAUDE.md §5 "인증되지 않은 사용자는 Kakao login 시작만 가능" 정책.
 */
@WebMvcTest(QtController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class})
class QtControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private GetTodayQtUseCase getTodayQtUseCase;

    @Test
    @DisplayName("GET /today — 미인증 시 401 Unauthorized")
    void getToday_미인증_401() throws Exception {
        mockMvc.perform(get("/api/v1/qt/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /passages/{id} — 미인증 시 401 Unauthorized")
    void getPassage_미인증_401() throws Exception {
        mockMvc.perform(get("/api/v1/qt/passages/1"))
                .andExpect(status().isUnauthorized());
    }
}
