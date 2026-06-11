package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link DevNotificationSeedRunner} 단위 테스트 — 멱등(기존재 시 미삽입)과 시드 구성(미읽음 2 + 읽음 3)을 검증한다.
 */
class DevNotificationSeedRunnerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-11T09:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("이미 시드된 경우(eventKey 존재) 아무것도 삽입하지 않는다")
    void run_멱등() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.existsByMemberIdAndEventKey(anyLong(), anyString())).thenReturn(true);
        var runner = new DevNotificationSeedRunner(repository, FIXED_CLOCK, 1L);

        runner.run(null);

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("최초 기동 시 5건(미읽음 2 + 읽음 3)을 시드 회원에게 삽입한다")
    void run_최초_시드() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.existsByMemberIdAndEventKey(anyLong(), anyString())).thenReturn(false);
        var runner = new DevNotificationSeedRunner(repository, FIXED_CLOCK, 7L);

        runner.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<Notification> seeded = captor.getValue();

        assertThat(seeded).hasSize(5);
        assertThat(seeded).allSatisfy(n -> {
            assertThat(n.getMemberId()).isEqualTo(7L);
            assertThat(n.getEventKey()).startsWith(DevNotificationSeedRunner.SEED_KEY_PREFIX);
            assertThat(n.getTitle()).isNotBlank();
            assertThat(n.getCreatedAt()).isNotNull();
        });
        assertThat(seeded.stream().filter(Notification::isRead)).hasSize(3);
        assertThat(seeded.stream().filter(n -> !n.isRead())).hasSize(2);
    }
}
