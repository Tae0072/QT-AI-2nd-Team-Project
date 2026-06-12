package com.qtai.domain.study.internal;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase;
import com.qtai.domain.study.api.dto.QtStudyAvailability;
import com.qtai.domain.qtvideo.api.GetQtVideoAvailabilityUseCase;

/**
 * Today QT enrich용 가용성 판정 — 승인 콘텐츠 존재 여부만 가볍게 조회한다.
 *
 * <p>의도적으로 qt 도메인을 역호출하지 않는다(verseIds는 호출자가 전달) —
 * qt→study→qt 빈 순환을 방지한다. 노출 게이트(published)는 호출자(qt)가
 * 이미 통과시킨 본문에 대해서만 호출된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class QtStudyAvailabilityService implements GetQtStudyAvailabilityUseCase {

    private static final String SIMULATOR_READY = "READY";
    private static final String SIMULATOR_MISSING = "MISSING";
    private static final String ACTIVE_UNIQUE_KEY = "ACTIVE";

    private final GetQtVideoAvailabilityUseCase getQtVideoAvailabilityUseCase;
    private final VerseExplanationRepository verseExplanationRepository;

    @Override
    public QtStudyAvailability getAvailability(Long qtPassageId, List<Long> verseIds) {
        boolean simulatorReady = getQtVideoAvailabilityUseCase.hasReadyVideo(qtPassageId);

        boolean hasExplanation = verseIds != null && !verseIds.isEmpty()
                && !verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        verseIds, VerseExplanationStatus.APPROVED, ACTIVE_UNIQUE_KEY)
                .isEmpty();

        return new QtStudyAvailability(
                simulatorReady ? SIMULATOR_READY : SIMULATOR_MISSING,
                hasExplanation
        );
    }
}
