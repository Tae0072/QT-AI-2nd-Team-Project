package com.qtai.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    @DisplayName("ErrorCode만 주면 message는 ErrorCode 기본 메시지")
    void usesErrorCodeMessageByDefault() {
        BusinessException e = new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("detailMessage를 주면 message가 그 값으로 대체된다")
    void usesDetailMessageWhenProvided() {
        BusinessException e = new BusinessException(ErrorCode.INVALID_INPUT, "닉네임 형식 오류");

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(e.getMessage()).isEqualTo("닉네임 형식 오류");
    }
}
