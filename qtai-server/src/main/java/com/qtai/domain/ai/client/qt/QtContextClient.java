package com.qtai.domain.ai.client.qt;

import com.qtai.domain.ai.client.qt.dto.QtContextResult;

public interface QtContextClient {

    QtContextResult getQtContext(Long viewerId, Long qtPassageId);
}
