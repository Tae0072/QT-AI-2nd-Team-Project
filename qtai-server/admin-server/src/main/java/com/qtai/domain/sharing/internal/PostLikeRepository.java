package com.qtai.domain.sharing.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

  /** 회원이 좋아요한 기록을 누른 시각 최신순으로 — 관리자 회원 상세 목록용. */
  Page<PostLike> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

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

  /** 이미 좋아요를 눌렀는지. like 중복(409) 사전 차단용. */
  boolean existsBySharingPostIdAndMemberId(Long sharingPostId, Long memberId);

  /** 글의 실제 좋아요 수. likeCount COUNT 재계산용. */
  long countBySharingPostId(Long sharingPostId);

  /** 좋아요 취소. 행이 없어도 0건 삭제로 끝나 멱등(예외 없음). */
  /** 해당 post를 memberId가 누른 좋아요 행을 지운다 */
  void deleteBySharingPostIdAndMemberId(Long sharingPostId, Long memberId);
}
