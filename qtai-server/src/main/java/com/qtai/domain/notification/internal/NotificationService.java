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

import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 알림 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * <p>발송 실패 정책: 푸시 전송 실패는 swallow + 로그만 (비즈니스 흐름 보호).
 * <p>도메인 경계 정책: Entity → DTO 변환은 이 서비스에서 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService implements
        SendNotificationUseCase, ListNotificationUseCase, MarkAsReadUseCase {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    // ── SendNotificationUseCase ──

    @Override
    @Transactional
    public void send(NotificationSendRequest request) {
        // TODO: 수신 회원 존재·활성 상태 검증 — GetMemberUseCase 연동 후 WITHDRAWN 회원 차단 예정.
        // 현재는 member 도메인 의존 없이 저장 허용 (MVP 단계).
        if (request.memberId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "알림 수신 memberId 는 null 일 수 없습니다.");
        }

        // eventKey 중복 시 멱등 처리 — 동일 이벤트 알림을 두 번 저장하지 않는다.
        if (request.eventKey() != null &&
                notificationRepository.existsByMemberIdAndEventKey(
                        request.memberId(), request.eventKey())) {
            log.debug("중복 eventKey 무시 (멱등): memberId={}, eventKey={}",
                    request.memberId(), request.eventKey());
            return;
        }

        Notification notification = Notification.builder()
                .memberId(request.memberId())
                .type(request.type())
                .title(request.title())
                .body(request.body())
                .noticeId(request.noticeId())
                .linkType(request.linkType())
                .linkId(request.linkId())
                .eventKey(request.eventKey())
                .createdAt(LocalDateTime.now(clock))
                .build();

        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            // TOCTOU: existsByMemberIdAndEventKey 이후 동시 INSERT → UK 위반 시 멱등 처리
            log.debug("알림 저장 UNIQUE 위반 (멱등 처리): memberId={}, eventKey={}",
                    request.memberId(), request.eventKey());
            return;
        }
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
        return page.map(this::toResponse);
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

        notification.markAsRead(clock);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long memberId) {
        return notificationRepository.markAllAsRead(memberId, LocalDateTime.now(clock));
    }

    // ── private ──

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getLinkType(),
                n.getLinkId(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
