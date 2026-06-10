package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NoticeNotificationFanoutServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T01:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    NoticeNotificationChunkWriter chunkWriter;

    NoticeNotificationFanoutService fanoutService;

    @BeforeEach
    void setUp() {
        fanoutService = new NoticeNotificationFanoutService(chunkWriter, CLOCK);
    }

    @Test
    void fanout_deduplicatesMemberIdsAndCountsCreatedRows() {
        when(chunkWriter.writeChunk(any(), any(), any(LocalDateTime.class))).thenReturn(2);

        NoticeNotificationFanoutResult result = fanoutService.fanout(
                publishedNotice(), List.of(10L, 10L, 11L));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
    }

    @Test
    void fanout_countsFailedChunkWithoutThrowing() {
        when(chunkWriter.writeChunk(any(), any(), any(LocalDateTime.class)))
                .thenThrow(new DataIntegrityViolationException("chunk failed"));

        NoticeNotificationFanoutResult result = fanoutService.fanout(
                publishedNotice(), List.of(10L, 11L));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.createdCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(2);
    }

    private static PublishedNotice publishedNotice() {
        return new PublishedNotice(
                1L,
                "공지",
                "본문",
                "PUBLISHED",
                LocalDateTime.of(2026, 6, 10, 10, 30),
                "{\"status\":\"DRAFT\"}"
        );
    }
}
