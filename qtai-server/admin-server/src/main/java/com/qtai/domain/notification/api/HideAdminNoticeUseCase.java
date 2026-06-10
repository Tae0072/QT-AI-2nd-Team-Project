package com.qtai.domain.notification.api;

public interface HideAdminNoticeUseCase {

    void hideNotice(Long adminUserId, Long noticeId);
}
