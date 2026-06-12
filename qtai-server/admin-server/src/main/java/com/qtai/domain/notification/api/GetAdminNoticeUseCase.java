package com.qtai.domain.notification.api;

import com.qtai.domain.notification.api.dto.AdminNoticeDetailResponse;

public interface GetAdminNoticeUseCase {

    AdminNoticeDetailResponse getAdminNotice(Long noticeId);
}
