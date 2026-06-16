package com.qtai.domain.qt.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시 시각(QT 날짜 04:00 KST)이 도래한 미게시 자동수집 본문을 게시한다.
 *
 * <p>자동수집 본문은 수집 즉시 노출하지 않고 '미게시(PENDING_REVIEW)'로 두며(§6: 00:00~04:00 이전 캐시),
 * 04:00에 이 로직이 게시(status=ACTIVE, publishedAt=해당 날짜 04:00)해 노출시킨다. 정기 실행(04:00)과
 * 기동 catch-up이 같은 메서드를 공유한다 — {@code cutoff} 이하의 누락분(과거 날짜)도 함께 게시한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QtPassageAutoPublishService {

    /** 게시 시각 = QT 날짜 04:00 KST (사용자 노출/cache refresh 기준, CLAUDE.md §6). */
    static final LocalTime PUBLISH_TIME = LocalTime.of(4, 0);

    private final QtPassageRepository qtPassageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 게시 시각이 도래한 미게시 자동수집 본문을 게시한다.
     *
     * @return 게시한 본문 수
     */
    @Transactional
    public int publishDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        // 04:00 이전이면 오늘 본문은 아직 게시 시각 미도래 → 어제까지만 대상(누락분 포함).
        LocalDate cutoff = now.toLocalTime().isBefore(PUBLISH_TIME)
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();

        List<QtPassage> targets =
                qtPassageRepository.findAutoPublishTargets(QtPassageStatus.PENDING_REVIEW, cutoff);
        for (QtPassage passage : targets) {
            LocalDateTime publishedAt = passage.getQtDate().atTime(PUBLISH_TIME);
            passage.publish(publishedAt);
            // 게시되면서 비로소 노출 대상이 되므로, 절 매핑 변경 이벤트를 다시 발행해
            // QT영상 클립 준비(미게시 본문은 skip하던 QtVideoClipPreparationListener)를 트리거한다.
            eventPublisher.publishEvent(new QtPassageVerseMappingsChangedEvent(passage.getId()));
            log.info("QT 자동게시. qtPassageId={}, qtDate={}, publishedAt={}",
                    passage.getId(), passage.getQtDate(), publishedAt);
        }
        return targets.size();
    }
}
