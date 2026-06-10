package com.qtai.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** lib-common 공통 응답 envelope 스모크. */
class ApiResponseTest {

    @Test
    void successResponseCarriesData() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertTrue(response.success());
        assertEquals("ok", response.data());
        assertNull(response.error());
    }

    @Test
    void errorResponseCarriesCode() {
        ApiResponse<Void> response = ApiResponse.error("C0001", "실패");

        assertFalse(response.success());
        assertNull(response.data());
        assertEquals("C0001", response.error().code());
        assertEquals("실패", response.error().message());
    }
}
