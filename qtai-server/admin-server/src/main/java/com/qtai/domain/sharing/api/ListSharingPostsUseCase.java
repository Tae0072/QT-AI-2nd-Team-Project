package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import org.springframework.data.domain.Pageable;

/**
 * 나눔 피드 목록 조회 UseCase 포트.
 *
 * GET /api/v1/sharing-posts?category=&q=&page=&size=&sort=
 *
 * 정책:
 * - PUBLISHED 상태 게시글만 반환 (HIDDEN/DELETED 제외)
 * - category(동등) / q(제목·본문 검색) 필터, 기본 정렬 publishedAt,desc
 * - likedByMe는 조회자(memberId) 기준으로 계산
 */
public interface ListSharingPostsUseCase {

    SharingPostListResponse list(Long memberId, String category, String q, Pageable pageable);
}
