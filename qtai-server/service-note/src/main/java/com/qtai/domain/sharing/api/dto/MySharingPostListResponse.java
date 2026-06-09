package com.qtai.domain.sharing.api.dto;

import java.util.List;

/**
 * 내 나눔 목록 응답 봉투. (04 API §1.6 표준 페이징 envelope)
 *
 * 구조는 {@link SharingPostListResponse}와 같고, content 항목 타입만 {@link MySharingPostListItem}이다.
 *
 * @param content        이번 페이지의 내 나눔 글 목록
 * @param page           현재 페이지 번호 (0부터)
 * @param size           한 페이지의 항목 수
 * @param totalElements  전체 항목 수
 * @param totalPages     전체 페이지 수
 * @param first          첫 페이지 여부
 * @param last           마지막 페이지 여부
 * @param sort           정렬 기준 (예: "publishedAt,desc")
 */
public record MySharingPostListResponse(
        List<MySharingPostListItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {}
