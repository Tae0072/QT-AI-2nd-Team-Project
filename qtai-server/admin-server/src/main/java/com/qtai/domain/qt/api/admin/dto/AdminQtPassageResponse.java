package com.qtai.domain.qt.api.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminQtPassageResponse(
        Long id,
        LocalDate qtDate,
        Short bookId,
        Short chapter,
        Short endChapter,
        Short startVerse,
        Short endVerse,
        String title,
        String mainVerseRef,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime hiddenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
