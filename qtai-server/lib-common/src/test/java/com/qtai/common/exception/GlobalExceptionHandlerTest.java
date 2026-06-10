package com.qtai.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.qtai.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

/** GlobalExceptionHandler 단위 테스트 — 표준 ApiResponse 변환/상태코드 검증. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void BusinessException은_ErrorCode의_상태와_코드로_변환된다() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusiness(new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("B0002", response.getBody().error().code());
    }

    @Test
    void AccessDenied는_403_FORBIDDEN으로_변환된다() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("M0003", response.getBody().error().code());
    }
}
