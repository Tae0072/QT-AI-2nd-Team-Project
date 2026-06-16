package com.qtai.domain.qt.internal;

import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시 시각(QT 날짜 04:00 KST)이 도래한 미게시 자동수집 본문을 게시한다.
 *
 * <p>정기 실행(04:00)과 기동 catch-up은 같은 {@link #publishDue()}를 공유한다.
 * 다중 인스턴스 중복 게시는 repository의 pessimistic write lock과 loop 내부 상태 재검증으로 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QtPassageAutoPublishService {

    /** 게시 시각 = QT 날짜 04:00 KST (사용자 노출/cache refresh 기준, CLAUDE.md §6). */
    static final LocalTime PUBLISH_TIME = LocalTime.of(4, 0);
    private static final String ACTOR_TYPE_SYSTEM_BATCH = "SYSTEM_BATCH";

    private final QtPassageRepository qtPassageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 게시 시각이 도래한 미게시 자동수집 본문을 게시한다.
     *
     * @return 실제 게시한 본문 수
     */
    @Transactional
    public int publishDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate cutoff = now.toLocalTime().isBefore(PUBLISH_TIME)
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();

        List<QtPassage> targets =
                qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, cutoff);
        int publishedCount = 0;
        for (QtPassage passage : targets) {
            if (!isStillAutoPublishable(passage)) {
                log.info("QT 자동게시 skip. actorType={}, qtPassageId={}, qtDate={}, status={}, collectedAt={}",
                        ACTOR_TYPE_SYSTEM_BATCH,
                        passage.getId(),
                        passage.getQtDate(),
                        passage.getStatus(),
                        passage.getCollectedAt());
                continue;
            }

            LocalDateTime publishedAt = passage.getQtDate().atTime(PUBLISH_TIME);
            passage.publish(publishedAt);
            eventPublisher.publishEvent(new QtPassageVerseMappingsChangedEvent(passage.getId()));
            publishedCount++;
            log.info("QT 자동게시. actorType={}, qtPassageId={}, qtDate={}, publishedAt={}",
                    ACTOR_TYPE_SYSTEM_BATCH, passage.getId(), passage.getQtDate(), publishedAt);
        }
        return publishedCount;
    }

    private boolean isStillAutoPublishable(QtPassage passage) {
        return passage.getStatus() == QtPassageStatus.PENDING_REVIEW
                && passage.getCollectedAt() != null;
    }
}
