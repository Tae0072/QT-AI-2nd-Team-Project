package com.qtai.domain.sharing.api.dto;

/**
 * 나눔 게시글 저장(북마크) 토글 응답.
 *
 * <p>저장 직후의 최신 상태를 돌려줘 클라이언트가 재조회 없이 UI(저장 아이콘)를 갱신할 수 있게 한다.
 *
 * @param bookmarked 요청자의 저장 여부 (저장 직후 true)
 */
public record BookmarkResponse(
        boolean bookmarked
) {}
