package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.QtPassageContentContext;

public interface GetQtPassageContentContextUseCase {

    QtPassageContentContext getContentContext(Long qtPassageId);
}
