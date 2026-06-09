package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.GetSettingsUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link NotificationService} 단위 테스트 — 소유권 검증/미읽음 카운트(Mockito, DB 미사용).
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private GetMemberUseCase getMemberUseCase;
    @Mock
    private GetSettingsUseCase getSettingsUseCase;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @InjectMocks
    private NotificationService notificationService;

    private Notification ownedBy(long memberId) {
        return Notification.builder()
                .memberId(memberId)
                .type(NotificationType.NOTICE)
                .title("t")
                .body("b")
                .createdAt(LocalDateTime.now(clock))
                .build();
    }

    @Test
    void markAsRead_없는알림이면_NOTIFICATION_NOT_FOUND() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 10L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    void markAsRead_타인의알림이면_ACCESS_DENIED() {
        // 주의: @Spy clock 호출이 when(...).thenReturn(...) 인자 안에서 일어나면 Mockito가
        // 미완료 stubbing으로 오인한다. 알림 객체는 stubbing 밖에서 미리 만든다.
        Notification othersNotification = ownedBy(1L);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(othersNotification));

        assertThatThrownBy(() -> notificationService.markAsRead(2L, 10L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED));
    }

    @Test
    void countUnread_리포지토리값을_그대로_반환한다() {
        when(notificationRepository.countByMemberIdAndReadAtIsNull(1L)).thenReturn(3L);

        assertThat(notificationService.countUnread(1L)).isEqualTo(3L);
    }
}
