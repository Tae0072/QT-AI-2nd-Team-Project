package com.qtai.domain.member.api.dto;

import java.time.LocalDateTime;

/** 관리자 회원 상세 — 닉네임 변경 이력 1건(이전→이후, 변경 시각). */
public record NicknameHistoryItem(
        String oldNickname,
        String newNickname,
        LocalDateTime changedAt
) {
}
