package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipResult;

/**
 * 노출 중인 시뮬레이터 클립을 숨긴다 (P1-11 긴급 차단).
 *
 * <p>ai 산출물 HIDE 시 simulator_clips가 APPROVED로 남아 사용자 노출이 계속되던 비대칭을 해소.
 */
public interface HidePublishedSimulatorClipUseCase {

    HidePublishedSimulatorClipResult hidePublishedSimulatorClip(
            HidePublishedSimulatorClipCommand command
    );
}
