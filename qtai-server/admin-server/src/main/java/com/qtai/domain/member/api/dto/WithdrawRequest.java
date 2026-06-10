package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.Size;

/**
 * 회원 탈퇴 요청 DTO.
 *
 * @param reason 탈퇴 사유 (선택, 감사 레코드용). 최대 500자.
 */
public record WithdrawRequest(
        @Size(max = 500, message = "탈퇴 사유는 500자 이내로 입력해주세요.")
        String reason
) {
}
