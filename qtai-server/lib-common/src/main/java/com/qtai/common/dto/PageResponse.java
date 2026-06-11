package com.qtai.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 표준 페이징 응답 envelope.
 *
 * <p>컨트롤러가 Spring Data {@link Page}를 그대로 직렬화하면 {@code pageable}, {@code sort}
 * 등 내부 구조가 클라이언트 계약으로 새어 나가 안정적이지 않다(JSON 스키마가 Spring 버전에
 * 묶임). 이 envelope으로 필요한 메타데이터만 고정 노출한다.
 *
 * <ul>
 *   <li>{@code content} — 현재 페이지 항목</li>
 *   <li>{@code page} — 0-기반 페이지 번호</li>
 *   <li>{@code size} — 페이지 크기</li>
 *   <li>{@code totalElements} — 전체 항목 수</li>
 *   <li>{@code totalPages} — 전체 페이지 수</li>
 *   <li>{@code first}/{@code last} — 처음/마지막 페이지 여부</li>
 * </ul>
 *
 * @param <T> 항목 타입
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    /** Spring Data {@link Page}를 표준 envelope으로 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
