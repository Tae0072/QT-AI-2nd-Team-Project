package com.qtai.domain.qt.api.admin.dto;

import java.time.LocalDate;

public record AdminQtPassageCommand(
        Long adminId,
        LocalDate qtDate,
        Short bookId,
        Short chapter,
        Short endChapter,
        Short startVerse,
        Short endVerse,
        String title,
        String mainVerseRef
) {
}
