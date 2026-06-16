package com.qtai.domain.qtvideo.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qtvideo.api.GetQtVideoUseCase;
import com.qtai.domain.qtvideo.api.dto.QtVideoClipResponse;
import com.qtai.domain.qtvideo.api.dto.QtVideoUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtVideoService implements GetQtVideoUseCase {

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final QtVideoClipRepository qtVideoClipRepository;

    @Override
    public QtVideoClipResponse getVideo(Long qtPassageId) {
        validateQtPassageId(qtPassageId);
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        if (!context.published()) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        var candidates = qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                qtPassageId,
                QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES);
        return QtVideoUserStatusResolver.chooseUserStatusClip(candidates)
                .map(this::toResponse)
                .orElseGet(() -> QtVideoClipResponse.missing(qtPassageId));
    }

    private static void validateQtPassageId(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private QtVideoClipResponse toResponse(QtVideoClip clip) {
        QtVideoUserStatus status = QtVideoUserStatusResolver.toUserStatus(clip.getStatus());
        if (status != QtVideoUserStatus.READY) {
            return QtVideoClipResponse.unavailable(
                    clip.getQtPassageId(),
                    status,
                    clip.getStatus().name()
            );
        }
        return new QtVideoClipResponse(
                status.name(),
                clip.getId(),
                clip.getQtPassageId(),
                clip.getTitle(),
                clip.getVideoUrl(),
                clip.getSourceVideo().getId(),
                clip.getStartTimeSec(),
                clip.getEndTimeSec(),
                clip.getCompositionType().name(),
                clip.getStatus().name()
        );
    }
}
