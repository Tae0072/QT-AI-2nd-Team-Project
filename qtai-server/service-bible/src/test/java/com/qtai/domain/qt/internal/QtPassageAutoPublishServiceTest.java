package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QtPassageAutoPublishServiceTest {

    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("04:00 이후면 오늘까지를 대상으로 미게시 본문을 게시(ACTIVE)하고 게시 시각=해당 날짜 04:00 + 매핑 변경 이벤트 발행")
    void publishDue_atOrAfter0400_publishesTodayWithDatePublishTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T04:30:00Z"), ZoneOffset.UTC); // 04:30
        QtPassageAutoPublishService service =
                new QtPassageAutoPublishService(qtPassageRepository, eventPublisher, clock);

        QtPassage target = pendingPassage(LocalDate.of(2026, 6, 16));
        when(qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 16)))
                .thenReturn(List.of(target));

        int published = service.publishDue();

        assertThat(published).isEqualTo(1);
        assertThat(target.getStatus()).isEqualTo(QtPassageStatus.ACTIVE);
        assertThat(target.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 4, 0));
        verify(eventPublisher).publishEvent(any(QtPassageVerseMappingsChangedEvent.class));
    }

    @Test
    @DisplayName("04:00 이전이면 오늘 본문은 아직 미도래라 어제까지만 대상으로 조회하고 게시하지 않는다")
    void publishDue_before0400_cutoffIsYesterday() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T02:00:00Z"), ZoneOffset.UTC); // 02:00
        QtPassageAutoPublishService service =
                new QtPassageAutoPublishService(qtPassageRepository, eventPublisher, clock);

        when(qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 15)))
                .thenReturn(List.of());

        int published = service.publishDue();

        assertThat(published).isEqualTo(0);
        verify(qtPassageRepository)
                .findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 15));
        verify(eventPublisher, never()).publishEvent(any(QtPassageVerseMappingsChangedEvent.class));
    }

    private static QtPassage pendingPassage(LocalDate qtDate) {
        QtPassage passage = QtPassage.create(
                qtDate, (short) 46, (short) 9, (short) 1, (short) 5, "오늘 QT", "ref");
        passage.scheduleForAutoPublish(); // 미게시(PENDING_REVIEW)
        ReflectionTestUtils.setField(passage, "id", 100L); // 이벤트 발행에 id 필요
        return passage;
    }
}
