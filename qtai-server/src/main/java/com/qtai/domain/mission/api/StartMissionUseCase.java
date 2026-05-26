package com.qtai.domain.mission.api;

/**
 * 미션 시작 UseCase 포트.
 *
 * 동일 미션을 동시에 두 번 시작 못함 — 진행 중인 행이 있으면 INVALID_INPUT.
 */
public interface StartMissionUseCase {

    // TODO: MissionResponse start(Long memberId, MissionStartRequest request);
}
