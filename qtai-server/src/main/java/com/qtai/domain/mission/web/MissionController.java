package com.qtai.domain.mission.web;

/**
 * 미션 REST 엔드포인트. base path: /api/v1/missions
 *
 * 엔드포인트:
 *   GET  /my                    → 내 진행/완료 미션 목록
 *   GET  /available             → 참여 가능한 미션 목록
 *   POST /start                 → 미션 시작
 *   POST /{id}/complete         → 미션 완료 처리
 */
// TODO: @RestController, @RequestMapping("/api/v1/missions"), @RequiredArgsConstructor
public class MissionController {

    // TODO: ListMissionUseCase, StartMissionUseCase, CompleteMissionUseCase 주입
    // TODO: @AuthenticationPrincipal memberId 추출 + ApiResponse 포장
}
