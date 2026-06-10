package com.qtai.domain.sharing.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 글의 살아있는 댓글을 시간순(오래된→최신)으로 페이징. 소프트 삭제(is_deleted=true)는 제외. */
    Page<Comment> findBySharingPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long sharingPostId, Pageable pageable);

    /** 글의 살아있는 댓글 수. commentCount COUNT 재계산용. */
    long countBySharingPostIdAndIsDeletedFalse(Long sharingPostId);
}
