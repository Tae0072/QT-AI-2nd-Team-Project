package com.qtai.domain.notification.api;

import com.qtai.domain.notification.api.dto.AdminNoticePublishResponse;

public interface PublishAdminNoticeUseCase {

    AdminNoticePublishResponse publishNotice(Long adminUserId, Long noticeId);
}
