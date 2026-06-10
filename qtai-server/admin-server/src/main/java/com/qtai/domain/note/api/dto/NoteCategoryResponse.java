package com.qtai.domain.note.api.dto;

import java.util.List;

public record NoteCategoryResponse(
        List<NoteCategoryItem> categories
) {
}
