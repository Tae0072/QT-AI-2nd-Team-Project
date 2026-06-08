package com.qtai.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    @DisplayName("success()는 success=true·data 설정·error=null·traceId 비어있지 않음")
    void success_buildsSuccessEnvelope() {
        ApiResponse<String> r = ApiResponse.success("ok");

        assertThat(r.success()).isTrue();
        assertThat(r.data()).isEqualTo("ok");
        assertThat(r.error()).isNull();
        assertThat(r.timestamp()).isNotNull();
        assertThat(r.traceId()).isNotBlank();
    }

    @Test
    @DisplayName("error()는 success=false·data=null·error.code/message 설정")
    void error_buildsErrorEnvelope() {
        ApiResponse<Object> r = ApiResponse.error("C0001", "서버 오류");

        assertThat(r.success()).isFalse();
        assertThat(r.data()).isNull();
        assertThat(r.error()).isNotNull();
        assertThat(r.error().code()).isEqualTo("C0001");
        assertThat(r.error().message()).isEqualTo("서버 오류");
        assertThat(r.traceId()).isNotBlank();
    }
}
