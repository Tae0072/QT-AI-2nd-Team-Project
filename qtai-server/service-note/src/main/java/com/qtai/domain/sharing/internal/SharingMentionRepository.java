package com.qtai.domain.sharing.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SharingMentionRepository extends JpaRepository<SharingMention, Long> {

    /**
     * 내가 태그된 글 목록(PUBLISHED만). sharing_mentions와 sharing_posts를 theta join으로 묶어
     * 내가 멘션된 글을 중복 없이(DISTINCT) 최근 글 순으로 페이지 반환한다.
     * 한 글에 여러 번 멘션돼도 글은 한 번만 나온다. 글이 숨김·삭제되면 제외된다.
     */
    @Query(value = """
            SELECT DISTINCT sp FROM SharingMention m, SharingPost sp
            WHERE sp.id = m.sharingPostId
              AND m.mentionedMemberId = :memberId
              AND sp.status = :status
            ORDER BY sp.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT m.sharingPostId) FROM SharingMention m, SharingPost sp
            WHERE sp.id = m.sharingPostId
              AND m.mentionedMemberId = :memberId
              AND sp.status = :status
            """)
    Page<SharingPost> findMentionedPosts(@Param("memberId") Long memberId,
                                         @Param("status") SharingPostStatus status,
                                         Pageable pageable);
}
