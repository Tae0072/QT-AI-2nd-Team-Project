package com.qtai.domain.sharing.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SharingBookmarkRepository extends JpaRepository<SharingBookmark, Long> {

    /**
     * bookmarkedByMe 배치 조회. 피드 한 페이지의 게시글 id들(postIds) 중
     * 해당 회원(memberId)이 저장한 id만 한 번에 반환한다. (좋아요 findLikedPostIds와 동일 패턴, N+1 방지)
     */
    @Query("""
            SELECT b.sharingPostId FROM SharingBookmark b
            WHERE b.memberId = :memberId
              AND b.sharingPostId IN :postIds
            """)
    List<Long> findBookmarkedPostIds(@Param("memberId") Long memberId,
                                     @Param("postIds") Collection<Long> postIds);

    /** 이미 저장했는지. 저장 추가 멱등 처리용(중복 INSERT 사전 차단). */
    boolean existsBySharingPostIdAndMemberId(Long sharingPostId, Long memberId);

    /** 저장 해제. 행이 없어도 0건 삭제로 끝나 멱등(예외 없음). */
    void deleteBySharingPostIdAndMemberId(Long sharingPostId, Long memberId);

    /**
     * 내 저장 목록(PUBLISHED 글만). sharing_bookmarks와 sharing_posts를 theta join으로 묶어
     * 내가 저장한 글 중 현재 공개 상태인 글을, 최근 저장순(b.id DESC)으로 페이지 반환한다.
     * 저장 후 글이 숨김·삭제됐으면 목록에서 제외된다(status 필터).
     */
    @Query(value = """
            SELECT sp FROM SharingBookmark b, SharingPost sp
            WHERE sp.id = b.sharingPostId
              AND b.memberId = :memberId
              AND sp.status = :status
            ORDER BY b.id DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM SharingBookmark b, SharingPost sp
            WHERE sp.id = b.sharingPostId
              AND b.memberId = :memberId
              AND sp.status = :status
            """)
    Page<SharingPost> findBookmarkedPosts(@Param("memberId") Long memberId,
                                          @Param("status") SharingPostStatus status,
                                          Pageable pageable);
}
