package com.qtai.domain.qtvideo.internal;

import com.qtai.domain.qtvideo.api.GetQtVideoAvailabilityUseCase;
import com.qtai.domain.qtvideo.api.dto.QtVideoAvailability;
import com.qtai.domain.qtvideo.api.dto.QtVideoUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtVideoAvailabilityService implements GetQtVideoAvailabilityUseCase {

    private final QtVideoClipRepository qtVideoClipRepository;

    @Override
    public QtVideoAvailability getAvailability(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            return new QtVideoAvailability(QtVideoUserStatus.MISSING.name());
        }
        boolean exists = qtVideoClipRepository
                .findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                        qtPassageId, QtVideoClipStatus.APPROVED)
                .isPresent();
        return new QtVideoAvailability(exists
                ? QtVideoUserStatus.READY.name()
                : QtVideoUserStatus.MISSING.name());
    }
}
