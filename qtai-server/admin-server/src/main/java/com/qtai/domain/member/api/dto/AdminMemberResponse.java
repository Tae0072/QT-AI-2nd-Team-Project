package com.qtai.domain.member.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 회원 목록·상세 응답 DTO (F-04/F-10).
 *
 * <p>개인정보 최소노출 원칙(CLAUDE.md §7): kakaoId·email 원문은 운영 화면에 노출하지 않는다.
 * 닉네임·상태·권한·가입/탈퇴 시각만 제공한다.
 */
public record AdminMemberResponse(
        Long id,
        String nickname,
        String status,
        String role,
        LocalDateTime nicknameChangedAt,
        LocalDateTime withdrawnAt,
        LocalDateTime createdAt
) {
}
