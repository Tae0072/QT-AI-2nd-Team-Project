package com.qtai.domain.qtvideo.internal;

import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QtVideoClipPreparationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final BibleVerseVideoSegmentRepository bibleVerseVideoSegmentRepository;
    private final QtVideoClipRepository qtVideoClipRepository;
    private final Clock clock;
    private final ConcurrentMap<Long, ReentrantLock> preparationLocks = new ConcurrentHashMap<>();

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public boolean prepareToday() {
        LocalDate today = LocalDate.now(clock.withZone(KST));
        return getQtPassageContentContextUseCase.findContentContextByDate(today)
                .map(this::prepare)
                .orElseGet(() -> {
                    log.debug("Skip QT video clip preparation - no QT passage for today. qtDate={}", today);
                    return false;
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public boolean prepare(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            log.warn("Skip QT video clip preparation - invalid qtPassageId={}", qtPassageId);
            return false;
        }
        return prepare(getQtPassageContentContextUseCase.getContentContext(qtPassageId));
    }

    private boolean prepare(QtPassageContentContext context) {
        if (context == null || context.qtPassageId() == null) {
            return false;
        }
        if (!context.published()) {
            log.debug("Skip QT video clip preparation - unpublished QT. qtPassageId={}", context.qtPassageId());
            return false;
        }

        List<Long> verseIds = distinctVerseIds(context.verseIds());
        if (verseIds.isEmpty()) {
            log.warn("Skip QT video clip preparation - no verse mapping. qtPassageId={}", context.qtPassageId());
            return false;
        }

        ReentrantLock lock = preparationLocks.computeIfAbsent(context.qtPassageId(), ignored -> new ReentrantLock());
        lock.lock();
        boolean releaseAfterTransaction = registerLockRelease(context.qtPassageId(), lock);
        try {
            return prepareLocked(context, verseIds);
        } finally {
            if (!releaseAfterTransaction) {
                releasePreparationLock(context.qtPassageId(), lock);
            }
        }
    }

    private boolean prepareLocked(QtPassageContentContext context, List<Long> verseIds) {
        Optional<QtVideoClip> activeClip = qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(
                context.qtPassageId(), QtVideoClip.ACTIVE_UNIQUE_KEY);
        if (activeClip.isEmpty()
                && qtVideoClipRepository.existsByQtPassageIdAndStatus(context.qtPassageId(), QtVideoClipStatus.HIDDEN)) {
            log.info("Skip QT video clip preparation - clip is manually hidden. qtPassageId={}", context.qtPassageId());
            return false;
        }

        Optional<SegmentGroup> segmentGroup = findCompleteSegmentGroup(verseIds);
        if (segmentGroup.isEmpty()) {
            log.warn(
                    "Skip QT video clip preparation - complete timecode mapping not found. qtPassageId={}, verseCount={}",
                    context.qtPassageId(),
                    verseIds.size()
            );
            return false;
        }

        SegmentGroup group = segmentGroup.get();
        if (group.endTimeSec().compareTo(group.startTimeSec()) <= 0) {
            log.warn(
                    "Skip QT video clip preparation - invalid timecode. qtPassageId={}, start={}, end={}",
                    context.qtPassageId(),
                    group.startTimeSec(),
                    group.endTimeSec()
            );
            return false;
        }

        String title = "QT video " + context.qtDate();
        LocalDateTime approvedAt = LocalDateTime.now(clock);
        QtVideoClip clip = activeClip.orElseGet(() -> QtVideoClip.approvedSingleCut(
                context.qtPassageId(),
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
        qtVideoClipRepository.save(clip);
        log.info(
                "Prepared QT video clip. qtPassageId={}, sourceVideoId={}, start={}, end={}",
                context.qtPassageId(),
                group.sourceVideo().getId(),
                group.startTimeSec(),
                group.endTimeSec()
        );
        return true;
    }

    private boolean registerLockRelease(Long qtPassageId, ReentrantLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releasePreparationLock(qtPassageId, lock);
            }
        });
        return true;
    }

    private void releasePreparationLock(Long qtPassageId, ReentrantLock lock) {
        lock.unlock();
        if (!lock.isLocked() && !lock.hasQueuedThreads()) {
            preparationLocks.remove(qtPassageId, lock);
        }
    }

    private Optional<SegmentGroup> findCompleteSegmentGroup(List<Long> verseIds) {
        List<BibleVerseVideoSegment> segments = bibleVerseVideoSegmentRepository.findActiveSourceSegmentsByVerseIds(
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
                .map(this::toSegmentGroup);
    }

    private SegmentGroup toSegmentGroup(List<BibleVerseVideoSegment> segments) {
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

    private static List<Long> distinctVerseIds(List<Long> verseIds) {
        if (verseIds == null) {
            return List.of();
        }
        return verseIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static long mappedVerseCount(List<BibleVerseVideoSegment> segments) {
        return segments.stream()
                .map(BibleVerseVideoSegment::getBibleVerseId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private record SegmentGroup(SourceVideo sourceVideo, BigDecimal startTimeSec, BigDecimal endTimeSec) {
    }
}
