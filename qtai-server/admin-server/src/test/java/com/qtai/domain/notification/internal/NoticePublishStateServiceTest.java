package com.qtai.domain.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticePublishStateServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T01:30:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    NoticeRepository noticeRepository;

    NoticePublishStateService service;

    @BeforeEach
    void setUp() {
        service = new NoticePublishStateService(
                noticeRepository,
                new NoticeAuditSnapshotFactory(new ObjectMapper()),
                CLOCK
        );
    }

    @Test
    void publish_usesPessimisticLockLookupAndKstClock() {
        Notice notice = Notice.draft(100L, "공지", "본문");
        ReflectionTestUtils.setField(notice, "id", 1L);
        when(noticeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(notice));

        PublishedNotice publishedNotice = service.publish(1L);

        verify(noticeRepository).findByIdForUpdate(1L);
        assertThat(publishedNotice.status()).isEqualTo("PUBLISHED");
        assertThat(publishedNotice.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 10, 10, 30));
    }
}
