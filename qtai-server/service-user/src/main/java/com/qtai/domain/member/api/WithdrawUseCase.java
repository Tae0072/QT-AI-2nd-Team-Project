package com.qtai.domain.member.api;

/**
 * 회원 탈퇴 UseCase 포트.
 *
 * 정책(2026-06-05 Lead 결정): status=WITHDRAWN 전환 + 세션 무효화.
 * 개인정보는 즉시 익명화하지 않고 2년 보존(탈퇴 시 고지), 만료 삭제는 별도 배치.
 * 보존 중 같은 카카오 계정으로 재로그인하면 기존 계정을 재활성화한다.
 */
public interface WithdrawUseCase {

    void withdraw(Long memberId, String reason);
}
