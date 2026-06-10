package com.qtai.domain.notification.api.dto;

public record AdminNoticeCommand(
        Long adminUserId,
        String title,
        String body,
        String status
) {
}
