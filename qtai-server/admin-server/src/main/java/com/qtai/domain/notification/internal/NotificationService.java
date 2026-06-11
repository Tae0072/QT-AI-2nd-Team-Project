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
    // 수신자 존재·활성 검증 + 수신 설정 확인용 (타 도메인은 api 포트로만, CLAUDE.md §4)
    private final com.qtai.domain.member.api.GetMemberUseCase getMemberUseCase;
    private final com.qtai.domain.member.api.GetSettingsUseCase getSettingsUseCase;
    private final Clock clock;

    /** 사용자 수신 설정(OFF)을 무시하고 항상 보내는 시스템·법적 성격의 알림 유형. */
    private static final java.util.Set<NotificationType> ALWAYS_SEND_TYPES =
            java.util.EnumSet.of(NotificationType.REPORT_RESULT, NotificationType.NOTICE);

    // ── SendNotificationUseCase ──

    @Override
    @Transactional
    public void send(NotificationSendRequest request) {
        if (request.memberId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "알림 수신 memberId 는 null 일 수 없습니다.");
        }

        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(request.type());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 알림 유형: " + request.type());
        }

        // 수신자 존재·활성 검증 — 탈퇴/없는 회원에게는 알림을 만들지 않는다(조용히 skip).
        // (members FK 위반으로 500 나던 경로를 사전 차단)
        try {
            getMemberUseCase.getMemberPublic(request.memberId());
        } catch (BusinessException e) {
            log.debug("알림 수신자 비활성/없음 — skip: memberId={}, type={}",
                    request.memberId(), notificationType);
            return;
        }

        // 사용자 수신 설정(OFF) 존중 — 단 시스템·법적 알림(REPORT_RESULT/NOTICE)은 항상 발송.
        if (!ALWAYS_SEND_TYPES.contains(notificationType) && !isNotificationEnabled(request.memberId())) {
            log.debug("수신 설정 OFF — skip: memberId={}, type={}", request.memberId(), notificationType);
            return;
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
                .type(notificationType)
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

    /** 수신자의 알림 수신 설정. 조회 실패 시 보수적으로 true(발송 허용). */
    private boolean isNotificationEnabled(Long memberId) {
        try {
            Boolean enabled = getSettingsUseCase.getSettings(memberId).notificationEnabled();
            return enabled == null || enabled;
        } catch (RuntimeException e) {
            log.debug("수신 설정 조회 실패 — 기본 허용: memberId={}", memberId);
            return true;
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
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
