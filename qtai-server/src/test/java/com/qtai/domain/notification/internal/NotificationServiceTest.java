package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.notification.api.dto.NotificationResponse;
import com.qtai.domain.notification.api.dto.NotificationSendRequest;

/**
 * NotificationService 단위 테스트.
 */
class NotificationServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-26T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = Mockito.mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository, FIXED_CLOCK);
    }

    // ── send ──

    @Test
    void send_알림_저장_성공() {
        NotificationSendRequest request = new NotificationSendRequest(
                1L, "LIKE", "좋아요 알림", "회원이 좋아요를 눌렀습니다.",
                null, "SHARING_POST", 10L, "LIKE_1_10"
        );
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo("LIKE");
        assertThat(saved.getTitle()).isEqualTo("좋아요 알림");
        assertThat(saved.getEventKey()).isEqualTo("LIKE_1_10");
    }

    @Test
    void send_memberId_null_예외_발생() {
        NotificationSendRequest request = new NotificationSendRequest(
                null, "LIKE", "알림", null, null, null, null, null
        );

        assertThatThrownBy(() -> notificationService.send(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void send_eventKey_중복_멱등_처리() {
        NotificationSendRequest request = new NotificationSendRequest(
                1L, "LIKE", "좋아요 알림", null,
                null, "SHARING_POST", 10L, "LIKE_1_10"
        );
        when(notificationRepository.existsByMemberIdAndEventKey(1L, "LIKE_1_10"))
                .thenReturn(true);

        notificationService.send(request);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void send_TOCTOU_UK위반_멱등_처리() {
        NotificationSendRequest request = new NotificationSendRequest(
                1L, "LIKE", "좋아요 알림", null,
                null, "SHARING_POST", 10L, "LIKE_1_10"
        );
        when(notificationRepository.existsByMemberIdAndEventKey(1L, "LIKE_1_10"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("UK violation"));

        // 멱등 처리 — 예외 없이 정상 종료
        notificationService.send(request);

        verify(notificationRepository, times(1)).save(any());
    }

    // ── countUnread ──

    @Test
    void countUnread_미읽음_수_반환() {
        when(notificationRepository.countByMemberIdAndReadAtIsNull(1L)).thenReturn(5L);

        long count = notificationService.countUnread(1L);

        assertThat(count).isEqualTo(5);
    }

    // ── listMy ──

    @Test
    void listMy_전체_알림_조회() {
        Notification n1 = createNotification(1L, 1L);
        Notification n2 = createNotification(2L, 1L);
        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findByMemberIdOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(n1, n2)));

        Page<NotificationResponse> result = notificationService.listMy(1L, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).type()).isEqualTo("LIKE");
    }

    @Test
    void listMy_unreadOnly_미읽음만_조회() {
        Notification unread = createNotification(1L, 1L);
        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findByMemberIdAndReadAtIsNullOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(unread)));

        Page<NotificationResponse> result = notificationService.listMy(1L, true, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listMy_unreadOnly_false_전체_조회() {
        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findByMemberIdOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<NotificationResponse> result = notificationService.listMy(1L, false, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(notificationRepository).findByMemberIdOrderByCreatedAtDesc(1L, pageable);
    }

    // ── markAsRead ──

    @Test
    void markAsRead_성공() {
        Notification notification = createNotification(10L, 1L);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 10L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_알림_없음() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void markAsRead_타인_알림_접근_불가() {
        Notification notification = createNotification(10L, 2L); // 다른 회원 소유
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED);
    }

    // ── markAllAsRead ──

    @Test
    void markAllAsRead_업데이트_행_수_반환() {
        LocalDateTime expectedTime = LocalDateTime.now(FIXED_CLOCK);
        when(notificationRepository.markAllAsRead(1L, expectedTime)).thenReturn(3);

        int count = notificationService.markAllAsRead(1L);

        assertThat(count).isEqualTo(3);
    }

    // ── helper ──

    private Notification createNotification(Long id, Long memberId) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .type("LIKE")
                .title("테스트 알림")
                .createdAt(LocalDateTime.now(FIXED_CLOCK))
                .build();
        try {
            var idField = notification.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notification, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return notification;
    }
}
