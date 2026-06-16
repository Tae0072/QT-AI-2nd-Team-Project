package com.qtai.domain.music.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

enum MusicTrackStatus {
    ACTIVE,
    HIDDEN;

    static MusicTrackStatus fromEnabled(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? ACTIVE : HIDDEN;
    }

    static Boolean enabledFilter(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return switch (MusicTrackStatus.valueOf(rawStatus)) {
                case ACTIVE -> Boolean.TRUE;
                case HIDDEN -> Boolean.FALSE;
            };
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 배경음악 상태입니다: " + rawStatus);
        }
    }
}
