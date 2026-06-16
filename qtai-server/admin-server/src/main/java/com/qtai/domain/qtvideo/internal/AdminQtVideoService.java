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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
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
    private final ListBibleBooksUseCase listBibleBooksUseCase;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_SOURCE_VIDEO = "SOURCE_VIDEO";
    private static final String TARGET_QT_VIDEO_CLIP = "QT_VIDEO_CLIP";
    private static final String TARGET_QT_PASSAGE = "QT_PASSAGE";
    private static final String ACTION_SOURCE_CREATE = "QT_VIDEO_SOURCE_CREATE";
    private static final String ACTION_SOURCE_UPDATE = "QT_VIDEO_SOURCE_UPDATE";
    private static final String ACTION_SOURCE_DELETE = "QT_VIDEO_SOURCE_DELETE";
    private static final String ACTION_SEGMENTS_REPLACE = "QT_VIDEO_SEGMENTS_REPLACE";
    private static final String ACTION_CLIP_PREPARE = "QT_VIDEO_CLIP_PREPARE";
    private static final String ACTION_CLIP_STATUS_CHANGE = "QT_VIDEO_CLIP_STATUS_CHANGE";
    private static final String ACTION_CLIP_DELETE = "QT_VIDEO_CLIP_DELETE";

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
            result = sourceVideoRepository.findByBibleBookIdAndStatusAndDeletedAtIsNull(
                    bibleBookId, parsedStatus, pageRequest);
        } else if (bibleBookId != null) {
            result = sourceVideoRepository.findByBibleBookIdAndDeletedAtIsNull(bibleBookId, pageRequest);
        } else if (parsedStatus != null) {
            result = sourceVideoRepository.findByStatusAndDeletedAtIsNull(parsedStatus, pageRequest);
        } else {
            result = sourceVideoRepository.findByDeletedAtIsNull(pageRequest);
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
            Long adminUserId,
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
        Map<String, Object> after = orderedMap();
        after.put("bibleBookId", saved.getBibleBookId());
        after.put("status", saved.getStatus().name());
        writeAudit(adminUserId, ACTION_SOURCE_CREATE, TARGET_SOURCE_VIDEO, saved.getId(), null, auditJson(after));
        return toSourceItem(saved);
    }

    @Transactional
    public AdminQtVideoSourceItem updateSourceVideo(
            Long adminUserId,
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
        Map<String, Object> before = orderedMap();
        before.put("title", sourceVideo.getTitle());
        before.put("status", sourceVideo.getStatus().name());
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
        Map<String, Object> after = orderedMap();
        after.put("title", sourceVideo.getTitle());
        after.put("status", sourceVideo.getStatus().name());
        writeAudit(adminUserId, ACTION_SOURCE_UPDATE, TARGET_SOURCE_VIDEO, sourceVideoId,
                auditJson(before), auditJson(after));
        return toSourceItem(sourceVideo);
    }

    @Transactional
    public void deleteSourceVideo(Long adminUserId, Long sourceVideoId) {
        SourceVideo sourceVideo = requireSourceVideo(sourceVideoId);
        // 삭제 전 상태를 스냅샷으로 남겨 감사 로그 before-state로 기록한다.
        Map<String, Object> before = orderedMap();
        before.put("bibleBookId", sourceVideo.getBibleBookId());
        before.put("status", sourceVideo.getStatus().name());
        LocalDateTime deletedAt = LocalDateTime.now(clock.withZone(KST));
        // 원본 영상을 소프트 삭제하면 그 원본으로 만든 QT 클립과 절별 구간도 함께 소프트 삭제한다.
        List<QtVideoClip> clips = clipRepository.findBySourceVideo_IdAndDeletedAtIsNull(sourceVideoId);
        clips.forEach(clip -> clip.softDelete(deletedAt));
        List<BibleVerseVideoSegment> segments =
                segmentRepository.findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(sourceVideoId);
        segments.forEach(segment -> segment.softDelete(deletedAt));
        sourceVideo.softDelete(deletedAt);
        before.put("deletedClips", clips.size());
        before.put("deletedSegments", segments.size());
        writeAudit(adminUserId, ACTION_SOURCE_DELETE, TARGET_SOURCE_VIDEO, sourceVideoId, auditJson(before), null);
    }

    @Transactional
    public void deleteClip(Long adminUserId, Long clipId) {
        QtVideoClip clip = clipRepository.findById(requirePositive(clipId, "clipId"))
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "qt video clip not found"));
        Map<String, Object> before = orderedMap();
        before.put("qtPassageId", clip.getQtPassageId());
        before.put("status", clip.getStatus().name());
        before.put("sourceVideoId", clip.getSourceVideo().getId());
        clip.softDelete(LocalDateTime.now(clock.withZone(KST)));
        writeAudit(adminUserId, ACTION_CLIP_DELETE, TARGET_QT_VIDEO_CLIP, clipId, auditJson(before), null);
    }

    public List<AdminQtVideoSegmentItem> listSegments(Long sourceVideoId) {
        requireSourceVideo(sourceVideoId);
        return segmentRepository.findBySourceVideo_IdAndDeletedAtIsNullOrderByStartTimeSecAscIdAsc(sourceVideoId)
                .stream()
                .map(AdminQtVideoService::toSegmentItem)
                .toList();
    }

    @Transactional
    public List<AdminQtVideoSegmentItem> replaceSegments(
            Long adminUserId, Long sourceVideoId, List<SegmentCommand> segments) {
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
        Map<String, Object> after = orderedMap();
        after.put("segmentCount", saved.size());
        writeAudit(adminUserId, ACTION_SEGMENTS_REPLACE, TARGET_SOURCE_VIDEO, sourceVideoId, null, auditJson(after));
        return saved.stream().map(AdminQtVideoService::toSegmentItem).toList();
    }

    public AdminQtVideoClipListResponse listClips(Long qtPassageId, String status, int page, int size) {
        requirePage(page, size);
        QtVideoClipStatus parsedStatus = parseClipStatus(status);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<QtVideoClip> result;
        if (qtPassageId != null && parsedStatus != null) {
            result = clipRepository.findByQtPassageIdAndStatusAndDeletedAtIsNull(qtPassageId, parsedStatus, pageRequest);
        } else if (qtPassageId != null) {
            result = clipRepository.findByQtPassageIdAndDeletedAtIsNull(qtPassageId, pageRequest);
        } else if (parsedStatus != null) {
            result = clipRepository.findByStatusAndDeletedAtIsNull(parsedStatus, pageRequest);
        } else {
            result = clipRepository.findByDeletedAtIsNull(pageRequest);
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
    public PrepareQtVideoClipResult prepareClip(Long adminUserId, Long qtPassageId) {
        requirePositive(qtPassageId, "qtPassageId");
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        if (context == null || !context.published()) {
            return auditedPrepareResult(adminUserId, new PrepareQtVideoClipResult(qtPassageId, false, null));
        }

        List<Long> verseIds = distinctVerseIds(context.verseIds());
        if (verseIds.isEmpty()) {
            return auditedPrepareResult(adminUserId, new PrepareQtVideoClipResult(qtPassageId, false, null));
        }

        Optional<SegmentGroup> segmentGroup = findCompleteSegmentGroup(verseIds);
        if (segmentGroup.isEmpty()) {
            return auditedPrepareResult(adminUserId, new PrepareQtVideoClipResult(qtPassageId, false, null));
        }

        SegmentGroup group = segmentGroup.get();
        if (!hasPlayableUrl(group.sourceVideo())
                || group.endTimeSec().compareTo(group.startTimeSec()) <= 0) {
            return auditedPrepareResult(adminUserId, new PrepareQtVideoClipResult(qtPassageId, false, null));
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
        return auditedPrepareResult(adminUserId, new PrepareQtVideoClipResult(qtPassageId, true, saved.getId()));
    }

    private PrepareQtVideoClipResult auditedPrepareResult(Long adminUserId, PrepareQtVideoClipResult result) {
        Map<String, Object> after = orderedMap();
        after.put("qtPassageId", result.qtPassageId());
        after.put("prepared", result.prepared());
        after.put("clipId", result.clipId());
        writeAudit(adminUserId, ACTION_CLIP_PREPARE, TARGET_QT_PASSAGE, result.qtPassageId(),
                null, auditJson(after));
        return result;
    }

    @Transactional
    public AdminQtVideoClipItem changeClipStatus(Long adminUserId, Long clipId, String status) {
        QtVideoClip clip = clipRepository.findById(requirePositive(clipId, "clipId"))
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "qt video clip not found"));
        QtVideoClipStatus nextStatus = parseRequiredClipStatus(status);
        String beforeStatus = clip.getStatus().name();
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
        Map<String, Object> before = orderedMap();
        before.put("status", beforeStatus);
        Map<String, Object> after = orderedMap();
        after.put("status", clip.getStatus().name());
        writeAudit(adminUserId, ACTION_CLIP_STATUS_CHANGE, TARGET_QT_VIDEO_CLIP, clipId,
                auditJson(before), auditJson(after));
        return toClipItem(clip);
    }

    private BibleVerseVideoSegment segmentEntity(SourceVideo sourceVideo, SegmentCommand segment) {
        if (segment == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "segment must not be null");
        }
        Long bibleVerseId = segment.bibleVerseId();
        if (bibleVerseId == null) {
            bibleVerseId = resolveBibleVerseId(sourceVideo.getBibleBookId(), segment);
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

    private Long resolveBibleVerseId(Short bibleBookId, SegmentCommand segment) {
        int chapter = requirePositive(segment.chapter() == null ? null : segment.chapter().longValue(), "chapter")
                .intValue();
        int verse = requirePositive(segment.verse() == null ? null : segment.verse().longValue(), "verse")
                .intValue();
        String bookCode = findBibleBookCode(bibleBookId);
        BibleVerseRangeResponse range = getBibleVerseUseCase.getVerses(bookCode, chapter, verse, null);
        if (range == null || range.verses() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "bible verse not found");
        }
        return range.verses().stream()
                .filter(candidate -> isSameVerse(candidate, chapter, verse))
                .findFirst()
                .map(BibleVerseResponse::id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "bible verse not found"));
    }

    private String findBibleBookCode(Short bibleBookId) {
        requirePositive(bibleBookId == null ? null : bibleBookId.longValue(), "bibleBookId");
        return listBibleBooksUseCase.listBibleBooks().stream()
                .filter(book -> book.id() != null && Objects.equals(book.id(), bibleBookId.intValue()))
                .findFirst()
                .map(BibleBookResponse::code)
                .filter(code -> code != null && !code.isBlank())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "bible book not found"));
    }

    private static boolean isSameVerse(BibleVerseResponse candidate, int chapter, int verse) {
        return candidate != null
                && candidate.chapterNo() != null
                && candidate.verseNo() != null
                && candidate.chapterNo() == chapter
                && candidate.verseNo() == verse;
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
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "source video not found"));
    }

    // 감사 로그는 변경과 같은 트랜잭션 안에서 기록한다(원자성). 다른 admin 도메인과 동일 패턴.
    private void writeAudit(
            Long adminUserId,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson
    ) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                adminUserId,
                ACTOR_TYPE_ADMIN,
                adminUserId,
                ACTOR_TYPE_ADMIN + ":" + adminUserId,
                actionType,
                targetType,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    private String auditJson(Map<String, Object> fields) {
        if (fields == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize qt video audit json. keys={}, error={}",
                    fields.keySet(), exception.getMessage());
            return null;
        }
    }

    private static Map<String, Object> orderedMap() {
        return new LinkedHashMap<>();
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
