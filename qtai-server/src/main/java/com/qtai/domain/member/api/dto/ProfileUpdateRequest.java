package com.qtai.domain.member.api.dto;

/** 프로필 수정 요청 DTO. */
public record ProfileUpdateRequest(
        // TODO: String nickname        — 변경할 닉네임 (필수, 2~20자, @Size)
        // TODO: String profileImageUrl — 변경할 프로필 이미지 URL (null이면 유지)
) {}
