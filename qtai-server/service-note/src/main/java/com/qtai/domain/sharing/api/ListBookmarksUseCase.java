package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import org.springframework.data.domain.Pageable;

/**
 * 내 저장(북마크) 목록 조회 UseCase 포트.
 *
 * GET /api/v1/me/bookmarks — 내가 저장한 나눔 글을 최근 저장순으로 반환한다.
 *
 * 응답은 피드 목록과 동일한 {@link SharingPostListResponse}를 재사용한다(같은 카드 렌더링).
 * 저장한 글이 이후 숨김·삭제됐으면 목록에서 제외된다(PUBLISHED만).
 */
public interface ListBookmarksUseCase {

    SharingPostListResponse listBookmarks(Long memberId, Pageable pageable);
}
