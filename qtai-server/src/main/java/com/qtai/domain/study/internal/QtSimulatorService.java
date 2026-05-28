package com.qtai.domain.study.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.study.api.GetQtSimulatorUseCase;
import com.qtai.domain.study.api.dto.QtSimulatorResponse;
import com.qtai.domain.study.api.dto.QtSimulatorUserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtSimulatorService implements GetQtSimulatorUseCase {

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final SimulatorClipRepository simulatorClipRepository;
    private final ObjectMapper objectMapper;

    @Override
    public QtSimulatorResponse getSimulator(Long qtPassageId) {
        validateQtPassageId(qtPassageId);
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        if (!context.published()) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        return simulatorClipRepository
                .findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(qtPassageId, SimulatorClipStatus.APPROVED)
                .map(this::toReadyResponse)
                .orElseGet(() -> QtSimulatorResponse.missing(qtPassageId));
    }

    private static void validateQtPassageId(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private QtSimulatorResponse toReadyResponse(SimulatorClip clip) {
        JsonNode sceneScriptJson;
        try {
            sceneScriptJson = objectMapper.readTree(clip.getSceneScriptJson());
        } catch (JsonProcessingException e) {
            log.warn("승인 시뮬레이터 클립 JSON 파싱 실패. clipId={}, qtPassageId={}, error={}",
                    clip.getId(), clip.getQtPassageId(), e.getOriginalMessage());
            return QtSimulatorResponse.failed(clip.getQtPassageId());
        }

        return new QtSimulatorResponse(
                QtSimulatorUserStatus.READY.name(),
                clip.getId(),
                clip.getQtPassageId(),
                clip.getTitle(),
                clip.getComponentLibraryVersion().getVersion(),
                sceneScriptJson,
                clip.getStatus().name()
        );
    }
}
