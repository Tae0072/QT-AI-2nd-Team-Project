package com.qtai.domain.praise.api.dto;

import java.time.LocalDateTime;

/**
 * 찬양 큐레이션 곡 응답 DTO.
 *
 * <p>도메인 경계 정책: api/dto 는 internal 패키지를 import 하지 않는다.
 */
public record PraiseResponse(
        Long id,
        String title,
        String artist,
        String sourceType,
        String status,
        LocalDateTime createdAt
) {
}
