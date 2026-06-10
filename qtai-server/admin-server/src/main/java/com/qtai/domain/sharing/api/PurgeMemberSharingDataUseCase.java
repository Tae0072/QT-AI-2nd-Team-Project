package com.qtai.domain.sharing.api;

/**
 * 회원 보존기간 만료 정리 — sharing 도메인 데이터 삭제 포트.
 *
 * <p>탈퇴 후 2년(보존기간) 경과 회원의 나눔 관련 데이터를 hard delete한다:
 * 회원이 누른 좋아요, 회원 글에 달린 좋아요(타인 포함), 회원 댓글과 회원 글의
 * 댓글 트리 전체(타인 대댓글 포함 — 2026-06-05 결정), 나눔 글 본체.
 * member 도메인의 보존기간 만료 배치(SYSTEM_BATCH)에서만 호출한다.
 *
 * <p>note 도메인 정리보다 먼저 호출해야 한다 (sharing_posts.note_id FK).
 */
public interface PurgeMemberSharingDataUseCase {

    /**
     * 해당 회원의 나눔 데이터를 모두 삭제한다 (호출자 트랜잭션에 참여).
     *
     * @param memberId 대상 회원 ID
     * @return 삭제된 행 수 합계
     */
    int purgeByMemberId(Long memberId);
}
