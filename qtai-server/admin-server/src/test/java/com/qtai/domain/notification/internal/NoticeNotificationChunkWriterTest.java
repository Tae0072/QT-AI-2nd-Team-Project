package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeNotificationChunkWriterTest {

    @Mock
    NotificationRepository notificationRepository;

    NoticeNotificationChunkWriter chunkWriter;

    @BeforeEach
    void setUp() {
        chunkWriter = new NoticeNotificationChunkWriter(notificationRepository);
    }

    @Test
    void writeChunk_deduplicatesMemberIdsInsideChunk() {
        when(notificationRepository.findEventKeysIn(any(Collection.class))).thenReturn(List.of());
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int createdCount = chunkWriter.writeChunk(publishedNotice("본문"), List.of(10L, 10L, 11L), now());

        assertThat(createdCount).isEqualTo(2);
        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void writeChunk_keepsNotificationBodyWithinColumnLength() {
        when(notificationRepository.findEventKeysIn(any(Collection.class))).thenReturn(List.of());
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String longBody = "a".repeat(600);

        chunkWriter.writeChunk(publishedNotice(longBody), List.of(10L), now());

        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        Notification notification = captor.getValue().iterator().next();
        assertThat(notification.getBody()).hasSize(500);
    }

    @Test
    void writeChunk_keepsKoreanNotificationBodyWithinColumnLength() {
        when(notificationRepository.findEventKeysIn(any(Collection.class))).thenReturn(List.of());
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String longBody = "가".repeat(600);

        chunkWriter.writeChunk(publishedNotice(longBody), List.of(10L), now());

        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        Notification notification = captor.getValue().iterator().next();
        assertThat(notification.getBody()).hasSize(500);
        assertThat(notification.getBody()).endsWith("...");
    }

    private static PublishedNotice publishedNotice(String body) {
        return new PublishedNotice(
                1L,
                "공지",
                body,
                "PUBLISHED",
                LocalDateTime.of(2026, 6, 10, 10, 30),
                "{\"status\":\"DRAFT\"}"
        );
    }

    private static LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 10, 10, 30);
    }
}
