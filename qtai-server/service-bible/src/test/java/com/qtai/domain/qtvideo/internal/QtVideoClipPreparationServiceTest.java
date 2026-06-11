package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class QtVideoClipPreparationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-11T00:05:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    @Mock private BibleVerseVideoSegmentRepository bibleVerseVideoSegmentRepository;
    @Mock private QtVideoClipRepository qtVideoClipRepository;

    private QtVideoClipPreparationService service;

    @BeforeEach
    void setUp() {
        service = new QtVideoClipPreparationService(
                getQtPassageContentContextUseCase,
                bibleVerseVideoSegmentRepository,
                qtVideoClipRepository,
                FIXED_CLOCK
        );
    }

    @Test
    @DisplayName("Creates an APPROVED clip when QT verse mappings and timecodes are complete")
    void prepare_createsApprovedClip() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, List.of(100L, 101L, 102L), true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(
                1L,
                (short) 46,
                "https://cdn.example.com/videos/corinthians_full.mp4"
        );
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(6L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.HIDDEN))
                .thenReturn(false);
        when(bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(100L, 101L, 102L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(List.of(
                        TestEntityFactory.bibleVerseVideoSegment(100L, sourceVideo, "10.000", "20.000"),
                        TestEntityFactory.bibleVerseVideoSegment(101L, sourceVideo, "20.000", "30.000"),
                        TestEntityFactory.bibleVerseVideoSegment(102L, sourceVideo, "30.000", "40.000")
                ));
        when(qtVideoClipRepository.save(any(QtVideoClip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.prepare(6L));

        ArgumentCaptor<QtVideoClip> captor = ArgumentCaptor.forClass(QtVideoClip.class);
        verify(qtVideoClipRepository).save(captor.capture());
        QtVideoClip saved = captor.getValue();
        assertEquals(6L, saved.getQtPassageId());
        assertEquals("QT video 2026-06-11", saved.getTitle());
        assertEquals(sourceVideo, saved.getSourceVideo());
        assertEquals("https://cdn.example.com/videos/corinthians_full.mp4", saved.getVideoUrl());
        assertEquals(new BigDecimal("10.000"), saved.getStartTimeSec());
        assertEquals(new BigDecimal("40.000"), saved.getEndTimeSec());
        assertEquals(QtVideoClipStatus.APPROVED, saved.getStatus());
        assertEquals(QtVideoClip.ACTIVE_UNIQUE_KEY, saved.getActiveUniqueKey());
    }

    @Test
    @DisplayName("Updates the existing active clip instead of creating a duplicate row")
    void prepare_updatesExistingActiveClip() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, List.of(100L, 101L), true));
        SourceVideo oldSourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/old.mp4");
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(2L, (short) 46, "https://cdn.example.com/new.mp4");
        QtVideoClip existing = TestEntityFactory.qtVideoClip(10L, 6L, oldSourceVideo, "https://cdn.example.com/old.mp4");
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(6L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.of(existing));
        when(bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(100L, 101L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(List.of(
                        TestEntityFactory.bibleVerseVideoSegment(100L, sourceVideo, "50.000", "60.000"),
                        TestEntityFactory.bibleVerseVideoSegment(101L, sourceVideo, "60.000", "70.000")
                ));

        assertTrue(service.prepare(6L));

        verify(qtVideoClipRepository).save(existing);
        assertEquals(sourceVideo, existing.getSourceVideo());
        assertEquals("https://cdn.example.com/new.mp4", existing.getVideoUrl());
        assertEquals(new BigDecimal("50.000"), existing.getStartTimeSec());
        assertEquals(new BigDecimal("70.000"), existing.getEndTimeSec());
        assertEquals(QtVideoClipStatus.APPROVED, existing.getStatus());
    }

    @Test
    @DisplayName("Prepares a clip for a non-today QT passage by passage id")
    void prepare_supportsNonTodayPassage() {
        when(getQtPassageContentContextUseCase.getContentContext(4L))
                .thenReturn(new QtPassageContentContext(
                        4L,
                        LocalDate.of(2026, 6, 9),
                        "past QT",
                        List.of(200L, 201L),
                        true
                ));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(
                1L,
                (short) 46,
                "https://cdn.example.com/videos/corinthians_full.mp4"
        );
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(4L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(4L, QtVideoClipStatus.HIDDEN))
                .thenReturn(false);
        when(bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(200L, 201L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(List.of(
                        TestEntityFactory.bibleVerseVideoSegment(200L, sourceVideo, "100.000", "110.000"),
                        TestEntityFactory.bibleVerseVideoSegment(201L, sourceVideo, "110.000", "120.000")
                ));
        when(qtVideoClipRepository.save(any(QtVideoClip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.prepare(4L));

        ArgumentCaptor<QtVideoClip> captor = ArgumentCaptor.forClass(QtVideoClip.class);
        verify(qtVideoClipRepository).save(captor.capture());
        QtVideoClip saved = captor.getValue();
        assertEquals(4L, saved.getQtPassageId());
        assertEquals("QT video 2026-06-09", saved.getTitle());
        assertEquals(new BigDecimal("100.000"), saved.getStartTimeSec());
        assertEquals(new BigDecimal("120.000"), saved.getEndTimeSec());
        assertEquals(QtVideoClipStatus.APPROVED, saved.getStatus());
    }

    @Test
    @DisplayName("Skips clip creation when any QT verse timecode is missing")
    void prepare_skipsWhenSegmentMissing() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, List.of(100L, 101L, 102L), true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(6L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.HIDDEN))
                .thenReturn(false);
        when(bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(100L, 101L, 102L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(List.of(
                        TestEntityFactory.bibleVerseVideoSegment(100L, sourceVideo, "10.000", "20.000"),
                        TestEntityFactory.bibleVerseVideoSegment(101L, sourceVideo, "20.000", "30.000")
                ));

        assertFalse(service.prepare(6L));

        verify(qtVideoClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Skips clip creation when QT passage is not published")
    void prepare_skipsWhenPassageIsUnpublished() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, List.of(100L), false));

        assertFalse(service.prepare(6L));

        verify(qtVideoClipRepository, never()).findByQtPassageIdAndActiveUniqueKey(any(), any());
        verify(bibleVerseVideoSegmentRepository, never())
                .findActiveSourceSegmentsByVerseIds(any(), eq(SourceVideoStatus.ACTIVE), eq(SourceVideo.ACTIVE_UNIQUE_KEY));
        verify(qtVideoClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Skips clip creation when calculated timecode range is reversed")
    void prepare_skipsWhenTimecodeRangeIsInvalid() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, List.of(100L), true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(6L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.HIDDEN))
                .thenReturn(false);
        when(bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(100L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(List.of(
                        TestEntityFactory.bibleVerseVideoSegment(100L, sourceVideo, "40.000", "30.000")
                ));

        assertFalse(service.prepare(6L));

        verify(qtVideoClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Does not auto-approve a QT clip when it was manually hidden")
    void prepare_respectsHiddenClip() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, List.of(100L), true));
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(6L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.HIDDEN))
                .thenReturn(true);

        assertFalse(service.prepare(6L));

        verify(bibleVerseVideoSegmentRepository, never())
                .findActiveSourceSegmentsByVerseIds(any(), eq(SourceVideoStatus.ACTIVE), eq(SourceVideo.ACTIVE_UNIQUE_KEY));
        verify(qtVideoClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Prepares today's QT clip on server startup")
    void prepareToday_usesTodayContext() {
        when(getQtPassageContentContextUseCase.findContentContextByDate(LocalDate.of(2026, 6, 11)))
                .thenReturn(Optional.of(context(6L, List.of(100L), true)));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        when(qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(6L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.empty());
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(6L, QtVideoClipStatus.HIDDEN))
                .thenReturn(false);
        when(bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
                List.of(100L), SourceVideoStatus.ACTIVE, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(List.of(
                        TestEntityFactory.bibleVerseVideoSegment(100L, sourceVideo, "10.000", "20.000")
                ));

        assertTrue(service.prepareToday());
    }

    private static QtPassageContentContext context(Long qtPassageId, List<Long> verseIds, boolean published) {
        return new QtPassageContentContext(
                qtPassageId,
                LocalDate.of(2026, 6, 11),
                "test QT",
                verseIds,
                published
        );
    }
}
