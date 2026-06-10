package com.qtai.domain.qt.api.admin;

import com.qtai.domain.qt.api.admin.dto.AdminQtPassageCommand;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;

public interface UpdateAdminQtPassageUseCase {

    AdminQtPassageResponse update(Long qtPassageId, AdminQtPassageCommand command);
}
