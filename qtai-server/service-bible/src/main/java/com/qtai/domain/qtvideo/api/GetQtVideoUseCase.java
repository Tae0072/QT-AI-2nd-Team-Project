package com.qtai.domain.qtvideo.api;

import com.qtai.domain.qtvideo.api.dto.QtVideoClipResponse;

public interface GetQtVideoUseCase {

    QtVideoClipResponse getVideo(Long qtPassageId);
}
