package com.qtai.domain.mission.api;

/**
 * 미션 완료 처리 UseCase 포트.
 *
 * 완료 조건은 qt 도메인과 연계 (예: "오늘 QT 작성" 미션 → 오늘 작성 QT 존재 여부 확인).
 * 검증은 client/qt 어댑터로 위임.
 */
public interface CompleteMissionUseCase {

    // TODO: MissionResponse complete(Long memberId, Long missionId);
    //       조건 미충족 시 throw BusinessException(INVALID_INPUT, "완료 조건 미충족")
}
