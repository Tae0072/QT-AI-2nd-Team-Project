package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipResult;

/**
 * AI 산출물 승인본으로부터 사용자 노출용 시뮬레이터 클립을 게시한다 (P1-11).
 *
 * <p>해설(VerseExplanation)에는 publish/hide 경로가 있었으나 SIMULATOR에는 APPROVED row를
 * 만드는 경로가 전혀 없어, 승인해도 사용자 시뮬레이터 버튼(READY)이 켜지지 않는 비대칭이 있었다.
 * 이 UseCase가 ai 승인 흐름에서 호출되어 simulator_clips에 APPROVED 클립을 만든다.
 */
public interface PublishApprovedSimulatorClipUseCase {

    PublishApprovedSimulatorClipResult publishApprovedSimulatorClip(
            PublishApprovedSimulatorClipCommand command
    );
}
