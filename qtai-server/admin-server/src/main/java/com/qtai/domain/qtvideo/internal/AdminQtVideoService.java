package com.qtai.domain.qtvideo.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.internal.BibleVerse;
import com.qtai.domain.bible.internal.BibleVerseRepository;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoClipItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoClipListResponse;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSegmentItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSourceItem;
import com.qtai.domain.qtvideo.api.dto.AdminQtVideoSourceListResponse;
import com.qtai.domain.qtvideo.api.dto.PrepareQtVideoClipResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQtVideoService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;
    private static final String UNCONFIGURED_VIDEO_URL_PREFIX = "qt-video://unconfigured/";

    private final SourceVideoRepository sourceVideoRepository;
    private final BibleVerseVideoSegmentRepository segmentRepository;
    private final QtVideoClipRepository clipRepository;
    private final BibleVerseRepository bibleVerseRepository;
    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final Clock clock;

    public AdminQtVideoSourceListResponse listSourceVideos(
            Short bibleBookId,
            String status,
            int page,
            int size
    ) {
        requirePage(page, size);
        SourceVideoStatus parsedStatus = parseSourceStatus(status);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SourceVideo> result;
        if (bibleBookId != null && parsedStatus != null) {
            result = sourceVideoRepository.findByBibleBookIdAndStatus(bibleBookId, parsedStatus, pageRequest);
        } else if (bibleBookId != null) {
            result = sourceVideoRepository.findByBibleBookId(bibleBookId, pageRequest);
        } else if (parsedStatus != null) {
            result = sourceVideoRepository.findByStatus(parsedStatus, pageRequest);
        } else {
            result = sourceVideoRepository.findAll(pageRequest);
        }
        return new AdminQtVideoSourceListResponse(
                result.getContent().stream().map(AdminQtVideoService::toSourceItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Transactional
    public AdminQtVideoSourceItem createSourceVideo(
            Short bibleBookId,
            String title,
            String videoUrl,
            BigDecimal durationSec
    ) {
        requirePositive(bibleBookId == null ? null : bibleBookId.longValue(), "bibleBookId");
        requireText(title, "title");
        requireText(videoUrl, "videoUrl");
        requirePositiveDecimal(durationSec, "durationSec");

        sourceVideoRepository.findByBibleBookIdAndActiveUniqueKey(bibleBookId, SourceVideo.ACTIVE_UNIQUE_KEY)
                .ifPresent(sourceVideo -> {
                    sourceVideo.deactivate();
                    sourceVideoRepository.saveAndFlush(sourceVideo);
                });
        SourceVideo saved = sourceVideoRepository.save(SourceVideo.active(bibleBookId, title, videoUrl, durationSec));
        return toSourceItem(saved);
    }

    @Transactional
    public AdminQtVideoSourceItem updateSourceVideo(
            Long sourceVideoId,
            String title,
            String videoUrl,
            BigDecimal durationSec,
            String status
    ) {
        SourceVideo sourceVideo = requireSourceVideo(sourceVideoId);
        requireText(title, "title");
        requireText(videoUrl, "videoUrl");
        requirePositiveDecimal(durationSec, "durationSec");
        sourceVideo.update(title, videoUrl, durationSec);

        SourceVideoStatus nextStatus = parseRequiredSourceStatus(status);
        if (nextStatus == SourceVideoStatus.ACTIVE) {
            sourceVideoRepository.findByBibleBookIdAndActiveUniqueKey(
                            sourceVideo.getBibleBookId(),
                            SourceVideo.ACTIVE_UNIQUE_KEY
                    )
                    .filter(active -> !active.getId().equals(sourceVideo.getId()))
                    .ifPresent(active -> {
                        active.deactivate();
                        sourceVideoRepository.saveAndFlush(active);
                    });
            sourceVideo.activate();
        } else {
            sourceVideo.deactivate();
        }
        return toSourceItem(sourceVideo);
    }

    @Transactional
    public void deleteSourceVideo(Long sourceVideoId) {
        SourceVideo sourceVideo = requireSourceVideo(sourceVideoId);
        // 원본 영상을 지우면 그 원본으로 만든 QT 클립과 절별 구간도 함께 삭제한다.
        clipRepository.deleteBySourceVideo_Id(sourceVideoId);
        segmentRepository.deleteBySourceVideo_Id(sourceVideoId);
        segmentRepository.flush();
        sourceVideoRepository.delete(sourceVideo);
    }

    @Transactional
    public void deleteClip(Long clipId) {
        QtVideoClip clip = clipRepository.findById(requirePositive(clipId, "clipId"))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "qt video clip not found"));
        clipRepository.delete(clip);
    }

    public List<AdminQtVideoSegmentItem> listSegments(Long sourceVideoId) {
        requireSourceVideo(sourceVideoId);
        return segmentRepository.findBySourceVideo_IdOrderByStartTimeSecAscIdAsc(sourceVideoId)
                .stream()
                .map(AdminQtVideoService::toSegmentItem)
                .toList();
    }

    @Transactional
    public List<AdminQtVideoSegmentItem> replaceSegments(Long sourceVideoId, List<SegmentCommand> segments) {
        SourceVideo sourceVideo = requireSourceVideo(sourceVideoId);
        if (segments == null || segments.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "segments must not be empty");
        }

        segmentRepository.deleteBySourceVideo_Id(sourceVideoId);
        segmentRepository.flush();

        List<BibleVerseVideoSegment> saved = segments.stream()
                .map(segment -> segmentEntity(sourceVideo, segment))
                .map(segmentRepository::save)
                .toList();
        return saved.stream().map(AdminQtVideoService::toSegmentItem).toList();
    }

    public AdminQtVideoClipListResponse listClips(Long qtPassageId, String status, int page, int size) {
        requirePage(page, size);
        QtVideoClipStatus parsedStatus = parseClipStatus(status);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<QtVideoClip> result;
        if (qtPassageId != null && parsedStatus != null) {
            result = clipRepository.findByQtPassageIdAndStatus(qtPassageId, parsedStatus, pageRequest);
        } else if (qtPassageId != null) {
            result = clipRepository.findByQtPassageId(qtPassageId, pageRequest);
        } else if (parsedStatus != null) {
            result = clipRepository.findByStatus(parsedStatus, pageRequest);
        } else {
            result = clipRepository.findAll(pageRequest);
        }
        return new AdminQtVideoClipListResponse(
                result.getContent().stream().map(AdminQtVideoService::toClipItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Transactional
    public PrepareQtVideoClipResult prepareClip(Long qtPassageId) {
        requirePositive(qtPassageId, "qtPassageId");
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        if (context == null || !context.published()) {
            return new PrepareQtVideoClipResult(qtPassageId, false, null);
        }

        List<Long> verseIds = distinctVerseIds(context.verseIds());
        if (verseIds.isEmpty()) {
            return new PrepareQtVideoClipResult(qtPassageId, false, null);
        }

        Optional<SegmentGroup> segmentGroup = findCompleteSegmentGroup(verseIds);
        if (segmentGroup.isEmpty()) {
            return new PrepareQtVideoClipResult(qtPassageId, false, null);
        }

        SegmentGroup group = segmentGroup.get();
        if (!hasPlayableUrl(group.sourceVideo())
                || group.endTimeSec().compareTo(group.startTimeSec()) <= 0) {
            return new PrepareQtVideoClipResult(qtPassageId, false, null);
        }

        Optional<QtVideoClip> activeClip = clipRepository.findByQtPassageIdAndActiveUniqueKey(
                qtPassageId,
                QtVideoClip.ACTIVE_UNIQUE_KEY
        );
        LocalDateTime approvedAt = LocalDateTime.now(clock.withZone(KST));
        String title = "QT video " + context.qtDate();
        QtVideoClip clip = activeClip.orElseGet(() -> QtVideoClip.approvedSingleCut(
                qtPassageId,
                title,
                group.sourceVideo(),
                group.sourceVideo().getVideoUrl(),
                group.startTimeSec(),
                group.endTimeSec(),
                approvedAt
        ));
        if (activeClip.isPresent()) {
            clip.replaceWithApprovedSingleCut(
                    title,
                    group.sourceVideo(),
                    group.sourceVideo().getVideoUrl(),
                    group.startTimeSec(),
                    group.endTimeSec(),
                    approvedAt
            );
        }
        QtVideoClip saved = clipRepository.save(clip);
        return new PrepareQtVideoClipResult(qtPassageId, true, saved.getId());
    }

    @Transactional
    public AdminQtVideoClipItem changeClipStatus(Long clipId, String status) {
        QtVideoClip clip = clipRepository.findById(requirePositive(clipId, "clipId"))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "qt video clip not found"));
        QtVideoClipStatus nextStatus = parseRequiredClipStatus(status);
        if (nextStatus == QtVideoClipStatus.APPROVED) {
            clipRepository.findByQtPassageIdAndActiveUniqueKey(clip.getQtPassageId(), QtVideoClip.ACTIVE_UNIQUE_KEY)
                    .filter(active -> !active.getId().equals(clip.getId()))
                    .ifPresent(active -> {
                        active.hide();
                        clipRepository.saveAndFlush(active);
                    });
            clip.approve(LocalDateTime.now(clock.withZone(KST)));
        } else if (nextStatus == QtVideoClipStatus.HIDDEN) {
            clip.hide();
        } else if (nextStatus == QtVideoClipStatus.FAILED) {
            clip.fail();
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status is not supported");
        }
        return toClipItem(clip);
    }

    private BibleVerseVideoSegment segmentEntity(SourceVideo sourceVideo, SegmentCommand segment) {
        if (segment == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "segment must not be null");
        }
        Long bibleVerseId = segment.bibleVerseId();
        if (bibleVerseId == null) {
            requirePositive(segment.chapter() == null ? null : segment.chapter().longValue(), "chapter");
            requirePositive(segment.verse() == null ? null : segment.verse().longValue(), "verse");
            bibleVerseId = bibleVerseRepository.findByBook_IdAndChapterNoAndVerseNo(
                            sourceVideo.getBibleBookId(),
                            segment.chapter(),
                            segment.verse()
                    )
                    .map(BibleVerse::getId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "bible verse not found"));
        }
        requirePositive(bibleVerseId, "bibleVerseId");
        requireTimeRange(segment.startTimeSec(), segment.endTimeSec());
        return BibleVerseVideoSegment.create(
                bibleVerseId,
                sourceVideo,
                segment.startTimeSec(),
                segment.endTimeSec()
        );
    }

    private Optional<SegmentGroup> findCompleteSegmentGroup(List<Long> verseIds) {
        List<BibleVerseVideoSegment> segments = segmentRepository.findActiveSourceSegmentsByVerseIds(
                verseIds,
                SourceVideoStatus.ACTIVE,
                SourceVideo.ACTIVE_UNIQUE_KEY
        );
        if (segments.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, List<BibleVerseVideoSegment>> bySourceVideoId = segments.stream()
                .filter(segment -> segment.getSourceVideo() != null && segment.getSourceVideo().getId() != null)
                .collect(Collectors.groupingBy(
                        segment -> segment.getSourceVideo().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int requiredVerseCount = verseIds.size();
        return bySourceVideoId.values().stream()
                .filter(candidateSegments -> mappedVerseCount(candidateSegments) == requiredVerseCount)
                .min(Comparator.comparing(candidateSegments -> candidateSegments.get(0).getSourceVideo().getId()))
                .map(AdminQtVideoService::toSegmentGroup);
    }

    private static SegmentGroup toSegmentGroup(List<BibleVerseVideoSegment> segments) {
        BigDecimal startTimeSec = segments.stream()
                .map(BibleVerseVideoSegment::getStartTimeSec)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal endTimeSec = segments.stream()
                .map(BibleVerseVideoSegment::getEndTimeSec)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        return new SegmentGroup(segments.get(0).getSourceVideo(), startTimeSec, endTimeSec);
    }

    private SourceVideo requireSourceVideo(Long sourceVideoId) {
        return sourceVideoRepository.findById(requirePositive(sourceVideoId, "sourceVideoId"))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "source video not found"));
    }

    private static AdminQtVideoSourceItem toSourceItem(SourceVideo sourceVideo) {
        return new AdminQtVideoSourceItem(
                sourceVideo.getId(),
                sourceVideo.getBibleBookId(),
                sourceVideo.getTitle(),
                sourceVideo.getVideoUrl(),
                sourceVideo.getDurationSec(),
                sourceVideo.getStatus().name(),
                toOffset(sourceVideo.getCreatedAt())
        );
    }

    private static AdminQtVideoSegmentItem toSegmentItem(BibleVerseVideoSegment segment) {
        return new AdminQtVideoSegmentItem(
                segment.getId(),
                segment.getBibleVerseId(),
                segment.getStartTimeSec(),
                segment.getEndTimeSec()
        );
    }

    private static AdminQtVideoClipItem toClipItem(QtVideoClip clip) {
        return new AdminQtVideoClipItem(
                clip.getId(),
                clip.getQtPassageId(),
                clip.getTitle(),
                clip.getSourceVideo().getId(),
                clip.getVideoUrl(),
                clip.getStartTimeSec(),
                clip.getEndTimeSec(),
                clip.getCompositionType().name(),
                clip.getStatus().name(),
                toOffset(clip.getApprovedAt())
        );
    }

    private static OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(KST).toOffsetDateTime();
    }

    private static SourceVideoStatus parseSourceStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseRequiredSourceStatus(status);
    }

    private static SourceVideoStatus parseRequiredSourceStatus(String status) {
        try {
            return SourceVideoStatus.valueOf(requireText(status, "status"));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status is not supported");
        }
    }

    private static QtVideoClipStatus parseClipStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseRequiredClipStatus(status);
    }

    private static QtVideoClipStatus parseRequiredClipStatus(String status) {
        try {
            return QtVideoClipStatus.valueOf(requireText(status, "status"));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status is not supported");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return value;
    }

    private static void requirePositiveDecimal(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requireTimeRange(BigDecimal startTimeSec, BigDecimal endTimeSec) {
        if (startTimeSec == null || startTimeSec.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "startTimeSec must not be negative");
        }
        if (endTimeSec == null || endTimeSec.compareTo(startTimeSec) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "endTimeSec must be greater than startTimeSec");
        }
    }

    private static boolean hasPlayableUrl(SourceVideo sourceVideo) {
        return sourceVideo != null
                && sourceVideo.getVideoUrl() != null
                && !sourceVideo.getVideoUrl().isBlank()
                && !sourceVideo.getVideoUrl().startsWith(UNCONFIGURED_VIDEO_URL_PREFIX);
    }

    private static List<Long> distinctVerseIds(List<Long> verseIds) {
        if (verseIds == null) {
            return List.of();
        }
        return verseIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static long mappedVerseCount(List<BibleVerseVideoSegment> segments) {
        return segments.stream()
                .map(BibleVerseVideoSegment::getBibleVerseId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    public record SegmentCommand(
            Long bibleVerseId,
            Short chapter,
            Short verse,
            BigDecimal startTimeSec,
            BigDecimal endTimeSec
    ) {
    }

    private record SegmentGroup(SourceVideo sourceVideo, BigDecimal startTimeSec, BigDecimal endTimeSec) {
    }
}
