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
    @DisplayName("04:00 이후면 오늘까지 자동수집 미게시 본문을 QT 날짜 04:00으로 게시하고 매핑 이벤트를 발행한다")
    void publishDue_atOrAfter0400_publishesTodayWithDatePublishTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T04:30:00Z"), ZoneOffset.UTC);
        QtPassageAutoPublishService service =
                new QtPassageAutoPublishService(qtPassageRepository, eventPublisher, clock);

        QtPassage target = pendingCollectedPassage(LocalDate.of(2026, 6, 16), 100L);
        when(qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 16)))
                .thenReturn(List.of(target));

        int published = service.publishDue();

        assertThat(published).isEqualTo(1);
        assertThat(target.getStatus()).isEqualTo(QtPassageStatus.ACTIVE);
        assertThat(target.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 4, 0));
        verify(eventPublisher).publishEvent(any(QtPassageVerseMappingsChangedEvent.class));
    }

    @Test
    @DisplayName("04:00 이전이면 어제까지를 cutoff로 조회하고 이벤트를 발행하지 않는다")
    void publishDue_before0400_cutoffIsYesterday() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T02:00:00Z"), ZoneOffset.UTC);
        QtPassageAutoPublishService service =
                new QtPassageAutoPublishService(qtPassageRepository, eventPublisher, clock);

        when(qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 15)))
                .thenReturn(List.of());

        int published = service.publishDue();

        assertThat(published).isZero();
        verify(qtPassageRepository)
                .findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 15));
        verify(eventPublisher, never()).publishEvent(any(QtPassageVerseMappingsChangedEvent.class));
    }

    @Test
    @DisplayName("stale target이 이미 게시 상태면 중복 이벤트 없이 skip한다")
    void publishDue_whenTargetAlreadyActive_skipsDuplicatePublishEvent() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T04:30:00Z"), ZoneOffset.UTC);
        QtPassageAutoPublishService service =
                new QtPassageAutoPublishService(qtPassageRepository, eventPublisher, clock);

        QtPassage staleTarget = pendingCollectedPassage(LocalDate.of(2026, 6, 16), 101L);
        staleTarget.publish(LocalDateTime.of(2026, 6, 16, 4, 0));
        when(qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 16)))
                .thenReturn(List.of(staleTarget));

        int published = service.publishDue();

        assertThat(published).isZero();
        verify(eventPublisher, never()).publishEvent(any(QtPassageVerseMappingsChangedEvent.class));
    }

    @Test
    @DisplayName("stale target의 collectedAt이 없으면 수동 등록으로 보고 자동게시하지 않는다")
    void publishDue_whenTargetHasNoCollectedAt_skipsAutoPublish() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-16T04:30:00Z"), ZoneOffset.UTC);
        QtPassageAutoPublishService service =
                new QtPassageAutoPublishService(qtPassageRepository, eventPublisher, clock);

        QtPassage manualTarget = QtPassage.create(
                LocalDate.of(2026, 6, 16), (short) 46, (short) 9, (short) 1, (short) 5, "manual", "ref");
        manualTarget.scheduleForAutoPublish();
        ReflectionTestUtils.setField(manualTarget, "id", 102L);
        when(qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, LocalDate.of(2026, 6, 16)))
                .thenReturn(List.of(manualTarget));

        int published = service.publishDue();

        assertThat(published).isZero();
        assertThat(manualTarget.getStatus()).isEqualTo(QtPassageStatus.PENDING_REVIEW);
        verify(eventPublisher, never()).publishEvent(any(QtPassageVerseMappingsChangedEvent.class));
    }

    private static QtPassage pendingCollectedPassage(LocalDate qtDate, Long id) {
        QtPassage passage = QtPassage.create(
                qtDate, (short) 46, (short) 9, (short) 1, (short) 5, "today QT", "ref");
        passage.scheduleForAutoPublish();
        passage.recordCollected(qtDate.atTime(0, 2), null);
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }
}
