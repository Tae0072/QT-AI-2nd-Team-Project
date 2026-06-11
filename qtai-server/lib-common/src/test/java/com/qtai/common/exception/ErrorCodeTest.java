package com.qtai.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCodeTest {

    @Test
    @DisplayName("모든 ErrorCode는 code·message·httpStatus를 보유한다")
    void everyCode_hasFields() {
        for (ErrorCode c : ErrorCode.values()) {
            assertThat(c.getCode()).as(c.name() + ".code").isNotBlank();
            assertThat(c.getMessage()).as(c.name() + ".message").isNotBlank();
            assertThat(c.getHttpStatus()).as(c.name() + ".httpStatus").isNotNull();
        }
    }

    @Test
    @DisplayName("ErrorCode 코드 값은 전역에서 유일하다")
    void codes_areUnique() {
        long distinct = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();
        assertThat(distinct).isEqualTo(ErrorCode.values().length);
    }

    @Test
    @DisplayName("INTERNAL_ERROR는 C0001 / 500 매핑")
    void internalError_mapping() {
        assertThat(ErrorCode.INTERNAL_ERROR.getCode()).isEqualTo("C0001");
        assertThat(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
