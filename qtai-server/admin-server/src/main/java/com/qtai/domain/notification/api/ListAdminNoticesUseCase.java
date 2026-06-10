package com.qtai.domain.notification.api;

import com.qtai.domain.notification.api.dto.AdminNoticeListResponse;

public interface ListAdminNoticesUseCase {

    AdminNoticeListResponse listAdminNotices(int page, int size);
}
