package com.qtai.domain.member.api;

/**
 * 보존기간(2년) 만료 탈퇴 회원 정리 UseCase 포트.
 *
 * <p>정책(2026-06-05 Lead 결정): 탈퇴 시 "개인정보와 작성 기록은 2년간 보관 후
 * 자동 삭제"를 고지한다. 이 UseCase는 탈퇴(withdrawn_at) 후 2년이 지난 회원의
 * 회원 정보와 작성 콘텐츠(노트·나눔 글·댓글·좋아요 등)를 모두 hard delete한다.
 * 탈퇴자 나눔 글에 달린 다른 회원의 댓글·좋아요도 글과 함께 삭제된다(연쇄 삭제).
 *
 * <p>호출 주체: 내부 배치(SYSTEM_BATCH)만 호출한다. 사용자 요청 경로에서 호출하지 않는다.
 */
public interface PurgeExpiredWithdrawnMembersUseCase {

    /**
     * 보존기간이 만료된 탈퇴 회원을 일괄 삭제한다.
     *
     * @return 삭제된 회원 수
     */
    int purgeExpired();
}
