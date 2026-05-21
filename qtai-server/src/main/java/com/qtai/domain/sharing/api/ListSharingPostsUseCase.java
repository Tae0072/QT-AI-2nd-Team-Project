package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.SharingPostResponse;

/**
 * 나눔 피드 목록 조회 UseCase 포트.
 *
 * GET /api/v1/sharing-posts?page=&size=
 *
 * 정책:
 * - PUBLISHED 상태 게시글만 반환 (DELETED 제외)
 * - 최신순 페이지네이션
 */
public interface ListSharingPostsUseCase {

    // TODO: Page<SharingPostResponse> list(Pageable pageable);
}
