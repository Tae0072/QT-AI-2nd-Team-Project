package com.qtai.domain.notification.internal;

final class NoticeBodyPreview {

    static final int LIST_BODY_PREVIEW_MAX_LENGTH = 80;
    static final int NOTIFICATION_BODY_COLUMN_LENGTH = 500;
    private static final int ELLIPSIS_LENGTH = 3;

    private NoticeBodyPreview() {
    }

    static String listPreview(String value) {
        return preview(value, LIST_BODY_PREVIEW_MAX_LENGTH);
    }

    static String notificationPreview(String value) {
        return preview(value, NOTIFICATION_BODY_COLUMN_LENGTH - ELLIPSIS_LENGTH);
    }

    static String preview(String value, int maxLengthBeforeEllipsis) {
        if (value == null || value.length() <= maxLengthBeforeEllipsis) {
            return value;
        }
        return value.substring(0, maxLengthBeforeEllipsis) + "...";
    }
}
