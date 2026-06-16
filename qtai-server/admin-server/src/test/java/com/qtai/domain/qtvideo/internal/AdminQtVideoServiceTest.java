package com.qtai.domain.qtvideo.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoClipItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSegmentItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSourceItem;
import com.qtai.domain.qtvideo.api.dto.PrepareQtVideoClipResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AdminQtVideoServiceTest {

    private SourceVideoRepository sourceVideoRepository;
    private BibleVerseVideoSegmentRepository segmentRepository;
    private QtVideoClipRepository clipRepository;
    private ListBibleBooksUseCase listBibleBooksUseCase;
    private GetBibleVerseUseCase getBibleVerseUseCase;
    private GetQtPassageContentContextUseCase contentContextUseCase;
    private WriteAuditLogUseCase auditLogUseCase;
    private AdminQtVideoService service;

    private static final long ADMIN_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        sourceVideoRepository = mock(SourceVideoRepository.class);
        segmentRepository = mock(BibleVerseVideoSegmentRepository.class);
        clipRepository = mock(QtVideoClipRepository.class);
        listBibleBooksUseCase = mock(ListBibleBooksUseCase.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        contentContextUseCase = mock(GetQtPassageContentContextUseCase.class);
        auditLogUseCase = mock(WriteAuditLogUseCase.class);
        service = new AdminQtVideoService(
                sourceVideoRepository,
                segmentRepository,
                clipRepository,
                listBibleBooksUseCase,
                getBibleVerseUseCase,
                contentContextUseCase,
                auditLogUseCase,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneId.of("UTC"))
        );
    }

    private AuditLogWriteRequest capturedAudit() {
        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(captor.capture());
        return captor.getValue();
    }

    @Test
    void createSourceVideoDeactivatesPreviousActiveSourceForSameBook() {
        SourceVideo previous = activeSourceVideo(1L);
        when(sourceVideoRepository.findByBibleBookIdAndActiveUniqueKey((short) 46, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.of(previous));
        when(sourceVideoRepository.save(any(SourceVideo.class))).thenAnswer(invocation -> {
            SourceVideo sourceVideo = invocation.getArgument(0);
            ReflectionTestUtils.setField(sourceVideo, "id", 2L);
            return sourceVideo;
        });

        AdminQtVideoSourceItem result = service.createSourceVideo(
                ADMIN_USER_ID,
                (short) 46,
                "new source",
                "https://example.com/new.mp4",
                new BigDecimal("100.000")
        );

        assertThat(previous.getStatus()).isEqualTo(SourceVideoStatus.INACTIVE);
        assertThat(previous.getActiveUniqueKey()).isNull();
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.status()).isEqualTo(SourceVideoStatus.ACTIVE.name());
        verify(sourceVideoRepository).saveAndFlush(previous);
    }

    @Test
    void updateSourceVideoActivatesTargetAndDeactivatesPreviousActiveSource() {
        SourceVideo target = activeSourceVideo(3L);
        target.deactivate();
        SourceVideo previousActive = activeSourceVideo(4L);
        when(sourceVideoRepository.findById(3L)).thenReturn(Optional.of(target));
        when(sourceVideoRepository.findByBibleBookIdAndActiveUniqueKey((short) 46, SourceVideo.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.of(previousActive));

        AdminQtVideoSourceItem result = service.updateSourceVideo(
                ADMIN_USER_ID,
                3L,
                "updated source",
                "https://example.com/updated.mp4",
                new BigDecimal("120.000"),
                "ACTIVE"
        );

        assertThat(previousActive.getStatus()).isEqualTo(SourceVideoStatus.INACTIVE);
        assertThat(target.getStatus()).isEqualTo(SourceVideoStatus.ACTIVE);
        assertThat(result.title()).isEqualTo("updated source");
        assertThat(result.videoUrl()).isEqualTo("https://example.com/updated.mp4");
        verify(sourceVideoRepository).saveAndFlush(previousActive);
    }

    @Test
    void replaceSegmentsResolvesBibleVerseThroughBiblePublicUseCases() {
        SourceVideo sourceVideo = activeSourceVideo(3L);
        when(sourceVideoRepository.findById(3L)).thenReturn(Optional.of(sourceVideo));
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(
                new BibleBookResponse(46, "NT", "1CO", "고린도전서", "1 Corinthians", 46)
        ));
        when(getBibleVerseUseCase.getVerses("1CO", 10, 14, null)).thenReturn(new BibleVerseRangeResponse(
                new BibleVerseBookResponse("1CO", "고린도전서", "1 Corinthians", 10),
                List.of(new BibleVerseResponse(28582L, "1CO", 10, 14, "", ""))
        ));
        when(segmentRepository.save(any(BibleVerseVideoSegment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AdminQtVideoSegmentItem> result = service.replaceSegments(ADMIN_USER_ID, 3L, List.of(
                new AdminQtVideoService.SegmentCommand(
                        null,
                        (short) 10,
                        (short) 14,
                        new BigDecimal("0.000"),
                        new BigDecimal("10.000")
                )
        ));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bibleVerseId()).isEqualTo(28582L);
        verify(getBibleVerseUseCase).getVerses("1CO", 10, 14, null);
        verify(segmentRepository).deleteBySourceVideo_Id(3L);
        verify(segmentRepository).flush();
    }

    @Test
    void replaceSegmentsRejectsEmptySegments() {
        when(sourceVideoRepository.findById(3L)).thenReturn(Optional.of(activeSourceVideo(3L)));

        assertThatThrownBy(() -> service.replaceSegments(ADMIN_USER_ID, 3L, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("segments must not be empty");
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

        PrepareQtVideoClipResult result = service.prepareClip(ADMIN_USER_ID, 10L);

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

        PrepareQtVideoClipResult result = service.prepareClip(ADMIN_USER_ID, 10L);

        assertThat(result.prepared()).isFalse();
        assertThat(result.clipId()).isNull();
        verify(clipRepository, never()).save(any());
    }

    @Test
    void deleteSourceVideoSoftDeletesSourceClipsAndSegments() {
        SourceVideo sourceVideo = activeSourceVideo(3L);
        when(sourceVideoRepository.findById(3L)).thenReturn(Optional.of(sourceVideo));
        QtVideoClip clip = QtVideoClip.approvedSingleCut(
                10L, "QT video 2026-06-15", sourceVideo, sourceVideo.getVideoUrl(),
                new BigDecimal("10.000"), new BigDecimal("20.000"), null);
        BibleVerseVideoSegment segment = BibleVerseVideoSegment.create(
                28582L, sourceVideo, new BigDecimal("0.000"), new BigDecimal("10.000"));
        when(clipRepository.findBySourceVideo_IdAndDeletedAtIsNull(3L)).thenReturn(List.of(clip));
        when(segmentRepository.findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(3L))
                .thenReturn(List.of(segment));

        service.deleteSourceVideo(ADMIN_USER_ID, 3L);

        // 소프트 삭제: deleted_at만 기록하고 행을 물리 삭제하지 않는다.
        assertThat(sourceVideo.getDeletedAt()).isNotNull();
        assertThat(clip.getDeletedAt()).isNotNull();
        assertThat(segment.getDeletedAt()).isNotNull();
        verify(sourceVideoRepository, never()).delete(any());
        verify(clipRepository, never()).delete(any());
        // 감사 로그를 같은 트랜잭션에서 before-state(동반삭제 수 포함)로 기록한다.
        AuditLogWriteRequest audit = capturedAudit();
        assertThat(audit.actionType()).isEqualTo("QT_VIDEO_SOURCE_DELETE");
        assertThat(audit.actorId()).isEqualTo(ADMIN_USER_ID);
        assertThat(audit.beforeJson()).contains("\"deletedClips\":1", "\"deletedSegments\":1");
        assertThat(audit.afterJson()).isNull();
    }

    @Test
    void deleteClipSoftDeletesClip() {
        SourceVideo sourceVideo = activeSourceVideo(3L);
        QtVideoClip clip = QtVideoClip.approvedSingleCut(
                10L,
                "QT video 2026-06-15",
                sourceVideo,
                sourceVideo.getVideoUrl(),
                new BigDecimal("10.000"),
                new BigDecimal("20.000"),
                null
        );
        ReflectionTestUtils.setField(clip, "id", 700L);
        when(clipRepository.findById(700L)).thenReturn(Optional.of(clip));

        service.deleteClip(ADMIN_USER_ID, 700L);

        assertThat(clip.getDeletedAt()).isNotNull();
        assertThat(clip.getActiveUniqueKey()).isNull();
        verify(clipRepository, never()).delete(any());
        AuditLogWriteRequest audit = capturedAudit();
        assertThat(audit.actionType()).isEqualTo("QT_VIDEO_CLIP_DELETE");
        assertThat(audit.beforeJson()).contains("\"qtPassageId\":10", "\"sourceVideoId\":3");
        assertThat(audit.afterJson()).isNull();
    }

    @Test
    void changeClipStatusApprovesTargetAndHidesPreviousActiveClip() {
        SourceVideo sourceVideo = activeSourceVideo(3L);
        QtVideoClip target = QtVideoClip.approvedSingleCut(
                10L,
                "old",
                sourceVideo,
                sourceVideo.getVideoUrl(),
                new BigDecimal("10.000"),
                new BigDecimal("20.000"),
                null
        );
        target.hide();
        ReflectionTestUtils.setField(target, "id", 700L);
        QtVideoClip previousActive = QtVideoClip.approvedSingleCut(
                10L,
                "active",
                sourceVideo,
                sourceVideo.getVideoUrl(),
                new BigDecimal("30.000"),
                new BigDecimal("40.000"),
                null
        );
        ReflectionTestUtils.setField(previousActive, "id", 701L);
        when(clipRepository.findById(700L)).thenReturn(Optional.of(target));
        when(clipRepository.findByQtPassageIdAndActiveUniqueKey(10L, QtVideoClip.ACTIVE_UNIQUE_KEY))
                .thenReturn(Optional.of(previousActive));

        AdminQtVideoClipItem result = service.changeClipStatus(ADMIN_USER_ID, 700L, "APPROVED");

        assertThat(previousActive.getStatus()).isEqualTo(QtVideoClipStatus.HIDDEN);
        assertThat(target.getStatus()).isEqualTo(QtVideoClipStatus.APPROVED);
        assertThat(result.status()).isEqualTo(QtVideoClipStatus.APPROVED.name());
        verify(clipRepository).saveAndFlush(previousActive);
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
