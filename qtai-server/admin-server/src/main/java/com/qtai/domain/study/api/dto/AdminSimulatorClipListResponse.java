package com.qtai.domain.study.api.dto;

import java.util.List;

/** 관리자 시뮬레이터 클립 목록 응답(서버 페이지네이션) (F-06/F-12). */
public record AdminSimulatorClipListResponse(
        List<AdminSimulatorClipListItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
