package com.qtai.domain.mission.internal;

/**
 * 미션 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * 타 도메인은 client/ 어댑터로만 접근:
 *   - member.GetMemberUseCase    — 회원 검증
 *   - qt.GetQtUseCase            — "오늘 QT 작성" 같은 완료 조건 확인
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements StartMissionUseCase, CompleteMissionUseCase, ListMissionUseCase
public class MissionService {

    // TODO: final MissionRepository missionRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: final GetQtUseCase getQtUseCase;

    // TODO: @Transactional start(memberId, request) — 중복 진행 검증 후 INSERT
    // TODO: @Transactional complete(memberId, missionId) — qt 조건 확인 → status=COMPLETED
    // TODO: listMyMissions / listAvailable 구현
}
