package com.qtai.domain.member.api.dto;

import com.qtai.domain.member.internal.Member;

import java.time.LocalDateTime;

/**
 * 회원 정보 응답 DTO.
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
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getProfileImageUrl(),
                member.getStatus().name(),
                member.getRole(),
                member.getNicknameUnlockAt(),
                member.getCreatedAt()
        );
    }
}
