package com.qtai.common;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 단독(standalone) MockMvc 테스트.
 * Spring 컨텍스트 없이 직접 MockMvc를 구성하여 예외 → ApiResponse 변환 검증.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException → 해당 ErrorCode의 HTTP 상태 + 표준 에러 본문")
    void businessException_returns_errorCode_status() throws Exception {
        mockMvc.perform(get("/test/business-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0001"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("예상치 못한 Exception → 500 + C0001 에러 코드")
    void unexpectedException_returns_500() throws Exception {
        mockMvc.perform(get("/test/unexpected-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0001"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    // ── 테스트용 더미 컨트롤러 ──

    @RestController
    static class ErrorTestController {

        @GetMapping("/test/business-error")
        public void throwBusiness() {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        @GetMapping("/test/unexpected-error")
        public void throwUnexpected() {
            throw new RuntimeException("테스트용 예상치 못한 예외");
        }
    }
}
