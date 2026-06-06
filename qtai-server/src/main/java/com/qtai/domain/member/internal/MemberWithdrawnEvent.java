package com.qtai.domain.member.internal;

/**
 * 회원 탈퇴 완료 인프로세스 이벤트 (member 도메인 내부 전용).
 *
 * <p>탈퇴 트랜잭션 커밋 후(AFTER_COMMIT) 부수 효과(세션 무효화 등)를 분리하기 위해
 * 발행한다 — 트랜잭션 롤백 시 토큰이 유지되도록 보장.
 *
 * @param eventId  핸들러 실패 로그 추적용 식별자
 * @param memberId 탈퇴한 회원 ID
 */
public record MemberWithdrawnEvent(String eventId, Long memberId) {
}
