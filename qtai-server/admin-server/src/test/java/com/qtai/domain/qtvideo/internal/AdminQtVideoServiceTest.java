package com.qtai.domain.qtvideo.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.qtai.domain.bible.internal.BibleVerseRepository;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qtvideo.api.dto.PrepareQtVideoClipResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AdminQtVideoServiceTest {

    private SourceVideoRepository sourceVideoRepository;
    private BibleVerseVideoSegmentRepository segmentRepository;
    private QtVideoClipRepository clipRepository;
    private GetQtPassageContentContextUseCase contentContextUseCase;
    private AdminQtVideoService service;

    @BeforeEach
    void setUp() {
        sourceVideoRepository = mock(SourceVideoRepository.class);
        segmentRepository = mock(BibleVerseVideoSegmentRepository.class);
        clipRepository = mock(QtVideoClipRepository.class);
        contentContextUseCase = mock(GetQtPassageContentContextUseCase.class);
        service = new AdminQtVideoService(
                sourceVideoRepository,
                segmentRepository,
                clipRepository,
                mock(BibleVerseRepository.class),
                contentContextUseCase,
                Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("UTC"))
        );
    }

    @Test
    void prepareClipCreatesApprovedSingleCutWhenAllVersesHaveActiveSourceSegments() {
        SourceVideo sourceVideo = activeSourceVideo(3L);
        List<Long> verseIds = List.of(101L, 102L);
        when(contentContextUseCase.getContentContext(10L))
                .thenReturn(new QtPassageContentContext(
                        10L,
                        LocalDate.of(2026, 6, 15),
                        "QT title",
                        verseIds,
                        true
                ));
        when(segmentRepository.findActiveSourceSegmentsByVerseIds(
                eq(verseIds),
                eq(SourceVideoStatus.ACTIVE),
                eq(SourceVideo.ACTIVE_UNIQUE_KEY)
        )).thenReturn(List.of(
                segment(101L, sourceVideo, "10.000", "20.000"),
                segment(102L, sourceVideo, "20.000", "35.500")
        ));
        when(clipRepository.findByQtPassageIdAndActiveUniqueKey(10L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(clipRepository.save(any(QtVideoClip.class))).thenAnswer(invocation -> {
            QtVideoClip clip = invocation.getArgument(0);
            ReflectionTestUtils.setField(clip, "id", 700L);
            return clip;
        });

        PrepareQtVideoClipResult result = service.prepareClip(10L);

        assertThat(result.prepared()).isTrue();
        assertThat(result.clipId()).isEqualTo(700L);
        ArgumentCaptor<QtVideoClip> clipCaptor = ArgumentCaptor.forClass(QtVideoClip.class);
        verify(clipRepository).save(clipCaptor.capture());
        QtVideoClip saved = clipCaptor.getValue();
        assertThat(saved.getQtPassageId()).isEqualTo(10L);
        assertThat(saved.getSourceVideo().getId()).isEqualTo(3L);
        assertThat(saved.getVideoUrl()).isEqualTo("https://example.com/video.mp4");
        assertThat(saved.getStartTimeSec()).isEqualByComparingTo("10.000");
        assertThat(saved.getEndTimeSec()).isEqualByComparingTo("35.500");
        assertThat(saved.getStatus()).isEqualTo(QtVideoClipStatus.APPROVED);
    }

    @Test
    void prepareClipDoesNotCreateClipWhenContextIsNotPublished() {
        when(contentContextUseCase.getContentContext(10L))
                .thenReturn(new QtPassageContentContext(
                        10L,
                        LocalDate.of(2026, 6, 15),
                        "QT title",
                        List.of(101L),
                        false
                ));

        PrepareQtVideoClipResult result = service.prepareClip(10L);

        assertThat(result.prepared()).isFalse();
        assertThat(result.clipId()).isNull();
        verify(clipRepository, never()).save(any());
    }

    private static SourceVideo activeSourceVideo(Long id) {
        SourceVideo sourceVideo = SourceVideo.active(
                (short) 46,
                "1 Corinthians",
                "https://example.com/video.mp4",
                new BigDecimal("3600.000")
        );
        ReflectionTestUtils.setField(sourceVideo, "id", id);
        return sourceVideo;
    }

    private static BibleVerseVideoSegment segment(
            Long bibleVerseId,
            SourceVideo sourceVideo,
            String startTimeSec,
            String endTimeSec
    ) {
        return BibleVerseVideoSegment.create(
                bibleVerseId,
                sourceVideo,
                new BigDecimal(startTimeSec),
                new BigDecimal(endTimeSec)
        );
    }
}
