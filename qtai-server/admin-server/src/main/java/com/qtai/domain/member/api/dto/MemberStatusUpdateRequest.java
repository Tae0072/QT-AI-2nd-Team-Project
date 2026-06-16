package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 관리자 회원 상태 변경 요청 (F-10 신고 회피·트롤 대응).
 *
 * <p>허용 값은 ACTIVE(정지 해제) / SUSPENDED(정지)뿐이다.
 * WITHDRAWN(탈퇴)은 이 경로로 설정할 수 없다.
 */
public record MemberStatusUpdateRequest(
        @NotBlank(message = "status는 필수입니다.")
        @Pattern(regexp = "ACTIVE|SUSPENDED", message = "status는 ACTIVE 또는 SUSPENDED만 허용됩니다.")
        String status
) {
}
