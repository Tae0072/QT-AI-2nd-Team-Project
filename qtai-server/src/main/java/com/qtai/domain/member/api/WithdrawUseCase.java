package com.qtai.domain.member.api;

/**
 * 회원 탈퇴 UseCase 포트.
 *
 * 정책: status=WITHDRAWN 전환 + 개인정보 익명화.
 */
public interface WithdrawUseCase {

    void withdraw(Long memberId, String reason);
}
