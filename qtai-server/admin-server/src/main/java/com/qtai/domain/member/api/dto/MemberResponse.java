package com.qtai.domain.member.api.dto;

import java.time.LocalDateTime;

/**
 * 회원 정보 응답 DTO.
 *
 * <p>도메인 경계 정책: api/dto 는 internal 패키지를 import 하지 않는다.
 * Entity → DTO 변환은 Service 레이어에서 수행한다.
 */
public record MemberResponse(
        Long id,
        String nickname,
        String email,
        String profileImageUrl,
        String status,
        String role,
        LocalDateTime nicknameUnlockAt,
        LocalDateTime createdAt
) {
}
