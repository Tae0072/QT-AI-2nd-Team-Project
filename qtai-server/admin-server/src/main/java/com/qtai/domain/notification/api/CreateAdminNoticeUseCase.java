package com.qtai.domain.notification.api;

import com.qtai.domain.notification.api.dto.AdminNoticeCommand;
import com.qtai.domain.notification.api.dto.AdminNoticeDetailResponse;

public interface CreateAdminNoticeUseCase {

    AdminNoticeDetailResponse createNotice(AdminNoticeCommand command);
}
