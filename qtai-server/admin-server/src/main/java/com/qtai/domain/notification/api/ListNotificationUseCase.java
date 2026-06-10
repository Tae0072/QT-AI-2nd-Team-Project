package com.qtai.domain.notification.api;

import com.qtai.domain.notification.api.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 알림 목록 조회 UseCase 포트.
 *
 * 본인 알림만 조회. 페이징 + 읽음 필터.
 */
public interface ListNotificationUseCase {

    Page<NotificationResponse> listMy(Long memberId, Boolean unreadOnly, Pageable pageable);

    long countUnread(Long memberId);
}
