package com.qtai.domain.member.api.dto;

/** 회원 정보 응답 DTO. */
public record MemberResponse(
        // TODO: Long id
        // TODO: String nickname
        // TODO: String email           — 정책에 따라 마스킹 또는 본인만 노출
        // TODO: String profileImageUrl
        // TODO: MemberStatus status    — ACTIVE / WITHDRAWN / SUSPENDED
        // TODO: LocalDateTime joinedAt
) {}
