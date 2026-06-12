package com.qtai.domain.qt.internal;

import java.util.Arrays;
import java.util.Locale;

public enum QtPassageStatus {
    ACTIVE("active"),
    HIDDEN("hidden"),
    PENDING_REVIEW("pending_review"),
    DELETION_NOTIFIED("deletion_notified"),
    REMOVED("removed");

    private final String apiValue;

    QtPassageStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static QtPassageStatus fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.apiValue.equals(normalized)
                        || status.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported status: " + value));
    }
}
