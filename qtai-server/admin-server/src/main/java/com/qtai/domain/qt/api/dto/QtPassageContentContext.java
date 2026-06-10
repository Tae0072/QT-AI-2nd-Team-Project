package com.qtai.domain.qt.api.dto;

import java.time.LocalDate;
import java.util.List;

public record QtPassageContentContext(
        Long qtPassageId,
        LocalDate qtDate,
        String title,
        List<Long> verseIds,
        boolean published
) {
}
