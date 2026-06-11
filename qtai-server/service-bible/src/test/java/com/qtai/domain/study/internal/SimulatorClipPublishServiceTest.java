package com.qtai.domain.study.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipResult;
import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시뮬레이터 클립 게시·숨김 서비스 테스트 (P1-11).
 */
class SimulatorClipPublishServiceTest {

    private SimulatorClipRepository simulatorClipRepository;
    private SimulatorComponentLibraryVersionRepository componentLibraryVersionRepository;
    private SimulatorClipPublishService service;

    @BeforeEach
    void setUp() {
        simulatorClipRepository = mock(SimulatorClipRepository.class);
        componentLibraryVersionRepository = mock(SimulatorComponentLibraryVersionRepository.class);
        service = new SimulatorClipPublishService(
                simulatorClipRepository, componentLibraryVersionRepository, new ObjectMapper());
    }

    private PublishApprovedSimulatorClipCommand command(String sceneJson) {
        return new PublishApprovedSimulatorClipCommand(
                100L, "오늘 본문 시뮬레이터", 5L, sceneJson, 900L,
                OffsetDateTime.parse("2026-06-01T04:00:00+09:00"));
    }

    @Test
    @DisplayName("게시: 유효한 scene JSON이면 APPROVED 클립을 저장한다")
    void publish_savesApprovedClip() {
        when(componentLibraryVersionRepository.findById(5L))
                .thenReturn(Optional.of(mock(SimulatorComponentLibraryVersion.class)));
        when(simulatorClipRepository.save(any(SimulatorClip.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PublishApprovedSimulatorClipResult result =
                service.publishApprovedSimulatorClip(command("{\"scenes\":[]}"));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.qtPassageId()).isEqualTo(100L);
        verify(simulatorClipRepository).save(any(SimulatorClip.class));
    }

    @Test
    @DisplayName("게시 거부: scene JSON이 파싱 불가면 INVALID_INPUT (저장 안 함)")
    void publish_rejectsInvalidJson() {
        when(componentLibraryVersionRepository.findById(5L))
                .thenReturn(Optional.of(mock(SimulatorComponentLibraryVersion.class)));

        assertThatThrownBy(() -> service.publishApprovedSimulatorClip(command("not-a-json{")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(simulatorClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("게시 거부: 컴포넌트 라이브러리 버전이 없으면 INVALID_INPUT")
    void publish_rejectsMissingVersion() {
        when(componentLibraryVersionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publishApprovedSimulatorClip(command("{}")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(simulatorClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("숨김: 해당 AI 산출물의 APPROVED 클립을 HIDDEN으로 전환한다")
    void hide_hidesApprovedClips() {
        SimulatorClip clip = SimulatorClip.approvedFromAiAsset(
                100L, "t", mock(SimulatorComponentLibraryVersion.class), "{}", 900L, null);
        when(simulatorClipRepository.findApprovedByAiAssetIdForUpdate(900L))
                .thenReturn(List.of(clip));

        HidePublishedSimulatorClipResult result =
                service.hidePublishedSimulatorClip(new HidePublishedSimulatorClipCommand(900L));

        assertThat(result.hiddenCount()).isEqualTo(1);
        assertThat(clip.getStatus()).isEqualTo(SimulatorClipStatus.HIDDEN);
    }

    @Test
    @DisplayName("숨김: 노출 중인 클립이 없으면 hiddenCount=0 (멱등)")
    void hide_noClips_isIdempotent() {
        when(simulatorClipRepository.findApprovedByAiAssetIdForUpdate(900L)).thenReturn(List.of());

        HidePublishedSimulatorClipResult result =
                service.hidePublishedSimulatorClip(new HidePublishedSimulatorClipCommand(900L));

        assertThat(result.hiddenCount()).isZero();
    }

    @Test
    @DisplayName("게시 게이팅: qtPassageId가 0 이하면 INVALID_INPUT (저장 안 함)")
    void publish_rejectsNonPositiveQtPassageId() {
        PublishApprovedSimulatorClipCommand invalid = new PublishApprovedSimulatorClipCommand(
                0L, "제목", 5L, "{}", 900L,
                OffsetDateTime.parse("2026-06-01T04:00:00+09:00"));

        assertThatThrownBy(() -> service.publishApprovedSimulatorClip(invalid))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(simulatorClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("게시 게이팅: title이 공백이면 INVALID_INPUT (저장 안 함)")
    void publish_rejectsBlankTitle() {
        PublishApprovedSimulatorClipCommand invalid = new PublishApprovedSimulatorClipCommand(
                100L, "   ", 5L, "{}", 900L,
                OffsetDateTime.parse("2026-06-01T04:00:00+09:00"));

        assertThatThrownBy(() -> service.publishApprovedSimulatorClip(invalid))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(simulatorClipRepository, never()).save(any());
    }

    @Test
    @DisplayName("숨김 게이팅: aiAssetId가 0 이하면 INVALID_INPUT (노출 클립 조회 안 함)")
    void hide_rejectsNonPositiveAiAssetId() {
        assertThatThrownBy(() -> service.hidePublishedSimulatorClip(
                new HidePublishedSimulatorClipCommand(0L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("게시 게이팅: scene JSON이 최대 길이(200,000자)를 초과하면 INVALID_INPUT (저장 안 함)")
    void publish_rejectsOversizedSceneScript() {
        when(componentLibraryVersionRepository.findById(5L))
                .thenReturn(Optional.of(mock(SimulatorComponentLibraryVersion.class)));
        // 유효 JSON 형식이지만 200,000자 상한을 초과하는 본문 (LONGTEXT 보호 상한 경계)
        String oversized = "{\"d\":\"" + "x".repeat(200_001) + "\"}";

        assertThatThrownBy(() -> service.publishApprovedSimulatorClip(command(oversized)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(simulatorClipRepository, never()).save(any());
    }
}
