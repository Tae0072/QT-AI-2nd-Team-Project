package com.qtai.domain.study.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.study.api.dto.QtSimulatorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.qtai.support.TestEntityFactory.simulatorClip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QtSimulatorServiceTest {

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase =
            mock(GetQtPassageContentContextUseCase.class);
    private final SimulatorClipRepository simulatorClipRepository = mock(SimulatorClipRepository.class);
    private final QtSimulatorService service = new QtSimulatorService(
            getQtPassageContentContextUseCase,
            simulatorClipRepository,
            new ObjectMapper()
    );

    @Test
    @DisplayName("APPROVED 클립만 READY로 노출한다")
    void getSimulator_whenApprovedClipExists_returnsReady() {
        when(getQtPassageContentContextUseCase.getContentContext(10L)).thenReturn(context(true));
        when(simulatorClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                10L,
                SimulatorClipStatus.APPROVED
        )).thenReturn(Optional.of(simulatorClip(
                50L,
                10L,
                SimulatorClipStatus.APPROVED,
                "{\"scenes\":[]}"
        )));

        QtSimulatorResponse response = service.getSimulator(10L);

        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.clipId()).isEqualTo(50L);
        assertThat(response.qtPassageId()).isEqualTo(10L);
        assertThat(response.componentLibraryVersion()).isEqualTo("2026.05.1");
        assertThat(response.sceneScriptJson().get("scenes").isArray()).isTrue();
        assertThat(response.clipStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("승인 클립이 없으면 payload 없이 MISSING으로 반환한다")
    void getSimulator_whenNoApprovedClip_returnsMissing() {
        when(getQtPassageContentContextUseCase.getContentContext(10L)).thenReturn(context(true));
        when(simulatorClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                10L,
                SimulatorClipStatus.APPROVED
        )).thenReturn(Optional.empty());

        QtSimulatorResponse response = service.getSimulator(10L);

        assertThat(response.status()).isEqualTo("MISSING");
        assertThat(response.clipId()).isNull();
        assertThat(response.sceneScriptJson()).isNull();
    }

    @Test
    @DisplayName("승인 클립 JSON이 잘못되면 payload 없이 FAILED로 반환한다")
    void getSimulator_whenApprovedClipJsonInvalid_returnsFailed() {
        when(getQtPassageContentContextUseCase.getContentContext(10L)).thenReturn(context(true));
        when(simulatorClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                10L,
                SimulatorClipStatus.APPROVED
        )).thenReturn(Optional.of(simulatorClip(
                50L,
                10L,
                SimulatorClipStatus.APPROVED,
                "{invalid"
        )));

        QtSimulatorResponse response = service.getSimulator(10L);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.sceneScriptJson()).isNull();
    }

    @Test
    @DisplayName("qtPassageId가 1보다 작으면 INVALID_INPUT")
    void getSimulator_whenInvalidId_throwsInvalidInput() {
        assertThatThrownBy(() -> service.getSimulator(0L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("노출 불가 QT 본문이면 QT_PASSAGE_NOT_FOUND")
    void getSimulator_whenUnpublished_throwsQtPassageNotFound() {
        when(getQtPassageContentContextUseCase.getContentContext(10L)).thenReturn(context(false));

        assertThatThrownBy(() -> service.getSimulator(10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND));
    }

    private static QtPassageContentContext context(boolean published) {
        return new QtPassageContentContext(
                10L,
                LocalDate.of(2026, 5, 28),
                "test",
                List.of(1L),
                published
        );
    }
}
