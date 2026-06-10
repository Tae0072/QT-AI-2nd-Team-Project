package com.qtai.domain.qt.api.admin.dto;

import java.time.LocalDate;

public record ListAdminQtPassagesQuery(
        String status,
        LocalDate from,
        LocalDate to,
        String q,
        int page,
        int size
) {
}
