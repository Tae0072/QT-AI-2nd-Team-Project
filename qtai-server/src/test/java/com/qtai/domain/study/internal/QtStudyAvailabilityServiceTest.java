package com.qtai.domain.study.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.qtai.domain.study.api.dto.QtStudyAvailability;

/**
 * Today QT enrich용 가용성 판정 테스트 (CLAUDE.md §6 — Today QT 100%).
 */
class QtStudyAvailabilityServiceTest {

    private SimulatorClipRepository simulatorClipRepository;
    private VerseExplanationRepository verseExplanationRepository;
    private QtStudyAvailabilityService service;

    @BeforeEach
    void setUp() {
        simulatorClipRepository = mock(SimulatorClipRepository.class);
        verseExplanationRepository = mock(VerseExplanationRepository.class);
        service = new QtStudyAvailabilityService(simulatorClipRepository, verseExplanationRepository);
    }

    @Test
    @DisplayName("승인(APPROVED) 클립과 승인 해설이 있으면 READY/true")
    void approvedContentsYieldReadyAndTrue() {
        when(simulatorClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                eq(5L), eq(SimulatorClipStatus.APPROVED)))
                .thenReturn(Optional.of(mock(SimulatorClip.class)));
        when(verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                anyList(), eq(VerseExplanationStatus.APPROVED), eq("ACTIVE")))
                .thenReturn(List.of(mock(VerseExplanation.class)));

        QtStudyAvailability availability = service.getAvailability(5L, List.of(101L, 102L));

        assertThat(availability.simulatorStatus()).isEqualTo("READY");
        assertThat(availability.hasExplanation()).isTrue();
    }

    @Test
    @DisplayName("승인 콘텐츠가 없으면 MISSING/false")
    void missingContentsYieldMissingAndFalse() {
        when(simulatorClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                any(), any()))
                .thenReturn(Optional.empty());
        when(verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                anyList(), any(), any()))
                .thenReturn(List.of());

        QtStudyAvailability availability = service.getAvailability(5L, List.of(101L));

        assertThat(availability.simulatorStatus()).isEqualTo("MISSING");
        assertThat(availability.hasExplanation()).isFalse();
    }

    @Test
    @DisplayName("verseIds가 비어 있으면 해설 조회 없이 hasExplanation=false")
    void emptyVerseIdsSkipExplanationLookup() {
        when(simulatorClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                any(), any()))
                .thenReturn(Optional.empty());

        QtStudyAvailability availability = service.getAvailability(5L, List.of());

        assertThat(availability.hasExplanation()).isFalse();
        verifyNoInteractions(verseExplanationRepository);
    }
}
