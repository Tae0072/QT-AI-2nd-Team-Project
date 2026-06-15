package com.qtai.domain.ai.api.admin.prompt.dto;

import java.util.List;

public record AiPromptVersionListResponse(
        List<AiPromptVersionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
}
