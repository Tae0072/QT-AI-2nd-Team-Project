package com.qtai.domain.sharing.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * likedByMe 배치 조회. 피드 한 페이지의 게시글 id들(postIds) 중
     * 해당 회원(memberId)이 좋아요를 누른 id만 한 번에 반환한다.
     * 게시글마다 따로 조회하면 N+1이 되므로 IN 절로 1회에 모은다.
     */
    @Query("""
            SELECT pl.sharingPostId FROM PostLike pl
            WHERE pl.memberId = :memberId
              AND pl.sharingPostId IN :postIds
            """)
    List<Long> findLikedPostIds(@Param("memberId") Long memberId,
                                @Param("postIds") Collection<Long> postIds);
}
