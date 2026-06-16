package com.qtai.domain.qtvideo.internal;

import com.qtai.domain.qtvideo.api.GetQtVideoAvailabilityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class QtVideoAvailabilityService implements GetQtVideoAvailabilityUseCase {

    private final QtVideoClipRepository qtVideoClipRepository;

    @Override
    public boolean hasReadyVideo(Long qtPassageId) {
        return qtPassageId != null
                && qtPassageId > 0
                && qtVideoClipRepository.existsByQtPassageIdAndStatus(qtPassageId, QtVideoClipStatus.APPROVED);
    }
}
