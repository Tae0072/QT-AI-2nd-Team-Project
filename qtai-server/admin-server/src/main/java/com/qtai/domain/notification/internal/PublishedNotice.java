package com.qtai.domain.notification.internal;

import java.time.LocalDateTime;

record PublishedNotice(
        Long id,
        String title,
        String body,
        String status,
        LocalDateTime publishedAt,
        String beforeJson
) {
}
