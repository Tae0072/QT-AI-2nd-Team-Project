package com.qtai.domain.qtvideo.internal;

import com.qtai.domain.qtvideo.api.dto.QtVideoUserStatus;

import java.util.List;
import java.util.Optional;

final class QtVideoUserStatusResolver {

    static final List<QtVideoClipStatus> USER_STATUS_CANDIDATE_STATUSES = List.of(
            QtVideoClipStatus.APPROVED,
            QtVideoClipStatus.FAILED,
            QtVideoClipStatus.HIDDEN
    );

    private QtVideoUserStatusResolver() {
    }

    static Optional<QtVideoClip> chooseUserStatusClip(List<QtVideoClip> clips) {
        for (QtVideoClipStatus status : USER_STATUS_CANDIDATE_STATUSES) {
            Optional<QtVideoClip> clip = clips.stream()
                    .filter(candidate -> candidate.getStatus() == status)
                    .findFirst();
            if (clip.isPresent()) {
                return clip;
            }
        }
        return Optional.empty();
    }

    static QtVideoUserStatus toUserStatus(QtVideoClipStatus status) {
        return switch (status) {
            case APPROVED -> QtVideoUserStatus.READY;
            case FAILED -> QtVideoUserStatus.FAILED;
            case HIDDEN -> QtVideoUserStatus.DISABLED;
            case PENDING -> QtVideoUserStatus.MISSING;
        };
    }
}
