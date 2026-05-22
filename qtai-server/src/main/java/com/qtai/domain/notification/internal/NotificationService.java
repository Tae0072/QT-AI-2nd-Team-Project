package com.qtai.domain.notification.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.notification.api.ListNotificationUseCase;
import com.qtai.domain.notification.api.MarkAsReadUseCase;
import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.notification.api.dto.NotificationResponse;
import com.qtai.domain.notification.api.dto.NotificationSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * 발송 실패 정책: 푸시 전송 실패는 swallow + 로그만 (비즈니스 흐름 보호).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService implements
        SendNotificationUseCase, ListNotificationUseCase, MarkAsReadUseCase {

    private final NotificationRepository notificationRepository;

    // ── SendNotificationUseCase ──

    @Override
    @Transactional
    public void send(NotificationSendRequest request) {
        Notification notification = Notification.builder()
                .memberId(request.memberId())
                .type(request.type())
                .title(request.title())
                .body(request.body())
                .noticeId(request.noticeId())
                .linkType(request.linkType())
                .linkId(request.linkId())
                .eventKey(request.eventKey())
                .build();

        notificationRepository.save(notification);
        // FCM 푸시는 MVP 이후 추가 예정
        log.debug("알림 저장 완료: memberId={}, type={}", request.memberId(), request.type());
    }

    // ── ListNotificationUseCase ──

    @Override
    public Page<NotificationResponse> listMy(Long memberId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByMemberIdAndReadAtIsNullOrderByCreatedAtDesc(
                    memberId, pageable);
        } else {
            page = notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        }
        return page.map(NotificationResponse::from);
    }

    @Override
    public long countUnread(Long memberId) {
        return notificationRepository.countByMemberIdAndReadAtIsNull(memberId);
    }

    // ── MarkAsReadUseCase ──

    @Override
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
    }

    @Override
    @Transactional
    public int markAllAsRead(Long memberId) {
        return notificationRepository.markAllAsRead(memberId);
    }
}
