package com.qtai.domain.qt.api.admin;

import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;

public interface PublishAdminQtPassageUseCase {

    AdminQtPassageResponse publish(Long adminId, Long qtPassageId);
}
