package com.qtai.domain.qt.api.admin;

import com.qtai.domain.qt.api.admin.dto.AdminQtPassageResponse;

public interface HideAdminQtPassageUseCase {

    AdminQtPassageResponse hide(Long adminId, Long qtPassageId);
}
