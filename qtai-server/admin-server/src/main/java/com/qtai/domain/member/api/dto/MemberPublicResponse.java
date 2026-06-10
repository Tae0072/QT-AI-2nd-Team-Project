package com.qtai.domain.member.api.dto;

/**
 * 타 회원 공개 프로필 응답 DTO.
 *
 * <p>GET /api/v1/members/{id} — 이메일·역할·닉네임 잠금 등 비공개 필드를 제외한다.
 */
public record MemberPublicResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {
}
