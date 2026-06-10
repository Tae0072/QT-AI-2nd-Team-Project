package com.qtai.domain.qtvideo.api;

import com.qtai.domain.qtvideo.api.dto.QtVideoAvailability;

public interface GetQtVideoAvailabilityUseCase {

    QtVideoAvailability getAvailability(Long qtPassageId);
}
