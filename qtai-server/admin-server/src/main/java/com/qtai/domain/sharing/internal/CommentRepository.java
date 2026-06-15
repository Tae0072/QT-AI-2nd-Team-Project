package com.qtai.domain.sharing.internal;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 글의 살아있는 댓글을 시간순(오래된→최신)으로 페이징. 소프트 삭제(is_deleted=true)는 제외. */
    Page<Comment> findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long sharingPostId, Pageable pageable);

    /** 글의 살아있는 댓글 수. commentCount COUNT 재계산용. */
    long countBySharingPostIdAndIsDeletedFalse(Long sharingPostId);

    /** 회원이 작성한 댓글 ID 목록 — 관리자 회원 상세에서 받은 신고(COMMENT) 집계용. */
    @Query("SELECT c.id FROM Comment c WHERE c.memberId = :memberId")
    List<Long> findIdsByMemberId(@Param("memberId") Long memberId);

    /** 회원이 작성한 댓글(삭제 포함)을 최신순으로 — 관리자 회원 상세 목록용. */
    Page<Comment> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
